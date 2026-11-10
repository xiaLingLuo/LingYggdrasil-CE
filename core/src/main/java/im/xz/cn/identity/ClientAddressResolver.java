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
package im.xz.cn.identity;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public final class ClientAddressResolver {

    private final List<CidrBlock> trustedProxies;

    private ClientAddressResolver(List<CidrBlock> trustedProxies) {
        this.trustedProxies = List.copyOf(trustedProxies);
    }

    public static ClientAddressResolver withTrustedProxies(List<String> cidrs) {
        List<CidrBlock> blocks = new ArrayList<>();
        if (cidrs != null) {
            for (String cidr : cidrs) {
                if (cidr != null && !cidr.isBlank()) {
                    blocks.add(CidrBlock.parse(cidr.trim()));
                }
            }
        }
        return new ClientAddressResolver(blocks);
    }

    
    public static ClientAddressResolver directOnly() {
        return new ClientAddressResolver(List.of());
    }

    public boolean isTrusted(String address) {
        if (address == null || trustedProxies.isEmpty()) {
            return false;
        }
        InetAddress parsed = parseLiteral(address);
        if (parsed == null) {
            return false;
        }
        for (CidrBlock block : trustedProxies) {
            if (block.contains(parsed)) {
                return true;
            }
        }
        return false;
    }

    
    public String resolve(String remoteAddress, String forwardedFor, String realIp) {
        String remote = normalize(remoteAddress);
        if (!isTrusted(remote)) {
            return remote;
        }
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            String[] hops = forwardedFor.split(",");
            String leftmost = null;
            for (String hop : hops) {
                String normalizedHop = normalize(hop);
                if (!normalizedHop.isEmpty()) {
                    leftmost = normalizedHop;
                    break;
                }
            }
            for (int i = hops.length - 1; i >= 0; i--) {
                String hop = normalize(hops[i]);
                if (hop.isEmpty()) {
                    continue;
                }
                if (!isTrusted(hop)) {
                    return hop;
                }
            }
            if (leftmost != null) {
                return leftmost;
            }
        }
        String real = normalize(realIp);
        if (!real.isEmpty()) {
            return real;
        }
        return remote;
    }

    private static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String value = raw.trim();
        if (value.isEmpty()) {
            return value;
        }
        if (value.startsWith("[")) {
            int end = value.indexOf(']');
            return end > 0 ? value.substring(1, end) : value;
        }
        int colon = value.indexOf(':');
        if (colon > 0 && value.indexOf(':', colon + 1) < 0) {
            return value.substring(0, colon);
        }
        return value;
    }

    private static InetAddress parseLiteral(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = normalize(raw);
        if (!looksLikeIpv4(value) && !looksLikeIpv6(value)) {
            return null;
        }
        try {
            return InetAddress.getByName(value);
        } catch (UnknownHostException e) {
            return null;
        }
    }

    private static boolean looksLikeIpv4(String value) {
        return value.matches("\\d{1,3}(\\.\\d{1,3}){3}");
    }

    private static boolean looksLikeIpv6(String value) {
        return value.contains(":") && value.matches("[0-9a-fA-F:.%]*");
    }

    
    public static final class CidrBlock {
        private final byte[] network;
        private final int prefixBits;

        private CidrBlock(byte[] network, int prefixBits) {
            this.network = network;
            this.prefixBits = prefixBits;
        }

        public static CidrBlock parse(String cidr) {
            String value = cidr.trim();
            String addressPart = value;
            int bits = -1;
            int slash = value.indexOf('/');
            if (slash >= 0) {
                addressPart = value.substring(0, slash);
                String bitsPart = value.substring(slash + 1);
                try {
                    bits = Integer.parseInt(bitsPart);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("invalid CIDR prefix: " + cidr, e);
                }
            }
            if (!looksLikeIpv4(addressPart) && !looksLikeIpv6(addressPart)) {
                throw new IllegalArgumentException("not an IP literal: " + addressPart);
            }
            InetAddress address;
            try {
                address = InetAddress.getByName(addressPart);
            } catch (UnknownHostException e) {
                throw new IllegalArgumentException("invalid address: " + addressPart, e);
            }
            byte[] bytes = address.getAddress();
            int maxBits = bytes.length * 8;
            if (bits < 0) {
                bits = maxBits;
            }
            if (bits > maxBits) {
                throw new IllegalArgumentException("prefix out of range: " + cidr);
            }
            return new CidrBlock(bytes, bits);
        }

        public boolean contains(InetAddress address) {
            byte[] candidate = address.getAddress();
            if (candidate.length != network.length) {
                return false;
            }
            int fullBytes = prefixBits / 8;
            for (int i = 0; i < fullBytes; i++) {
                if (candidate[i] != network[i]) {
                    return false;
                }
            }
            int remainder = prefixBits % 8;
            if (remainder == 0) {
                return true;
            }
            int mask = (0xFF << (8 - remainder)) & 0xFF;
            return (candidate[fullBytes] & mask) == (network[fullBytes] & mask);
        }

        @Override
        public String toString() {
            return network.length == 4 ? "ipv4/" + prefixBits : "ipv6/" + prefixBits;
        }
    }

    @Override
    public String toString() {
        return "ClientAddressResolver" + trustedProxies.toString().toLowerCase(Locale.ROOT);
    }
}
