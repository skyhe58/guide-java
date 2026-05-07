package com.example.minio;

/**
 * MinIO 对象存储演示（混合模式）
 *
 * <p>Part A：用文件系统模拟 MinIO 桶管理 + 分片上传（直接运行）
 * <ul>
 *   <li>桶（Bucket）管理：创建、列表、删除</li>
 *   <li>对象（Object）操作：上传、下载、删除、列表</li>
 *   <li>分片上传（Multipart Upload）模拟</li>
 *   <li>预签名 URL（Presigned URL）原理</li>
 * </ul>
 *
 * <p>Part B：用 MinIO Java SDK 连接真实 MinIO
 *
 * <p>Part B 运行前启动依赖：
 * {@code docker compose -f docker/docker-compose.yml up -d minio}
 * <p>控制台：http://localhost:9001（minioadmin/minioadmin）
 *
 * <h3>MinIO / S3 核心概念：</h3>
 * <pre>
 *  Bucket（桶）     — 类似文件系统的根目录，全局唯一命名
 *  Object（对象）    — 存储的文件，由 Key（路径）+ Value（内容）+ Metadata 组成
 *  Key（键）        — 对象的唯一标识，支持 "/" 模拟目录结构
 *  Presigned URL   — 临时授权 URL，允许未认证用户在有效期内访问对象
 *
 *  示例：
 *  Bucket: my-images
 *  Object Key: photos/2024/01/cat.jpg
 *  → 访问路径：http://localhost:9000/my-images/photos/2024/01/cat.jpg
 * </pre>
 */
public class MinIODemo {

    // ==================== Part A：模拟 MinIO ====================

    /** 模拟 MinIO 对象 */
    static class SimulatedObject {
        final String key;
        final byte[] data;
        final String contentType;
        final long size;
        final long lastModified;
        final java.util.Map<String, String> metadata;

        SimulatedObject(String key, byte[] data, String contentType, java.util.Map<String, String> metadata) {
            this.key = key;
            this.data = data;
            this.contentType = contentType;
            this.size = data.length;
            this.lastModified = System.currentTimeMillis();
            this.metadata = metadata != null ? metadata : new java.util.LinkedHashMap<>();
        }
    }

    /** 模拟 MinIO Bucket */
    static class SimulatedBucket {
        final String name;
        final long createTime;
        private final java.util.Map<String, SimulatedObject> objects = new java.util.LinkedHashMap<>();

        SimulatedBucket(String name) {
            this.name = name;
            this.createTime = System.currentTimeMillis();
        }

        void putObject(String key, byte[] data, String contentType, java.util.Map<String, String> metadata) {
            objects.put(key, new SimulatedObject(key, data, contentType, metadata));
        }

        SimulatedObject getObject(String key) { return objects.get(key); }

        boolean removeObject(String key) { return objects.remove(key) != null; }

        java.util.List<SimulatedObject> listObjects(String prefix) {
            return objects.entrySet().stream()
                    .filter(e -> prefix == null || e.getKey().startsWith(prefix))
                    .map(java.util.Map.Entry::getValue)
                    .collect(java.util.stream.Collectors.toList());
        }

        int objectCount() { return objects.size(); }
    }

    /** 模拟 MinIO 服务 */
    static class SimulatedMinIO {
        private final java.util.Map<String, SimulatedBucket> buckets = new java.util.LinkedHashMap<>();

        void makeBucket(String name) {
            if (buckets.containsKey(name)) throw new RuntimeException("Bucket 已存在: " + name);
            buckets.put(name, new SimulatedBucket(name));
        }

        boolean bucketExists(String name) { return buckets.containsKey(name); }

        void removeBucket(String name) {
            SimulatedBucket bucket = buckets.get(name);
            if (bucket == null) throw new RuntimeException("Bucket 不存在: " + name);
            if (bucket.objectCount() > 0) throw new RuntimeException("Bucket 非空，无法删除");
            buckets.remove(name);
        }

        java.util.List<String> listBuckets() { return new java.util.ArrayList<>(buckets.keySet()); }

