package com.example.middleware.config.security;

/**
 * 配置安全演示 — AES/RSA 加密解密 + 密钥轮转
 *
 * <p>本示例用 Java 标准加密库演示配置中心的安全机制：
 * <ul>
 *   <li>AES 对称加密：加密数据库密码等敏感配置</li>
 *   <li>RSA 非对称加密：加密 AES 密钥（信封加密）</li>
 *   <li>Jasypt 集成原理：ENC(密文) 格式</li>
 *   <li>密钥轮转流程</li>
 * </ul>
 *
 * <h3>配置加密方案：</h3>
 * <pre>
 *  方案1：Jasypt 对称加密
 *    配置文件：spring.datasource.password=ENC(加密后的密文)
 *    启动参数：-Djasypt.encryptor.password=加密密钥
 *
 *  方案2：信封加密（Envelope Encryption）
 *    1. 用 AES 密钥加密配置值（数据密钥 DEK）
 *    2. 用 RSA 公钥加密 AES 密钥（密钥加密密钥 KEK）
 *    3. 存储：加密后的配置值 + 加密后的 AES 密钥
 *    4. 解密：RSA 私钥解密 AES 密钥 → AES 密钥解密配置值
 * </pre>
 */
public class ConfigSecurityDemo {

    // ==================== AES 对称加密 ====================

    /** AES 加密工具 */
    static class AESEncryptor {
        private final javax.crypto.SecretKey secretKey;

        AESEncryptor(String password) throws Exception {
            // 从密码派生 AES 密钥（PBKDF2）
            javax.crypto.SecretKeyFactory factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec(
                    password.toCharArray(), "salt1234".getBytes(), 65536, 256);
            byte[] keyBytes = factory.generateSecret(spec).getEncoded();
            this.secretKey = new javax.crypto.spec.SecretKeySpec(keyBytes, "AES");
        }

        /** 加密 */
        String encrypt(String plaintext) throws Exception {
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, secretKey);
            byte[] encrypted = cipher.doFinal(plaintext.getBytes("UTF-8"));
            return java.util.Base64.getEncoder().encodeToString(encrypted);
        }

