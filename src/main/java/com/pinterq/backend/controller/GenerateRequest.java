package com.pinterq.backend.controller;

import lombok.Data;

@Data
public class GenerateRequest {
    private Long userId;
    private Long categoryId;
    private String title;
    private String content;
}
