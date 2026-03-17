package com.merdeleine.catalog.dto;

import com.merdeleine.catalog.enums.ImageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ProductImageCreateRequest {

    @NotNull(message = "Image type is required")
    private ImageType imageType;

    private Integer sortOrder = 0;

    @NotBlank(message = "S3 bucket is required")
    @Size(max = 100, message = "S3 bucket must not exceed 100 characters")
    private String s3Bucket;

    @NotBlank(message = "S3 key is required")
    @Size(max = 500, message = "S3 key must not exceed 500 characters")
    private String s3Key;

    @Size(max = 1000, message = "CDN URL must not exceed 1000 characters")
    private String cdnUrl;

    @Size(max = 255, message = "Original filename must not exceed 255 characters")
    private String originalFilename;

    @Size(max = 100, message = "Content type must not exceed 100 characters")
    private String contentType;

    private Long fileSize;

    private Integer width;

    private Integer height;

    private Boolean isPrimary = false;

    private Boolean isActive = true;

    public ProductImageCreateRequest() {}

    public ImageType getImageType() { return imageType; }
    public void setImageType(ImageType imageType) { this.imageType = imageType; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public String getS3Bucket() { return s3Bucket; }
    public void setS3Bucket(String s3Bucket) { this.s3Bucket = s3Bucket; }

    public String getS3Key() { return s3Key; }
    public void setS3Key(String s3Key) { this.s3Key = s3Key; }

    public String getCdnUrl() { return cdnUrl; }
    public void setCdnUrl(String cdnUrl) { this.cdnUrl = cdnUrl; }

    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public Integer getWidth() { return width; }
    public void setWidth(Integer width) { this.width = width; }

    public Integer getHeight() { return height; }
    public void setHeight(Integer height) { this.height = height; }

    public Boolean getIsPrimary() { return isPrimary; }
    public void setIsPrimary(Boolean isPrimary) { this.isPrimary = isPrimary; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}