        /** 解密 */
        String decrypt(String ciphertext) throws Exception {
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, secretKey);
            byte[] decrypted = cipher.doFinal(java.util.Base64.getDecoder().decode(ciphertext));
            return new String(decrypted, "UTF-8");
        }
    }

    // ==================== RSA 非对称加密 ====================

    /** RSA 加密工具 */
    static class RSAEncryptor {
        private final java.security.KeyPair keyPair;

        RSAEncryptor() throws Exception {
            java.security.KeyPairGenerator generator = java.security.KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            this.keyPair = generator.generateKeyPair();
        }

        /** 公钥加密 */
        String encrypt(String plaintext) throws Exception {
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("RSA");
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, keyPair.getPublic());
            byte[] encrypted = cipher.doFinal(plaintext.getBytes("UTF-8"));
            return java.util.Base64.getEncoder().encodeToString(encrypted);
        }

        /** 私钥解密 */
        String decrypt(String ciphertext) throws Exception {
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("RSA");
            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, keyPair.getPrivate());
            byte[] decrypted = cipher.doFinal(java.util.Base64.getDecoder().decode(ciphertext));
            return new String(decrypted, "UTF-8");
        }
    }

    // ==================== Jasypt 模拟 ====================

    /** 模拟 Jasypt 的 ENC() 格式处理 */
    static class JasyptSimulator {
        private final AESEncryptor encryptor;

        JasyptSimulator(String masterPassword) throws Exception {
            this.encryptor = new AESEncryptor(masterPassword);
        }

        /** 加密配置值，返回 ENC(密文) 格式 */
        String encryptProperty(String value) throws Exception {
            return "ENC(" + encryptor.encrypt(value) + ")";
        }

        /** 解析配置值，如果是 ENC() 格式则解密 */
        String resolveProperty(String value) throws Exception {
            if (value.startsWith("ENC(") && value.endsWith(")")) {
                String ciphertext = value.substring(4, value.length() - 1);
                return encryptor.decrypt(ciphertext);
            }
            return value; // 非加密配置，原样返回
        }
    }

    // ==================== 演示方法 ====================

    static void demoAESEncryption() throws Exception {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示1：AES 对称加密 — 加密数据库密码");
        System.out.println("═══════════════════════════════════════════════════");

        String masterPassword = "my-secret-key-2024";
        AESEncryptor aes = new AESEncryptor(masterPassword);

        String[] sensitiveConfigs = {"root123", "redis_password_456", "mq_secret_789"};

        System.out.println("\n  加密敏感配置：");
        for (String config : sensitiveConfigs) {
            String encrypted = aes.encrypt(config);
            String decrypted = aes.decrypt(encrypted);
            System.out.printf("    明文: %-25s → 密文: %s%n", config, encrypted.substring(0, 30) + "...");
            System.out.printf("    解密验证: %s ✓%n", decrypted.equals(config) ? "一致" : "不一致 ✗");
        }
        System.out.println();
    }

    static void demoRSAEncryption() throws Exception {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示2：RSA 非对称加密 — 信封加密");
        System.out.println("═══════════════════════════════════════════════════");

        RSAEncryptor rsa = new RSAEncryptor();

        // 信封加密流程
        System.out.println("\n  信封加密流程：");
        String aesKey = "random-aes-key-" + System.currentTimeMillis();
        System.out.printf("    1. 生成随机 AES 密钥: %s%n", aesKey);

        String encryptedKey = rsa.encrypt(aesKey);
        System.out.printf("    2. RSA 公钥加密 AES 密钥: %s...%n", encryptedKey.substring(0, 40));

        AESEncryptor aes = new AESEncryptor(aesKey);
        String sensitiveData = "jdbc:mysql://prod-rds.com:3306/prod_db?password=super_secret";
        String encryptedData = aes.encrypt(sensitiveData);
        System.out.printf("    3. AES 加密配置值: %s...%n", encryptedData.substring(0, 40));

        // 解密流程
        System.out.println("\n  解密流程：");
        String decryptedKey = rsa.decrypt(encryptedKey);
        System.out.printf("    1. RSA 私钥解密 AES 密钥: %s%n", decryptedKey);

        AESEncryptor aes2 = new AESEncryptor(decryptedKey);
        String decryptedData = aes2.decrypt(encryptedData);
        System.out.printf("    2. AES 解密配置值: %s%n", decryptedData);
        System.out.println();
    }

    static void demoJasypt() throws Exception {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示3：Jasypt 集成 — ENC() 格式");
        System.out.println("═══════════════════════════════════════════════════");

        JasyptSimulator jasypt = new JasyptSimulator("jasypt-master-password");

        // 模拟配置文件
        java.util.Map<String, String> configFile = new java.util.LinkedHashMap<>();
        configFile.put("server.port", "8080");
        configFile.put("spring.datasource.url", "jdbc:mysql://localhost:3306/mydb");
        configFile.put("spring.datasource.username", "root");
        configFile.put("spring.datasource.password", jasypt.encryptProperty("root123"));
        configFile.put("spring.redis.password", jasypt.encryptProperty("redis_pwd"));

        System.out.println("\n  配置文件内容（含加密项）：");
        for (var entry : configFile.entrySet()) {
            System.out.printf("    %s = %s%n", entry.getKey(), entry.getValue());
        }

        // 解析配置（自动解密 ENC() 格式）
        System.out.println("\n  Spring 启动时自动解密：");
        System.out.println("  启动参数：-Djasypt.encryptor.password=jasypt-master-password");
        for (var entry : configFile.entrySet()) {
            String resolved = jasypt.resolveProperty(entry.getValue());
            boolean wasEncrypted = entry.getValue().startsWith("ENC(");
            System.out.printf("    %s = %s%s%n", entry.getKey(), resolved,
                    wasEncrypted ? " ← 已解密" : "");
        }
        System.out.println();
    }

    static void demoKeyRotation() throws Exception {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示4：密钥轮转流程");
        System.out.println("═══════════════════════════════════════════════════");

        System.out.println("\n  密钥轮转步骤：");
        System.out.println("    1. 生成新密钥（new-key-2025）");
        System.out.println("    2. 用旧密钥解密所有配置");
        System.out.println("    3. 用新密钥重新加密所有配置");
        System.out.println("    4. 更新配置文件和启动参数");
        System.out.println("    5. 滚动重启应用");
        System.out.println("    6. 废弃旧密钥");

        // 模拟轮转
        String oldPassword = "old-key-2024";
        String newPassword = "new-key-2025";

        AESEncryptor oldEncryptor = new AESEncryptor(oldPassword);
        AESEncryptor newEncryptor = new AESEncryptor(newPassword);

        String[] secrets = {"db_password_123", "redis_pwd_456"};

        System.out.println("\n  模拟密钥轮转：");
        for (String secret : secrets) {
            String oldEncrypted = oldEncryptor.encrypt(secret);
            // 旧密钥解密
            String decrypted = oldEncryptor.decrypt(oldEncrypted);
            // 新密钥加密
            String newEncrypted = newEncryptor.encrypt(decrypted);
            // 验证
            String verified = newEncryptor.decrypt(newEncrypted);
            System.out.printf("    %s: 旧密文→解密→新密文→验证=%s ✓%n", secret, verified.equals(secret) ? "通过" : "失败");
        }
        System.out.println();
    }

    // ==================== 主方法 ====================

    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║  配置安全演示 — AES/RSA 加密 + Jasypt（纯内存）        ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();

        demoAESEncryption();
        demoRSAEncryption();
        demoJasypt();
        demoKeyRotation();
    }
}
