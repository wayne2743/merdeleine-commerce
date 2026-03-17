package com.merdeleine.catalog.controller;

import com.merdeleine.catalog.dto.ProductImageResponse;
import com.merdeleine.catalog.dto.ProductImageUpdateRequest;
import com.merdeleine.catalog.service.ProductImageService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/products/{productId}/images")
public class ProductImageController {

    private final ProductImageService productImageService;

    public ProductImageController(ProductImageService productImageService) {
        this.productImageService = productImageService;
    }

    /**
     * 上傳一張圖片，系統自動產生 original / 200px / 400px / 800px 四個版本上傳至 S3，
     * 並將四筆 metadata 寫入 DB。
     *
     * S3 路徑：
     *   products/{productId}/{groupId}/original.{ext}
     *   products/{productId}/{groupId}/200.jpg   ← 列表縮圖
     *   products/{productId}/{groupId}/400.jpg   ← 商品頁
     *   products/{productId}/{groupId}/800.jpg   ← zoom
     *
     * @param file      圖片檔案 (required)
     * @param sortOrder 排序，預設 0
     * @param isPrimary 是否為主圖，預設 false（僅標記 original 那筆）
     * @param isActive  是否啟用，預設 true
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<ProductImageResponse>> createImage(
            @PathVariable UUID productId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "sortOrder",  defaultValue = "0")     Integer sortOrder,
            @RequestParam(value = "isPrimary",  defaultValue = "false")  Boolean isPrimary,
            @RequestParam(value = "isActive",   defaultValue = "true")   Boolean isActive) {

        List<ProductImageResponse> responses =
                productImageService.createImage(productId, file, sortOrder, isPrimary, isActive);
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    /** 查詢產品所有圖片 */
    @GetMapping
    public ResponseEntity<List<ProductImageResponse>> getImagesByProduct(
            @PathVariable UUID productId,
            @RequestParam(required = false, defaultValue = "false") Boolean activeOnly) {
        return ResponseEntity.ok(productImageService.getImagesByProduct(productId, activeOnly));
    }

    /** 查詢單一圖片 */
    @GetMapping("/{imageId}")
    public ResponseEntity<ProductImageResponse> getImageById(
            @PathVariable UUID productId,
            @PathVariable UUID imageId) {
        return ResponseEntity.ok(productImageService.getImageById(productId, imageId));
    }

    /** 更新圖片 metadata */
    @PutMapping("/{imageId}")
    public ResponseEntity<ProductImageResponse> updateImage(
            @PathVariable UUID productId,
            @PathVariable UUID imageId,
            @Valid @RequestBody ProductImageUpdateRequest request) {
        return ResponseEntity.ok(productImageService.updateImage(productId, imageId, request));
    }

    /** 刪除圖片（同步刪除 S3 物件） */
    @DeleteMapping("/{imageId}")
    public ResponseEntity<Void> deleteImage(
            @PathVariable UUID productId,
            @PathVariable UUID imageId) {
        productImageService.deleteImage(productId, imageId);
        return ResponseEntity.noContent().build();
    }

    /** 設定主圖 */
    @PatchMapping("/{imageId}/set-primary")
    public ResponseEntity<ProductImageResponse> setPrimary(
            @PathVariable UUID productId,
            @PathVariable UUID imageId) {
        return ResponseEntity.ok(productImageService.setPrimary(productId, imageId));
    }
}