        SimulatedBucket getBucket(String name) {
            SimulatedBucket bucket = buckets.get(name);
            if (bucket == null) throw new RuntimeException("Bucket 不存在: " + name);
            return bucket;
        }

        /** 生成预签名 URL（模拟） */
        String presignedGetUrl(String bucket, String key, int expirySeconds) {
            return String.format("http://localhost:9000/%s/%s?X-Amz-Expires=%d&X-Amz-Signature=abc123",
                    bucket, key, expirySeconds);
        }

        String presignedPutUrl(String bucket, String key, int expirySeconds) {
            return String.format("http://localhost:9000/%s/%s?X-Amz-Expires=%d&X-Amz-Signature=def456&method=PUT",
                    bucket, key, expirySeconds);
        }
    }

    /** 模拟分片上传 */
    static class MultipartUpload {
        final String bucket;
        final String key;
        final String uploadId;
        private final java.util.Map<Integer, byte[]> parts = new java.util.TreeMap<>();

        MultipartUpload(String bucket, String key) {
            this.bucket = bucket;
            this.key = key;
            this.uploadId = java.util.UUID.randomUUID().toString().substring(0, 8);
        }

        void uploadPart(int partNumber, byte[] data) {
            parts.put(partNumber, data);
        }

        byte[] complete() {
            // 合并所有分片
            int totalSize = parts.values().stream().mapToInt(b -> b.length).sum();
            byte[] result = new byte[totalSize];
            int offset = 0;
            for (byte[] part : parts.values()) {
                System.arraycopy(part, 0, result, offset, part.length);
                offset += part.length;
            }
            return result;
        }

        int partCount() { return parts.size(); }
    }

    // ==================== Part A 演示方法 ====================

    static void demoBucketManagement() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示1：桶（Bucket）管理");
        System.out.println("═══════════════════════════════════════════════════");

        SimulatedMinIO minio = new SimulatedMinIO();

        System.out.println("\n  创建桶：");
        minio.makeBucket("my-images");
        minio.makeBucket("my-documents");
        minio.makeBucket("my-backups");
        System.out.printf("    创建 3 个桶: %s%n", minio.listBuckets());

        System.out.printf("    bucketExists('my-images') = %s%n", minio.bucketExists("my-images"));

