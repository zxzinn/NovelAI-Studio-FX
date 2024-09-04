package com.zxzinn.novelai.model;

import lombok.Getter;

@Getter
public class GenerationResult {
    private final byte[] imageData;
    private final boolean isSuccess;
    private final String errorMessage;

    private GenerationResult(byte[] imageData, boolean isSuccess, String errorMessage) {
        this.imageData = imageData;
        this.isSuccess = isSuccess;
        this.errorMessage = errorMessage;
    }

    public static GenerationResult success(byte[] imageData) {
        return new GenerationResult(imageData, true, null);
    }

    public static GenerationResult failure(String errorMessage) {
        return new GenerationResult(null, false, errorMessage);
    }
}