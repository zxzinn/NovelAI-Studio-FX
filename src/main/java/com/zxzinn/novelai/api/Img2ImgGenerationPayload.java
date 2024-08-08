package com.zxzinn.novelai.api;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class Img2ImgGenerationPayload extends GenerationPayload {
    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class Img2ImgGenerationParameters extends GenerationParameters {
        public double strength;
        public int noise;
        public boolean dynamic_thresholding;
        public double controlnet_strength;
        public boolean legacy;
        public boolean add_original_image;
        public int cfg_rescale;
        public String noise_schedule;
        public boolean legacy_v3_extend;
        public String image;
        public long extra_noise_seed;
    }
}