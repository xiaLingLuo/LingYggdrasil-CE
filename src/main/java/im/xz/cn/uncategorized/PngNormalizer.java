/*
 * LingYggdrasil - A modern Minecraft skin/cape hosting and Yggdrasil API system
 * Copyright (C) 2026 XIAZHIRUI HUANG
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package im.xz.cn.uncategorized;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

/**
 * 现代化的 PNG 安全处理工具：先净化（剔除所有辅助块）再解码，生成仅含 IHDR/PLTE/IDAT/IEND 的纯 PNG。
 * 结果对象包含处理状态、标准化后的纯 PNG 字节数组及可读消息。
 */

public class PngNormalizer {

    private static final Logger LOGGER = Logger.getLogger(PngNormalizer.class.getName());

    private final long maxFileSize;
    private final long maxChunkSize;
    private final int maxWidth;
    private final int maxHeight;
    private final long maxPixels;
    private final Set<ChunkType> allowedAuxChunks;
    private final boolean strictChunkMode;
    private static final byte[] PNG_SIGNATURE = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

        private record ChunkType(byte[] bytes) {
            private ChunkType(byte[] bytes) {
                if (bytes == null || bytes.length != 4) {
                    throw new IllegalArgumentException("Invalid chunk type length");
                }
                this.bytes = bytes.clone();
            }

