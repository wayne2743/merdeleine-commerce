package com.merdeleine.catalog.service;

import com.merdeleine.catalog.dto.ProductImageResponse;
import com.merdeleine.catalog.dto.ProductImageUpdateRequest;
import com.merdeleine.catalog.entity.Product;
import com.merdeleine.catalog.entity.ProductImage;
import com.merdeleine.catalog.enums.ImageType;
import com.merdeleine.catalog.exception.ProductImageNotFoundException;
import com.merdeleine.catalog.exception.ProductNotFoundException;
import com.merdeleine.catalog.repository.ProductImageRepository;
import com.merdeleine.catalog.repository.ProductRepository;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductImageService {

    /** 自動產生的尺寸：寬度 → ImageType */
    private static final int[] RESIZE_WIDTHS       = {200,              400,             800};
    private static final ImageType[] RESIZE_TYPES  = {ImageType.THUMBNAIL, ImageType.GALLERY, ImageType.DETAIL};

    private final ProductImageRepository productImageRepository;
    private final ProductRepository productRepository;
    private final S3Service s3Service;

    public ProductImageService(ProductImageRepository productImageRepository,
                               ProductRepository productRepository,
                               S3Service s3Service) {
        this.productImageRepository = productImageRepository;
        this.productRepository = productRepository;
        this.s3Service = s3Service;
    }

    /**
     * 上傳一次圖片，自動產生 original / 200 / 400 / 800 四個版本：
     * <pre>
     *   products/{productId}/{groupId}/original.{ext}
     *   products/{productId}/{groupId}/200.jpg
     *   products/{productId}/{groupId}/400.jpg
     *   products/{productId}/{groupId}/800.jpg
     * </pre>
     * 四筆 product_image 記錄一起寫入 DB，回傳全部。
     */
    public List<ProductImageResponse> createImage(UUID productId,
                                                  MultipartFile file,
                                                  Integer sortOrder,
                                                  Boolean isPrimary,
                                                  Boolean isActive) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        // ── 1. 讀取原始 bytes（只讀一次，供後續 ImageIO / Thumbnailator 重複使用）
        byte[] originalBytes;
        try {
            originalBytes = file.getBytes();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read uploaded file: " + file.getOriginalFilename(), e);
        }

        // ── 2. 解析原圖尺寸
        Integer origWidth = null, origHeight = null;
        try {
            BufferedImage bi = ImageIO.read(new ByteArrayInputStream(originalBytes));
            if (bi != null) {
                origWidth  = bi.getWidth();
                origHeight = bi.getHeight();
            }
        } catch (IOException ignored) { /* 解析失敗不影響主流程 */ }

        String ext         = extractExtension(file.getOriginalFilename());
        String contentType = resolveContentType(file);
        String groupId     = UUID.randomUUID().toString();
        String basePath    = "products/" + productId + "/" + groupId;

        int     effectiveSort    = sortOrder != null ? sortOrder : 0;
        boolean effectivePrimary = Boolean.TRUE.equals(isPrimary);
        boolean effectiveActive  = isActive == null || isActive;

        List<ProductImage> saved = new ArrayList<>();

        // ── 3. 上傳原圖
        String origKey = basePath + "/original." + ext;
        S3UploadResult origResult = s3Service.uploadBytes(origKey, originalBytes, contentType);

        ProductImage origImage = buildRecord(product, ImageType.ORIGINAL, effectiveSort,
                origResult, file.getOriginalFilename(), contentType,
                (long) originalBytes.length, origWidth, origHeight,
                effectivePrimary, effectiveActive);
        saved.add(productImageRepository.save(origImage));

        // ── 4. 逐一 resize 並上傳 200 / 400 / 800
        for (int i = 0; i < RESIZE_WIDTHS.length; i++) {
            int       targetWidth = RESIZE_WIDTHS[i];
            ImageType imgType     = RESIZE_TYPES[i];

            byte[] resizedBytes = resizeToWidth(originalBytes, targetWidth);

            // 計算 resize 後的實際尺寸
            int actualWidth  = (origWidth  != null && origWidth  < targetWidth) ? origWidth  : targetWidth;
            Integer resizedHeight = null;
            if (origWidth != null && origHeight != null && origWidth > 0) {
                resizedHeight = (int) Math.round((double) origHeight / origWidth * actualWidth);
            }

            String sizedKey    = basePath + "/" + targetWidth + ".jpg";
            S3UploadResult res = s3Service.uploadBytes(sizedKey, resizedBytes, "image/jpeg");

            ProductImage sizedImage = buildRecord(product, imgType, effectiveSort,
                    res, targetWidth + ".jpg", "image/jpeg",
                    (long) resizedBytes.length, actualWidth, resizedHeight,
                    false, effectiveActive);
            saved.add(productImageRepository.save(sizedImage));
        }

        // ── 5. 若設為 primary，清除同產品其他圖片的旗標
        if (effectivePrimary) {
            productImageRepository.clearPrimaryByProductId(productId, origImage.getId());
        }

        return saved.stream().map(ProductImageResponse::fromEntity).collect(Collectors.toList());
    }

    // ────────────────────────────────────────────────────────────────────────────
    // 以下方法維持原有邏輯
    // ────────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ProductImageResponse> getImagesByProduct(UUID productId, Boolean activeOnly) {
        if (!productRepository.existsById(productId)) {
            throw new ProductNotFoundException(productId);
        }
        List<ProductImage> images = Boolean.TRUE.equals(activeOnly)
                ? productImageRepository.findByProductIdAndIsActiveOrderBySortOrderAsc(productId, true)
                : productImageRepository.findByProductIdOrderBySortOrderAsc(productId);

        return images.stream().map(ProductImageResponse::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductImageResponse getImageById(UUID productId, UUID imageId) {
        ProductImage image = productImageRepository.findByIdAndProductId(imageId, productId)
                .orElseThrow(() -> new ProductImageNotFoundException(productId, imageId));
        return ProductImageResponse.fromEntity(image);
    }

    public ProductImageResponse updateImage(UUID productId, UUID imageId, ProductImageUpdateRequest request) {
        ProductImage image = productImageRepository.findByIdAndProductId(imageId, productId)
                .orElseThrow(() -> new ProductImageNotFoundException(productId, imageId));

        if (request.getImageType()       != null) image.setImageType(request.getImageType());
        if (request.getSortOrder()       != null) image.setSortOrder(request.getSortOrder());
        if (request.getS3Bucket()        != null) image.setS3Bucket(request.getS3Bucket());
        if (request.getS3Key()           != null) image.setS3Key(request.getS3Key());
        if (request.getCdnUrl()          != null) image.setCdnUrl(request.getCdnUrl());
        if (request.getOriginalFilename()!= null) image.setOriginalFilename(request.getOriginalFilename());
        if (request.getContentType()     != null) image.setContentType(request.getContentType());
        if (request.getFileSize()        != null) image.setFileSize(request.getFileSize());
        if (request.getWidth()           != null) image.setWidth(request.getWidth());
        if (request.getHeight()          != null) image.setHeight(request.getHeight());
        if (request.getIsActive()        != null) image.setIsActive(request.getIsActive());

        if (Boolean.TRUE.equals(request.getIsPrimary())) {
            productImageRepository.clearPrimaryByProductId(productId, imageId);
            image.setIsPrimary(true);
        } else if (Boolean.FALSE.equals(request.getIsPrimary())) {
            image.setIsPrimary(false);
        }

        return ProductImageResponse.fromEntity(productImageRepository.save(image));
    }

    public void deleteImage(UUID productId, UUID imageId) {
        ProductImage image = productImageRepository.findByIdAndProductId(imageId, productId)
                .orElseThrow(() -> new ProductImageNotFoundException(productId, imageId));

        if (image.getS3Key() != null) {
            s3Service.delete(image.getS3Key());
        }
        productImageRepository.delete(image);
    }

    public ProductImageResponse setPrimary(UUID productId, UUID imageId) {
        if (!productRepository.existsById(productId)) {
            throw new ProductNotFoundException(productId);
        }
        ProductImage image = productImageRepository.findByIdAndProductId(imageId, productId)
                .orElseThrow(() -> new ProductImageNotFoundException(productId, imageId));

        productImageRepository.clearPrimaryByProductId(productId, imageId);
        image.setIsPrimary(true);
        return ProductImageResponse.fromEntity(productImageRepository.save(image));
    }

    // ────────────────────────────────────────────────────────────────────────────
    // private helpers
    // ────────────────────────────────────────────────────────────────────────────

    /** 使用 Thumbnailator 將圖片寬度縮放至 targetWidth，輸出 JPEG bytes */
    private byte[] resizeToWidth(byte[] originalBytes, int targetWidth) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Thumbnails.of(new ByteArrayInputStream(originalBytes))
                    .width(targetWidth)
                    .keepAspectRatio(true)
                    .outputFormat("jpg")
                    .toOutputStream(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to resize image to " + targetWidth + "px", e);
        }
    }

    /** 建立 ProductImage entity（不含 ID，由 @PrePersist 自動產生） */
    private ProductImage buildRecord(Product product, ImageType imageType, int sortOrder,
                                     S3UploadResult s3Result, String filename,
                                     String contentType, Long fileSize,
                                     Integer width, Integer height,
                                     boolean isPrimary, boolean isActive) {
        ProductImage img = new ProductImage();
        img.setProduct(product);
        img.setImageType(imageType);
        img.setSortOrder(sortOrder);
        img.setS3Bucket(s3Result.getBucket());
        img.setS3Key(s3Result.getKey());
        img.setCdnUrl(s3Result.getCdnUrl());
        img.setOriginalFilename(filename);
        img.setContentType(contentType);
        img.setFileSize(fileSize);
        img.setWidth(width);
        img.setHeight(height);
        img.setIsPrimary(isPrimary);
        img.setIsActive(isActive);
        return img;
    }

    private String extractExtension(String filename) {
        if (filename == null) return "jpg";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot + 1).toLowerCase() : "jpg";
    }

    private String resolveContentType(MultipartFile file) {
        String ct = file.getContentType();
        return (ct != null && !ct.isBlank()) ? ct : "application/octet-stream";
    }
}
