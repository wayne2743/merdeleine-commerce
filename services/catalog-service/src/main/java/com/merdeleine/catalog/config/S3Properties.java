package com.merdeleine.catalog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "aws.s3")
public class S3Properties {

    private String region;
    private String bucket;
    private String accessKey;
    private String secretKey;
    private String cdnBaseUrl;

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getBucket() { return bucket; }
    public void setBucket(String bucket) { this.bucket = bucket; }

    public String getAccessKey() { return accessKey; }
    public void setAccessKey(String accessKey) { this.accessKey = accessKey; }

    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }

    public String getCdnBaseUrl() { return cdnBaseUrl; }
    public void setCdnBaseUrl(String cdnBaseUrl) { this.cdnBaseUrl = cdnBaseUrl; }
}

