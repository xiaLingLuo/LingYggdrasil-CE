package im.xz.cn.util;

import java.security.*;
import java.security.spec.*;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YggdrasilKeyManager {
    private static final Logger logger = LoggerFactory.getLogger(YggdrasilKeyManager.class);
    private static final YggdrasilKeyManager INSTANCE = new YggdrasilKeyManager();

    public static YggdrasilKeyManager getInstance() {
        return INSTANCE;
    }

    public static final String MODE_ED448 = "ed448";
    public static final String MODE_RSA_SHA512 = "rsa-sha512";

    private PrivateKey privateKey;
    private PublicKey publicKey;
    private String mode;

    public void loadFromPem(String privateKeyPem, String publicKeyPem, String mode) {
        this.mode = mode;
        try {
            if (privateKeyPem != null && !privateKeyPem.isEmpty()) {
                this.privateKey = loadPrivateKey(privateKeyPem, mode);
            }
            if (publicKeyPem != null && !publicKeyPem.isEmpty()) {
                this.publicKey = loadPublicKey(publicKeyPem, mode);
            }
        } catch (Exception e) {
            logger.error("加载密钥对失败: {}", e.getMessage(), e);
        }
    }

    public void generateKeyPair(String mode) throws Exception {
        this.mode = mode;
        KeyPairGenerator kpg;

        if (MODE_ED448.equals(mode)) {
            kpg = KeyPairGenerator.getInstance("Ed448");
        } else if (MODE_RSA_SHA512.equals(mode)) {
            kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(4096);
        } else {
            throw new IllegalArgumentException("未知的签名模式: " + mode);
        }

        KeyPair kp = kpg.generateKeyPair();
        this.privateKey = kp.getPrivate();
        this.publicKey = kp.getPublic();
        logger.info("已生成新的 {} 密钥对", mode);
    }

    public String sign(String data) {
        if (privateKey == null) {
            logger.warn("私钥未加载，无法签名");
            return "";
        }
        try {
            Signature sig;
            if (MODE_ED448.equals(mode)) {
                sig = Signature.getInstance("Ed448");
            } else {
                sig = Signature.getInstance("SHA512withRSA");
            }
            sig.initSign(privateKey);
            sig.update(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            byte[] signatureBytes = sig.sign();
            return Base64.getEncoder().encodeToString(signatureBytes);
        } catch (Exception e) {
            logger.error("签名失败: {}", e.getMessage(), e);
            return "";
        }
    }

    public String getPublicKeyPem() {
        if (publicKey == null) return "";
        String base64 = Base64.getEncoder().encodeToString(publicKey.getEncoded());
        StringBuilder sb = new StringBuilder();
        sb.append("-----BEGIN PUBLIC KEY-----\n");
        for (int i = 0; i < base64.length(); i += 76) {
            sb.append(base64, i, Math.min(i + 76, base64.length()));
            if (i + 76 < base64.length()) sb.append("\n");
        }
        sb.append("\n-----END PUBLIC KEY-----");
        return sb.toString();
    }

    public String getPrivateKeyPem() {
        if (privateKey == null) return "";
        String base64 = Base64.getEncoder().encodeToString(privateKey.getEncoded());
        StringBuilder sb = new StringBuilder();
        sb.append("-----BEGIN PRIVATE KEY-----\n");
        for (int i = 0; i < base64.length(); i += 76) {
            sb.append(base64, i, Math.min(i + 76, base64.length()));
            if (i + 76 < base64.length()) sb.append("\n");
        }
        sb.append("\n-----END PRIVATE KEY-----");
        return sb.toString();
    }

    public String getPublicKeyBase64() {
        if (publicKey == null) return "";
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    public boolean isLoaded() {
        return privateKey != null && publicKey != null;
    }

    public String getMode() {
        return mode;
    }

    private PrivateKey loadPrivateKey(String pem, String mode) throws Exception {
        String base64 = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] keyBytes = Base64.getDecoder().decode(base64);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        String algorithm = MODE_ED448.equals(mode) ? "Ed448" : "RSA";
        KeyFactory kf = KeyFactory.getInstance(algorithm);
        return kf.generatePrivate(spec);
    }

    private PublicKey loadPublicKey(String pem, String mode) throws Exception {
        String base64 = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] keyBytes = Base64.getDecoder().decode(base64);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        String algorithm = MODE_ED448.equals(mode) ? "Ed448" : "RSA";
        KeyFactory kf = KeyFactory.getInstance(algorithm);
        return kf.generatePublic(spec);
    }
}
