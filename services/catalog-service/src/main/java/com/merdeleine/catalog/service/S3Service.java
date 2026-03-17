package com.merdeleine.catalog.service;

import com.merdeleine.catalog.config.S3Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Service
public class S3Service {

    private static final Logger log = LoggerFactory.getLogger(S3Service.class);

    private final S3Client s3Client;
    private final S3Properties s3Properties;

    public S3Service(S3Client s3Client, S3Properties s3Properties) {
        this.s3Client = s3Client;
        this.s3Properties = s3Properties;
    }

    /**
     * 上傳圖片至 S3
     *
     * @param folder    S3 目錄前綴，例如 "products/uuid"
     * @param file      上傳的圖片檔案
     * @return          S3UploadResult (bucket, key, cdnUrl)
     */
    public S3UploadResult upload(String folder, MultipartFile file) {
        String bucket  = s3Properties.getBucket();
        String key     = buildKey(folder, file.getOriginalFilename());
        String content = resolveContentType(file);

        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(content)
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromBytes(file.getBytes()));
            log.info("S3 upload success: s3://{}/{}", bucket, key);

        } catch (IOException e) {
            throw new RuntimeException("Failed to read uploaded file: " + file.getOriginalFilename(), e);
        }

        String cdnUrl = buildCdnUrl(key);
        return new S3UploadResult(bucket, key, cdnUrl);
    }

    /**
     * 上傳 byte[] 到指定的 S3 Key（用於 resize 後的固定路徑圖片）
     *
     * @param key         完整的 S3 object key，例如 products/{productId}/{groupId}/200.jpg
     * @param data        圖片 byte array
     * @param contentType MIME type，例如 image/jpeg
     */
    public S3UploadResult uploadBytes(String key, byte[] data, String contentType) {
        String bucket = s3Properties.getBucket();

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .contentLength((long) data.length)
                .build();

        s3Client.putObject(putRequest, RequestBody.fromBytes(data));
        log.info("S3 upload success: s3://{}/{}", bucket, key);

        return new S3UploadResult(bucket, key, buildCdnUrl(key));
    }

    /**
     * 刪除 S3 物件
     */
    public void delete(String key) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(s3Properties.getBucket())
                .key(key)
                .build());
        log.info("S3 delete success: s3://{}/{}", s3Properties.getBucket(), key);
    }

    // ─── private helpers ────────────────────────────────────────────────────────

    /**
     * 產生唯一 S3 Key：{folder}/{uuid}_{sanitized-filename}
     */
    private String buildKey(String folder, String originalFilename) {
        String safe = originalFilename == null ? "image" : sanitize(originalFilename);
        return folder + "/" + UUID.randomUUID() + "_" + safe;
    }

    /**
     * 移除路徑分隔符等危險字元
     */
    private String sanitize(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9.\\-_]", "_");
    }

    /**
     * 若 MultipartFile 沒有帶 Content-Type，fallback 到 application/octet-stream
     */
    private String resolveContentType(MultipartFile file) {
        String ct = file.getContentType();
        return (ct != null && !ct.isBlank()) ? ct : "application/octet-stream";
    }

    /**
     * 若有設定 CDN base url 則組合完整 CDN 連結，否則回傳 null
     */
    private String buildCdnUrl(String key) {
        String base = s3Properties.getCdnBaseUrl();
        if (base == null || base.isBlank()) return null;
        return base.stripTrailing() + "/" + key;
    }
}

