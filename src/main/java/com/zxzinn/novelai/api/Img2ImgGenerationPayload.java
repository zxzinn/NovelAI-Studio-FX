package com.zxzinn.novelai.api;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Img2ImgGenerationPayload extends GenerationPayload {
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class Img2ImgGenerationParameters extends GenerationParameters {
        private double strength = 0.5;
        private int noise = 0;
        private String image;
        private long extra_noise_seed;
    }
}