        minio.removeBucket("my-backups");
        System.out.printf("    删除 my-backups 后: %s%n", minio.listBuckets());
        System.out.println();
    }

    static void demoObjectOperations() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示2：对象（Object）操作");
        System.out.println("═══════════════════════════════════════════════════");

        SimulatedMinIO minio = new SimulatedMinIO();
        minio.makeBucket("my-files");
        SimulatedBucket bucket = minio.getBucket("my-files");

        // 上传对象
        System.out.println("\n  【putObject】上传文件：");
        bucket.putObject("docs/readme.txt", "这是一个说明文件".getBytes(), "text/plain", null);
        bucket.putObject("images/logo.png", new byte[1024], "image/png", null);
        bucket.putObject("images/banner.jpg", new byte[2048], "image/jpeg", null);
        bucket.putObject("data/users.csv", "id,name,age\n1,张三,25".getBytes(), "text/csv", null);
        System.out.printf("    上传 4 个文件，桶中共 %d 个对象%n", bucket.objectCount());

        // 列出对象
        System.out.println("\n  【listObjects】列出 images/ 前缀的对象：");
        for (SimulatedObject obj : bucket.listObjects("images/")) {
            System.out.printf("    %s (%d bytes, %s)%n", obj.key, obj.size, obj.contentType);
        }

        // 下载对象
        System.out.println("\n  【getObject】下载 docs/readme.txt：");
        SimulatedObject readme = bucket.getObject("docs/readme.txt");
        System.out.printf("    内容: %s%n", new String(readme.data));

        // 删除对象
        System.out.println("\n  【removeObject】删除 data/users.csv：");
        bucket.removeObject("data/users.csv");
        System.out.printf("    删除后桶中共 %d 个对象%n", bucket.objectCount());
        System.out.println();
    }

    static void demoMultipartUpload() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示3：分片上传（Multipart Upload）");
        System.out.println("═══════════════════════════════════════════════════");

        // 模拟上传一个 50MB 的文件，分 5 片
        int fileSize = 50 * 1024 * 1024; // 50MB
        int partSize = 10 * 1024 * 1024; // 10MB per part
        int partCount = (int) Math.ceil((double) fileSize / partSize);

        System.out.printf("\n  文件大小: %dMB, 分片大小: %dMB, 分片数: %d%n",
                fileSize / 1024 / 1024, partSize / 1024 / 1024, partCount);

        MultipartUpload upload = new MultipartUpload("my-files", "large-file.zip");
        System.out.printf("  创建分片上传: uploadId=%s%n", upload.uploadId);

        // 上传各分片
        for (int i = 1; i <= partCount; i++) {
            int size = Math.min(partSize, fileSize - (i - 1) * partSize);
            upload.uploadPart(i, new byte[size]);
            System.out.printf("    上传分片 %d/%d (%dMB)%n", i, partCount, size / 1024 / 1024);
        }

        // 合并分片
        byte[] complete = upload.complete();
        System.out.printf("  合并完成: 总大小 %dMB%n", complete.length / 1024 / 1024);

        System.out.println("\n  分片上传流程：");
        System.out.println("    1. InitiateMultipartUpload → 获取 uploadId");
        System.out.println("    2. UploadPart × N → 上传各分片（可并行）");
        System.out.println("    3. CompleteMultipartUpload → 合并分片");
        System.out.println("    失败时：AbortMultipartUpload → 清理已上传的分片");
        System.out.println();
    }

    static void demoPresignedUrl() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示4：预签名 URL（Presigned URL）");
        System.out.println("═══════════════════════════════════════════════════");

        SimulatedMinIO minio = new SimulatedMinIO();
        minio.makeBucket("my-files");

        // 生成下载预签名 URL
        String getUrl = minio.presignedGetUrl("my-files", "docs/report.pdf", 3600);
        System.out.printf("\n  【GET 预签名 URL】（1小时有效）：%n    %s%n", getUrl);

        // 生成上传预签名 URL
        String putUrl = minio.presignedPutUrl("my-files", "uploads/photo.jpg", 600);
        System.out.printf("\n  【PUT 预签名 URL】（10分钟有效）：%n    %s%n", putUrl);

        System.out.println("\n  预签名 URL 应用场景：");
        System.out.println("    1. 前端直传：后端生成 PUT 预签名 URL，前端直接上传到 MinIO");
        System.out.println("    2. 临时下载：生成 GET 预签名 URL 分享给用户");
        System.out.println("    3. 避免文件经过后端服务器，减少带宽压力");
        System.out.println();
    }

    // ==================== 主方法 ====================

    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║  MinIO 对象存储演示（混合模式）                         ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();

        System.out.println("══════════ Part A：模拟 MinIO 操作 ══════════");
        System.out.println();
        demoBucketManagement();
        demoObjectOperations();
        demoMultipartUpload();
        demoPresignedUrl();

        if (args.length > 0 && "real".equals(args[0])) {
            System.out.println("══════════ Part B：MinIO Java SDK ══════════");
            System.out.println();
            RealMinIO.run();
        } else {
            System.out.println("提示：运行 Part B 请传入参数 'real'");
            System.out.println("  启动 MinIO: docker compose -f docker/docker-compose.yml up -d minio");
            System.out.println("  控制台: http://localhost:9001 (minioadmin/minioadmin)");
        }
    }

    // ==================== Part B：真实 MinIO ====================

    static class RealMinIO {

        static final String ENDPOINT = "http://localhost:9000";
        static final String ACCESS_KEY = "minioadmin";
        static final String SECRET_KEY = "minioadmin";

        static void run() throws Exception {
            io.minio.MinioClient client = io.minio.MinioClient.builder()
                    .endpoint(ENDPOINT)
                    .credentials(ACCESS_KEY, SECRET_KEY)
                    .build();

            String bucketName = "demo-bucket";

            try {
                // 1. 创建桶
                System.out.println("  【makeBucket】");
                if (!client.bucketExists(io.minio.BucketExistsArgs.builder().bucket(bucketName).build())) {
                    client.makeBucket(io.minio.MakeBucketArgs.builder().bucket(bucketName).build());
                    System.out.println("    创建桶: " + bucketName);
                } else {
                    System.out.println("    桶已存在: " + bucketName);
                }

                // 2. 上传对象
                System.out.println("\n  【putObject】上传文件：");
                String content = "Hello MinIO! 这是一个测试文件。\n包含中文内容。";
                byte[] data = content.getBytes("UTF-8");
                client.putObject(io.minio.PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object("test/hello.txt")
                        .stream(new java.io.ByteArrayInputStream(data), data.length, -1)
                        .contentType("text/plain")
                        .build());
                System.out.println("    上传: test/hello.txt (" + data.length + " bytes)");

                // 上传第二个文件
                byte[] jsonData = "{\"name\":\"张三\",\"age\":25}".getBytes("UTF-8");
                client.putObject(io.minio.PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object("test/user.json")
                        .stream(new java.io.ByteArrayInputStream(jsonData), jsonData.length, -1)
                        .contentType("application/json")
                        .build());
                System.out.println("    上传: test/user.json (" + jsonData.length + " bytes)");

                // 3. 列出对象
                System.out.println("\n  【listObjects】列出 test/ 前缀的对象：");
                for (io.minio.Result<io.minio.messages.Item> result :
                        client.listObjects(io.minio.ListObjectsArgs.builder()
                                .bucket(bucketName).prefix("test/").build())) {
                    io.minio.messages.Item item = result.get();
                    System.out.printf("    %s (%d bytes)%n", item.objectName(), item.size());
                }

                // 4. 下载对象
                System.out.println("\n  【getObject】下载 test/hello.txt：");
                try (java.io.InputStream is = client.getObject(io.minio.GetObjectArgs.builder()
                        .bucket(bucketName).object("test/hello.txt").build())) {
                    String downloaded = new String(is.readAllBytes(), "UTF-8");
                    System.out.println("    内容: " + downloaded.replace("\n", "\\n"));
                }

                // 5. 获取对象信息
                System.out.println("\n  【statObject】获取对象元信息：");
                io.minio.StatObjectResponse stat = client.statObject(io.minio.StatObjectArgs.builder()
                        .bucket(bucketName).object("test/hello.txt").build());
                System.out.printf("    size=%d, contentType=%s, lastModified=%s%n",
                        stat.size(), stat.contentType(), stat.lastModified());

                // 6. 生成预签名 URL
                System.out.println("\n  【presignedGetObject】生成下载预签名 URL（1小时有效）：");
                String presignedUrl = client.getPresignedObjectUrl(
                        io.minio.GetPresignedObjectUrlArgs.builder()
                                .bucket(bucketName)
                                .object("test/hello.txt")
                                .method(io.minio.http.Method.GET)
                                .expiry(3600)
                                .build());
                System.out.println("    " + presignedUrl.substring(0, Math.min(100, presignedUrl.length())) + "...");

                // 7. 删除对象
                System.out.println("\n  【removeObject】删除 test/user.json：");
                client.removeObject(io.minio.RemoveObjectArgs.builder()
                        .bucket(bucketName).object("test/user.json").build());
                System.out.println("    已删除 test/user.json");

            } finally {
                // 清理（如需保留数据在控制台查看，注释掉 cleanup 调用即可）
                cleanup(client, bucketName);
            }
        }

        /** 清理测试桶和对象。如需保留数据，注释掉 cleanup() 调用即可 */
        static void cleanup(io.minio.MinioClient client, String bucketName) throws Exception {
            // 先删除桶中所有对象
            for (io.minio.Result<io.minio.messages.Item> result :
                    client.listObjects(io.minio.ListObjectsArgs.builder()
                            .bucket(bucketName).recursive(true).build())) {
                client.removeObject(io.minio.RemoveObjectArgs.builder()
                        .bucket(bucketName).object(result.get().objectName()).build());
            }
            client.removeBucket(io.minio.RemoveBucketArgs.builder().bucket(bucketName).build());
            System.out.println("\n  清理：已删除测试桶 " + bucketName);
            System.out.println("  提示：如需保留数据，注释掉 cleanup() 调用即可");
        }
    }
}