            ChunkType(String ascii) {
                if (ascii == null || ascii.length() != 4 || !ascii.matches("[A-Za-z]{4}")) {
                    throw new IllegalArgumentException("Invalid chunk type: " + ascii);
                }
                this(ascii.getBytes(StandardCharsets.US_ASCII));
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof ChunkType(byte[] bytes1))) return false;
                return Arrays.equals(bytes, bytes1);
            }

            @Override
            public int hashCode() {
                return Arrays.hashCode(bytes);
            }
        }

    private static final ChunkType IHDR = new ChunkType("IHDR");
    private static final ChunkType IDAT = new ChunkType("IDAT");
    private static final ChunkType IEND = new ChunkType("IEND");
    private static final ChunkType PLTE = new ChunkType("PLTE");

    private static final Set<ChunkType> DEFAULT_AUX_CHUNKS = Set.of(
            new ChunkType("tEXt"), new ChunkType("zTXt"), new ChunkType("iTXt"),
            new ChunkType("tIME"), new ChunkType("gAMA"), new ChunkType("cHRM"),
            new ChunkType("sRGB"), new ChunkType("iCCP"),
            new ChunkType("pHYs"), new ChunkType("bKGD"), new ChunkType("hIST"),
            new ChunkType("tRNS"), new ChunkType("sBIT"), new ChunkType("sPLT")
    );

    public static class Result {
        private final boolean success;
        private final byte[] pngData;
        private final String message;

        private Result(boolean success, byte[] pngData, String message) {
            this.success = success;
            this.pngData = pngData;
            this.message = message;
        }

        public boolean isSuccess() { return success; }
        public byte[] getPngData() { return pngData; }
        public String getMessage() { return message; }
    }

    private PngNormalizer(long maxFileSize, long maxChunkSize, int maxWidth, int maxHeight, long maxPixels,
                          Set<String> allowedAuxChunkNames, boolean strictChunkMode) {
        this.maxFileSize = maxFileSize;
        this.maxChunkSize = maxChunkSize;
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
        this.maxPixels = maxPixels;
        Set<ChunkType> typed = new HashSet<>();
        for (String s : allowedAuxChunkNames) {
            typed.add(new ChunkType(s));
        }
        this.allowedAuxChunks = Collections.unmodifiableSet(typed);
        this.strictChunkMode = strictChunkMode;
    }

    public static class Builder {
        private long maxFileSize = 10 * 1024 * 1024;
        private long maxChunkSize = 1 * 1024 * 1024;
        private int maxWidth = 10000;
        private int maxHeight = 10000;
        private long maxPixels = 100_000_000;   // 1e像素
        private Set<String> allowedAuxChunks = new HashSet<>(defaultAuxChunkNames());
        private boolean strictChunkMode = true;

        private static Set<String> defaultAuxChunkNames() {
            return Set.of("tEXt", "zTXt", "iTXt", "tIME", "gAMA", "cHRM", "sRGB", "iCCP",
                    "pHYs", "bKGD", "hIST", "tRNS", "sBIT", "sPLT");
        }

        public Builder maxFileSize(long bytes) { this.maxFileSize = bytes; return this; }
        public Builder maxChunkSize(long bytes) { this.maxChunkSize = bytes; return this; }
        public Builder maxWidth(int w) { this.maxWidth = w; return this; }
        public Builder maxHeight(int h) { this.maxHeight = h; return this; }
        public Builder maxPixels(long p) { this.maxPixels = p; return this; }

        public Builder allowedAuxChunks(Set<String> names) {
            this.allowedAuxChunks = new HashSet<>();
            for (String s : names) addAllowedChunk(s);
            return this;
        }

        public Builder addAllowedChunk(String chunkType) {
            if (chunkType == null || chunkType.length() != 4 || !chunkType.matches("[A-Za-z]{4}")) {
                throw new IllegalArgumentException("Invalid chunk type: " + chunkType);
            }
            this.allowedAuxChunks.add(chunkType);
            return this;
        }

        public Builder strictChunkMode(boolean strict) { this.strictChunkMode = strict; return this; }

        public PngNormalizer build() {
            return new PngNormalizer(maxFileSize, maxChunkSize, maxWidth, maxHeight, maxPixels,
                    allowedAuxChunks, strictChunkMode);
        }
    }

    public Result normalize(Path source) {
        try {
            if (!Files.isRegularFile(source, LinkOption.NOFOLLOW_LINKS)) {
                return new Result(false, null, "Not a regular file");
            }

            byte[] fileData = readLimited(source, maxFileSize);
            if (fileData == null) {
                return new Result(false, null, "File too large (exceeds " + maxFileSize + " bytes)");
            }

            CriticalChunks critical;
            try (InputStream in = new ByteArrayInputStream(fileData)) {
                critical = parseAndCollectCriticalChunks(in, fileData.length);
            } catch (InvalidPngException e) {
                return new Result(false, null, e.getMessage());
            }

            byte[] cleanPng = buildCleanPng(critical);
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(cleanPng));
            if (image == null) {
                return new Result(false, null, "ImageIO could not decode cleaned PNG");
            }

            int width = image.getWidth();
            int height = image.getHeight();
            if (width <= 0 || height <= 0 || width > maxWidth || height > maxHeight ||
                    (long) width * height > maxPixels) {
                return new Result(false, null, String.format(
                        "Image dimensions invalid: %dx%d (max %dx%d, %d pixels)",
                        width, height, maxWidth, maxHeight, maxPixels));
            }

            byte[] finalPng = generatePurePng(image);
            return new Result(true, finalPng, "Success");

        } catch (InvalidPngException e) {
            LOGGER.log(Level.WARNING, "PNG validation failed: " + e.getMessage(), e);
            return new Result(false, null, e.getMessage());
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "IO error during PNG processing", e);
            return new Result(false, null, "IO error: " + e.getMessage());
        } catch (OutOfMemoryError e) {
            LOGGER.log(Level.SEVERE, "OOM while processing PNG", e);
            return new Result(false, null, "Out of memory");
        }
    }

    private static byte[] readLimited(Path path, long maxBytes) throws IOException {
        try (InputStream fis = Files.newInputStream(path)) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            long total = 0;
            int n;
            while ((n = fis.read(buf)) != -1) {
                total += n;
                if (total > maxBytes) {
                    return null;
                }
                bos.write(buf, 0, n);
            }
            return bos.toByteArray();
        }
    }

        private record CriticalChunks(byte[] ihdrData, byte[] plteData, byte[] idatData) {
    }

    private CriticalChunks parseAndCollectCriticalChunks(InputStream input, long totalLength) throws IOException {
        DataInputStream dis = new DataInputStream(input);

        byte[] sig = new byte[8];
        dis.readFully(sig);
        if (!Arrays.equals(sig, PNG_SIGNATURE)) {
            throw new InvalidPngException("Bad PNG signature");
        }

        boolean ihdrFound = false;
        boolean iendFound = false;
        boolean afterIdat = false;
        byte[] ihdrData = null;
        byte[] plteData = null;
        ByteArrayOutputStream idatStream = new ByteArrayOutputStream();
        int ihdrColorType = -1;
        long bytesRead = 8;

        while (!iendFound) {
            if (bytesRead + 4 > totalLength) throw new InvalidPngException("Unexpected EOF in chunk length");
            int length = dis.readInt();
            bytesRead += 4;
            if (length < 0)               throw new InvalidPngException("Negative chunk length");
            if (length > maxChunkSize)    throw new InvalidPngException(
                    "Chunk size " + length + " exceeds limit " + maxChunkSize);

            if (bytesRead + 4 > totalLength) throw new InvalidPngException("Unexpected EOF in chunk type");
            byte[] typeBytes = new byte[4];
            dis.readFully(typeBytes);
            bytesRead += 4;
            ChunkType chunkType = new ChunkType(typeBytes);

            if (bytesRead + length > totalLength) throw new InvalidPngException("Unexpected EOF in chunk data");
            byte[] data = new byte[length];
            dis.readFully(data);
            bytesRead += length;

            if (bytesRead + 4 > totalLength) throw new InvalidPngException("Unexpected EOF in chunk CRC");
            int crc = dis.readInt();
            bytesRead += 4;

            CRC32 crc32 = new CRC32();
            crc32.update(typeBytes);
            crc32.update(data);
            if ((crc32.getValue() & 0xFFFFFFFFL) != (crc & 0xFFFFFFFFL)) {
                throw new InvalidPngException(String.format("CRC mismatch for chunk %s", str(typeBytes)));
            }

            if (chunkType.equals(IHDR)) {
                if (ihdrFound)  throw new InvalidPngException("Duplicate IHDR");
                if (afterIdat)  throw new InvalidPngException("IHDR after IDAT");
                ihdrColorType = parseIHDR(data);
                ihdrData = data;
                ihdrFound = true;
            } else if (chunkType.equals(IEND)) {
                if (!ihdrFound) throw new InvalidPngException("IEND without IHDR");
                if (length != 0) throw new InvalidPngException("IEND length must be 0");
                iendFound = true;
            } else if (chunkType.equals(IDAT)) {
                if (!ihdrFound) throw new InvalidPngException("IDAT before IHDR");
                afterIdat = true;
                idatStream.write(data);
            } else if (chunkType.equals(PLTE)) {
                if (!ihdrFound) throw new InvalidPngException("PLTE before IHDR");
                if (afterIdat)  throw new InvalidPngException("PLTE after IDAT");
                int maxEntries = getMaxEntries(ihdrColorType, length, ihdrData);
                if (length / 3 > maxEntries) {
                    throw new InvalidPngException("PLTE entries exceed maximum for bit depth");
                }
                plteData = data;
            } else {
                if (!ihdrFound) throw new InvalidPngException("Aux chunk before IHDR");
                if (!allowedAuxChunks.contains(chunkType)) {
                    if (strictChunkMode) {
                        throw new InvalidPngException("Disallowed auxiliary chunk: " + str(typeBytes));
                    } else {
                        LOGGER.fine(() -> "Ignored non-critical auxiliary chunk: " + str(typeBytes));
                    }
                }
            }
        }

        if (ihdrColorType == 3 && plteData == null) {
            throw new InvalidPngException("Indexed color image requires PLTE chunk");
        }

        return new CriticalChunks(ihdrData, plteData, idatStream.toByteArray());
    }

    private static int getMaxEntries(int ihdrColorType, int length, byte[] ihdrData) throws InvalidPngException {
        if (ihdrColorType == 0 || ihdrColorType == 4) {
            throw new InvalidPngException("PLTE not allowed for grayscale images");
        }
        if (length < 3 || length > 768 || length % 3 != 0) {
            throw new InvalidPngException("Invalid PLTE length");
        }
        int bitDepth = (ihdrData[8] & 0xFF);
        return 1 << bitDepth;
    }

    private byte[] buildCleanPng(CriticalChunks cc) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bos.write(PNG_SIGNATURE);
        bos.write(buildChunk(IHDR.bytes, cc.ihdrData));
        if (cc.plteData != null) {
            bos.write(buildChunk(PLTE.bytes, cc.plteData));
        }
        bos.write(buildChunk(IDAT.bytes, cc.idatData));
        bos.write(buildChunk(IEND.bytes, new byte[0]));
        return bos.toByteArray();
    }

    private int parseIHDR(byte[] data) throws InvalidPngException {
        if (data.length != 13) throw new InvalidPngException("IHDR must be 13 bytes");
        try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data))) {
            int width = dis.readInt();
            int height = dis.readInt();
            int bitDepth = dis.readUnsignedByte();
            int colorType = dis.readUnsignedByte();
            int compression = dis.readUnsignedByte();
            int filter = dis.readUnsignedByte();
            int interlace = dis.readUnsignedByte();

            if (width <= 0 || height <= 0) throw new InvalidPngException("Invalid dimensions");
            if (width > maxWidth || height > maxHeight) throw new InvalidPngException("Dimensions too large");
            if ((long) width * height > maxPixels) throw new InvalidPngException("Too many pixels");

            Set<Integer> allowedDepths = switch (colorType) {
                case 0 -> Set.of(1, 2, 4, 8, 16);
                case 2, 4, 6 -> Set.of(8, 16);
                case 3 -> Set.of(1, 2, 4, 8);
                default -> throw new InvalidPngException("Unknown color type: " + colorType);
            };
            if (!allowedDepths.contains(bitDepth)) {
                throw new InvalidPngException(String.format("Invalid bit depth %d for color type %d", bitDepth, colorType));
            }

            if (compression != 0) throw new InvalidPngException("Unknown compression method");
            if (filter != 0) throw new InvalidPngException("Unknown filter method");
            if (interlace != 0 && interlace != 1) throw new InvalidPngException("Unknown interlace method");

            return colorType;
        } catch (IOException impossible) {
            throw new InvalidPngException("IHDR parsing error");
        }
    }

    private byte[] generatePurePng(BufferedImage sourceImage) throws IOException {
        int width = sourceImage.getWidth();
        int height = sourceImage.getHeight();

        boolean hasTransparency = false;
        int[] rowPixels = new int[width];
        for (int y = 0; y < height; y++) {
            sourceImage.getRGB(0, y, width, 1, rowPixels, 0, width);
            for (int x = 0; x < width; x++) {
                if ((rowPixels[x] >>> 24) != 0xFF) {
                    hasTransparency = true;
                    break;
                }
            }
            if (hasTransparency) break;
        }

        int colorType = hasTransparency ? 6 : 2;

        byte[] ihdrData = new byte[13];
        writeInt(ihdrData, 0, width);
        writeInt(ihdrData, 4, height);
        ihdrData[8] = 8;
        ihdrData[9] = (byte) colorType;
        ihdrData[10] = 0;
        ihdrData[11] = 0;
        ihdrData[12] = 0;

        ByteArrayOutputStream rawStream = new ByteArrayOutputStream();
        for (int y = 0; y < height; y++) {
            rawStream.write(0); // filter: none
            sourceImage.getRGB(0, y, width, 1, rowPixels, 0, width);
            for (int x = 0; x < width; x++) {
                int argb = rowPixels[x];
                rawStream.write((argb >> 16) & 0xFF); // R
                rawStream.write((argb >> 8) & 0xFF);  // G
                rawStream.write(argb & 0xFF);         // B
                if (hasTransparency) {
                    rawStream.write((argb >> 24) & 0xFF); // A
                }
            }
        }

        byte[] rawFiltered = rawStream.toByteArray();
        byte[] compressed = compressData(rawFiltered);

        ByteArrayOutputStream pngStream = new ByteArrayOutputStream();
        pngStream.write(PNG_SIGNATURE);
        pngStream.write(buildChunk(IHDR.bytes, ihdrData));
        pngStream.write(buildChunk(IDAT.bytes, compressed));
        pngStream.write(buildChunk(IEND.bytes, new byte[0]));
        return pngStream.toByteArray();
    }

    private static byte[] compressData(byte[] input) throws IOException {
        Deflater deflater = new Deflater();
        try {
            deflater.setInput(input);
            deflater.finish();
            byte[] out = new byte[input.length + 128];
            int outLen = 0;
            while (!deflater.finished()) {
                int len = deflater.deflate(out, outLen, out.length - outLen);
                if (len > 0) {
                    outLen += len;
                    if (outLen >= out.length - 64) {
                        if (out.length > Integer.MAX_VALUE / 2) {
                            throw new IOException("Compressed data too large");
                        }
                        out = Arrays.copyOf(out, out.length * 2);
                    }
                } else if (len == 0 && !deflater.needsInput()) {
                    if (out.length > Integer.MAX_VALUE / 2) {
                        throw new IOException("Compressed data too large");
                    }
                    out = Arrays.copyOf(out, out.length * 2);
                }
            }
            return Arrays.copyOf(out, outLen);
        } finally {
            deflater.end();
        }
    }

    private static void writeInt(byte[] buf, int offset, int value) {
        buf[offset]   = (byte) (value >> 24);
        buf[offset+1] = (byte) (value >> 16);
        buf[offset+2] = (byte) (value >> 8);
        buf[offset+3] = (byte) value;
    }

    private static byte[] buildChunk(byte[] type, byte[] data) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(data.length + 12);
        try {
            int length = data.length;
            baos.write((length >> 24) & 0xFF);
            baos.write((length >> 16) & 0xFF);
            baos.write((length >> 8) & 0xFF);
            baos.write(length & 0xFF);
            baos.write(type);
            baos.write(data);
            CRC32 crc = new CRC32();
            crc.update(type);
            crc.update(data);
            long crcValue = crc.getValue();
            baos.write((int) ((crcValue >> 24) & 0xFF));
            baos.write((int) ((crcValue >> 16) & 0xFF));
            baos.write((int) ((crcValue >> 8) & 0xFF));
            baos.write((int) (crcValue & 0xFF));
        } catch (IOException impossible) {
            assert true;
        }
        return baos.toByteArray();
    }

    private static String str(byte[] bytes) {
        return new String(bytes, StandardCharsets.US_ASCII);
    }

    private static class InvalidPngException extends IOException {
        InvalidPngException(String message) { super(message); }
    }
}