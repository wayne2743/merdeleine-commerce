package com.merdeleine.catalog.service;

/**
 * S3 上傳後的回傳結果
 */
public class S3UploadResult {

    private final String bucket;
    private final String key;
    private final String cdnUrl;

    public S3UploadResult(String bucket, String key, String cdnUrl) {
        this.bucket = bucket;
        this.key = key;
        this.cdnUrl = cdnUrl;
    }

    public String getBucket() { return bucket; }
    public String getKey() { return key; }
    public String getCdnUrl() { return cdnUrl; }
}

