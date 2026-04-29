package info.mengnan.aitalk.common.crypto;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Map;

/**
 * JWT 辅助类，注入到 GraalJS 上下文中供脚本调用。
 * JS 中通过 jwt.encode(payloadJson, key, algorithm, headersJson) 生成 JWT。
 * <p>
 * 支持的算法：
 * - HMAC：HS256, HS384, HS512（对称密钥，key 为原始密钥字符串）
 * - RSA：RS256, RS384, RS512（非对称密钥，key 为 Base64 编码的 PKCS#8 私钥）
 * - EC：ES256, ES384, ES512（非对称密钥，key 为 Base64 编码的 PKCS#8 私钥）
 * - EdDSA：EdDSA（非对称密钥，key 为 Base64 编码的 PKCS#8 私钥）
 */
@Slf4j
public class JwtHelper {

    /** JWT alg → JDK KeyFactory 算法名 */
    private static final Map<String, String> KEY_FACTORY_ALG = Map.ofEntries(
            Map.entry("RS256", "RSA"), Map.entry("RS384", "RSA"), Map.entry("RS512", "RSA"),
            Map.entry("PS256", "RSA"), Map.entry("PS384", "RSA"), Map.entry("PS512", "RSA"),
            Map.entry("ES256", "EC"),  Map.entry("ES384", "EC"),  Map.entry("ES512", "EC"),
            Map.entry("EdDSA", "EdDSA")
    );

    /** JWT alg → JDK Signature 算法名 */
    private static final Map<String, String> SIGNATURE_ALG = Map.ofEntries(
            Map.entry("RS256", "SHA256withRSA"),    Map.entry("RS384", "SHA384withRSA"),    Map.entry("RS512", "SHA512withRSA"),
            Map.entry("PS256", "SHA256withRSAandMGF1"), Map.entry("PS384", "SHA384withRSAandMGF1"), Map.entry("PS512", "SHA512withRSAandMGF1"),
            Map.entry("ES256", "SHA256withECDSA"),  Map.entry("ES384", "SHA384withECDSA"),  Map.entry("ES512", "SHA512withECDSA"),
            Map.entry("EdDSA", "EdDSA")
    );

    /** JWT alg → JDK Mac 算法名 */
    private static final Map<String, String> MAC_ALG = Map.of(
            "HS256", "HmacSHA256",
            "HS384", "HmacSHA384",
            "HS512", "HmacSHA512"
    );

    /**
     * 生成 JWT
     *
     * @param payloadJson JSON 字符串，包含 claims（sub, iat, exp 等）
     * @param key         密钥：HMAC 算法传原始密钥字符串；非对称算法传 Base64 编码的 PKCS#8 私钥
     * @param algorithm   签名算法（如 "HS256", "RS256", "ES256", "EdDSA"）
     * @param headersJson JSON 字符串，包含额外的 header 字段（如 {"kid": "..."}），可为 null
     * @return 完整的 JWT 字符串 (header.payload.signature)
     */
    public String encode(String payloadJson, String key, String algorithm, String headersJson) {
        try {
            Base64.Encoder urlEncoder = Base64.getUrlEncoder().withoutPadding();

            // 构建并编码 header 和 payload
            String headerJson = buildHeaderJson(algorithm, headersJson);
            String headerEncoded = urlEncoder.encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
            String payloadEncoded = urlEncoder.encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
            String data = headerEncoded + "." + payloadEncoded;

            // 根据算法类型选择签名方式
            byte[] signatureBytes;
            if (MAC_ALG.containsKey(algorithm)) {
                signatureBytes = signWithHmac(data, key, algorithm);
            } else if (SIGNATURE_ALG.containsKey(algorithm)) {
                signatureBytes = signWithKey(data, key, algorithm);
            } else {
                return "{\"error\": \"Unsupported algorithm: " + algorithm + "\"}";
            }

            String signatureEncoded = urlEncoder.encodeToString(signatureBytes);
            return data + "." + signatureEncoded;
        } catch (Exception e) {
            log.error("JWT encode failed: {}", e.getMessage(), e);
            return "{\"error\": \"JWT encode failed: " + e.getMessage() + "\"}";
        }
    }

    /**
     * HMAC 签名（HS256 / HS384 / HS512）
     * key 为原始密钥字符串
     */
    private byte[] signWithHmac(String data, String key, String algorithm) throws Exception {
        String macAlg = MAC_ALG.get(algorithm);
        Mac mac = Mac.getInstance(macAlg);
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), macAlg));
        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 非对称密钥签名（RSA / EC / EdDSA）
     * key 为 Base64 编码的 PKCS#8 私钥
     */
    private byte[] signWithKey(String data, String key, String algorithm) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(key);
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_FACTORY_ALG.get(algorithm));
        PrivateKey privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(keyBytes));

        Signature signer = Signature.getInstance(SIGNATURE_ALG.get(algorithm));
        signer.initSign(privateKey);
        signer.update(data.getBytes(StandardCharsets.UTF_8));
        return signer.sign();
    }

    /**
     * 合并 alg 字段和用户自定义 header 字段
     */
    private String buildHeaderJson(String algorithm, String headersJson) {
        StringBuilder sb = new StringBuilder("{\"alg\":\"").append(algorithm).append("\"");
        if (headersJson != null && !headersJson.isBlank()) {
            String trimmed = headersJson.trim();
            if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                String inner = trimmed.substring(1, trimmed.length() - 1).trim();
                if (!inner.isEmpty()) {
                    sb.append(",").append(inner);
                }
            }
        }
        sb.append("}");
        return sb.toString();
    }
}
