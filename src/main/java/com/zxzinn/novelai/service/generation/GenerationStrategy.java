package com.zxzinn.novelai.service.generation;


import com.zxzinn.novelai.api.GenerationPayload;

public interface GenerationStrategy {
    GenerationPayload createPayload(GenerationParameters params);
}