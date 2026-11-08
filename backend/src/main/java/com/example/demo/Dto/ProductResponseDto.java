package com.example.demo.Dto;

import java.math.BigDecimal;

import com.example.demo.Entity.Products;

public record ProductResponseDto(
    int product_id,
    String name,
    String description,
    BigDecimal price,
    Integer stock,
    String product_img
) {
    public static ProductResponseDto fromEntity(Products product) {
        if (product == null) {
            return null;
        }
        String imageUrl = (product.getPrimaryImage() != null) ? product.getPrimaryImage().getImageUrl() : null;
        return new ProductResponseDto(
            product.getProduct_id(),
            product.getName(),
            product.getDescription(),
            product.getPrice(),
            product.getStock(),
            imageUrl
        );
    }
}

