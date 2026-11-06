package com.example.demo.Dto;

import java.util.List;

public record CreateOrderRequest(List<OrderItemRequest> items) {}
