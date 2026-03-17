package com.merdeleine.catalog.dto;

import com.merdeleine.catalog.entity.ProductImage;
import com.merdeleine.catalog.enums.ImageType;

import java.time.OffsetDateTime;
import java.util.UUID;

public class ProductImageResponse {

    private UUID id;
    private UUID productId;
    private ImageType imageType;
    private Integer sortOrder;
    private String s3Bucket;
    private String s3Key;
    private String cdnUrl;
    private String originalFilename;
    private String contentType;
    private Long fileSize;
    private Integer width;
    private Integer height;
    private Boolean isPrimary;
    private Boolean isActive;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public ProductImageResponse() {}

    public static ProductImageResponse fromEntity(ProductImage image) {
        ProductImageResponse response = new ProductImageResponse();
        response.id = image.getId();
        response.productId = image.getProduct().getId();
        response.imageType = image.getImageType();
        response.sortOrder = image.getSortOrder();
        response.s3Bucket = image.getS3Bucket();
        response.s3Key = image.getS3Key();
        response.cdnUrl = image.getCdnUrl();
        response.originalFilename = image.getOriginalFilename();
        response.contentType = image.getContentType();
        response.fileSize = image.getFileSize();
        response.width = image.getWidth();
        response.height = image.getHeight();
        response.isPrimary = image.getIsPrimary();
        response.isActive = image.getIsActive();
        response.createdAt = image.getCreatedAt();
        response.updatedAt = image.getUpdatedAt();
        return response;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }

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

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}

