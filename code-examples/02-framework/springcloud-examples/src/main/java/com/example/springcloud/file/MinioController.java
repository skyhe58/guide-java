package com.example.springcloud.file;

import com.example.springcloud.common.Result;
import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 10.A.22 MinIO 文件存储 Controller
 *
 * <p>演示 MinIO 对象存储的核心功能：
 * <ul>
 *   <li>文件上传、下载、删除</li>
 *   <li>文件列表查询</li>
 *   <li>预签名 URL 生成</li>
 * </ul>
 *
 * <h3>curl 测试命令：</h3>
 * <pre>
 * # 上传文件
 * curl -X POST http://localhost:8090/demo/file/upload \
 *   -F "file=@/path/to/file.txt"
 *
 * # 下载文件
 * curl -O http://localhost:8090/demo/file/download/file.txt
 *
 * # 列出所有文件
 * curl http://localhost:8090/demo/file/list
 *
 * # 删除文件
 * curl -X DELETE http://localhost:8090/demo/file/file.txt
 *
 * # 获取预签名下载 URL
 * curl http://localhost:8090/demo/file/presigned/file.txt
 * </pre>
 */
@RestController
@RequestMapping("/demo/file")
public class MinioController {

    private static final Logger log = LoggerFactory.getLogger(MinioController.class);

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucket;

    public MinioController(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    /**
     * 确保 bucket 存在，不存在则创建
     */
    private void ensureBucketExists() throws Exception {
        boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            log.info("[MinIO] 创建 bucket: {}", bucket);
        }
    }

    // ==================== 文件操作 ====================

    /**
     * 上传文件
     *
     * <pre>
     * curl -X POST http://localhost:8090/demo/file/upload \
     *   -F "file=@/path/to/file.txt"
     * </pre>
     *
     * @param file 上传的文件
     * @return 上传结果
     */
    @PostMapping("/upload")
    public Result<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) {
        try {
            ensureBucketExists();

            String fileName = file.getOriginalFilename();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(fileName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());

            log.info("[MinIO] 文件上传成功: fileName={}, size={}", fileName, file.getSize());

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("说明", "文件上传成功");
            data.put("fileName", fileName);
            data.put("size", file.getSize());
            data.put("contentType", file.getContentType());
            data.put("bucket", bucket);
            return Result.ok(data);
        } catch (Exception e) {
            log.error("[MinIO] 文件上传失败", e);
            return Result.fail(500, "文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 下载文件
     *
     * <pre>
     * curl -O http://localhost:8090/demo/file/download/file.txt
     * </pre>
     *
     * @param fileName 文件名
     * @return 文件内容
     */
    @GetMapping("/download/{fileName}")
    public ResponseEntity<byte[]> download(@PathVariable String fileName) {
        try {
            GetObjectResponse response = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(fileName)
                    .build());

            byte[] content = response.readAllBytes();
            response.close();

            log.info("[MinIO] 文件下载: fileName={}, size={}", fileName, content.length);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(content);
        } catch (Exception e) {
            log.error("[MinIO] 文件下载失败: fileName={}", fileName, e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 列出所有文件
     *
     * <pre>
     * curl http://localhost:8090/demo/file/list
     * </pre>
     *
     * @return 文件列表
     */
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list() {
        try {
            ensureBucketExists();

            Iterable<io.minio.Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder().bucket(bucket).build());

            List<Map<String, Object>> files = new ArrayList<>();
            for (io.minio.Result<Item> result : results) {
                Item item = result.get();
                Map<String, Object> fileInfo = new LinkedHashMap<>();
                fileInfo.put("fileName", item.objectName());
                fileInfo.put("size", item.size());
                fileInfo.put("lastModified", item.lastModified());
                fileInfo.put("isDir", item.isDir());
                files.add(fileInfo);
            }

            log.info("[MinIO] 列出文件，共 {} 个", files.size());
            return Result.ok(files);
        } catch (Exception e) {
            log.error("[MinIO] 列出文件失败", e);
            return Result.fail(500, "列出文件失败: " + e.getMessage());
        }
    }

    /**
     * 删除文件
     *
     * <pre>
     * curl -X DELETE http://localhost:8090/demo/file/file.txt
     * </pre>
     *
     * @param fileName 文件名
     * @return 删除结果
     */
    @DeleteMapping("/{fileName}")
    public Result<String> delete(@PathVariable String fileName) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(fileName)
                    .build());

            log.info("[MinIO] 文件删除成功: fileName={}", fileName);
            return Result.ok("文件删除成功: " + fileName);
        } catch (Exception e) {
            log.error("[MinIO] 文件删除失败: fileName={}", fileName, e);
            return Result.fail(500, "文件删除失败: " + e.getMessage());
        }
    }

    /**
     * 获取预签名下载 URL
     *
     * <p>生成有效期 1 小时的预签名下载链接。
     *
     * <pre>
     * curl http://localhost:8090/demo/file/presigned/file.txt
     * </pre>
     *
     * @param fileName 文件名
     * @return 预签名 URL
     */
    @GetMapping("/presigned/{fileName}")
    public Result<Map<String, Object>> presigned(@PathVariable String fileName) {
        try {
            String url = minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucket)
                    .object(fileName)
                    .expiry(1, TimeUnit.HOURS)
                    .build());

            log.info("[MinIO] 生成预签名 URL: fileName={}", fileName);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("说明", "预签名下载 URL（有效期 1 小时）");
            data.put("fileName", fileName);
            data.put("url", url);
            data.put("有效期", "1 小时");
            return Result.ok(data);
        } catch (Exception e) {
            log.error("[MinIO] 生成预签名 URL 失败: fileName={}", fileName, e);
            return Result.fail(500, "生成预签名 URL 失败: " + e.getMessage());
        }
    }
}
