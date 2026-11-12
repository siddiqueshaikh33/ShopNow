package com.example.demo.Dto;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PaginatedProductResponse {

    private List<ProductResponseDto> products;
    private int currentPage;
    private int pageSize;
    private boolean hasMore;
    private Map<String, String> user;

    public PaginatedProductResponse() {
    }

    public PaginatedProductResponse(List<ProductResponseDto> products, int currentPage, int pageSize, boolean hasMore, Map<String, String> user) {
        this.products = products;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        this.hasMore = hasMore;
        this.user = user;
    }

    public List<ProductResponseDto> getProducts() {
        return products;
    }

    public void setProducts(List<ProductResponseDto> products) {
        this.products = products;
    }

    // Backward-compatible alias for existing consumers and tests
    @JsonProperty("product")
    public List<ProductResponseDto> getProduct() {
        return products;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public boolean isHasMore() {
        return hasMore;
    }

    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
    }

    public Map<String, String> getUser() {
        return user;
    }

    public void setUser(Map<String, String> user) {
        this.user = user;
    }
}

