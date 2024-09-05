package com.zxzinn.novelai.model;

import com.zxzinn.novelai.api.GenerationPayload;

public record GenerationTask(GenerationPayload payload, String apiKey) {
}