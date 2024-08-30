package com.zxzinn.novelai.service.generation;

import com.zxzinn.novelai.api.GenerationPayload;

public class GenerationPayloadFactory {
    public static GenerationPayload createPayload(String generationMode, GenerationParameters params) {
        GenerationStrategy strategy;
        if ("Text2Image".equals(generationMode)) {
            strategy = new Text2ImgStrategy();
        } else {
            strategy = new Img2ImgStrategy();
        }
        return strategy.createPayload(params);
    }
}