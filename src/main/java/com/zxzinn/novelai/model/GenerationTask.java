package com.zxzinn.novelai.model;

import com.zxzinn.novelai.api.GenerationPayload;
import lombok.Getter;

public record GenerationTask(GenerationPayload payload, String apiKey) {
}