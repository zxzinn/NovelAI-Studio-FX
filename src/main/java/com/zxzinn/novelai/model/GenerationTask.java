package com.zxzinn.novelai.model;

import com.zxzinn.novelai.api.GenerationPayload;
import lombok.Getter;

@Getter
public class GenerationTask {
    private final GenerationPayload payload;
    private final String apiKey;

    public GenerationTask(GenerationPayload payload, String apiKey) {
        this.payload = payload;
        this.apiKey = apiKey;
    }
}