package com.zxzinn.novelai.api;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Img2ImgGenerationPayload extends ImageGenerationPayload {
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class Img2ImgGenerationParameters extends ImageGenerationParameters {
        public double strength = 0.5;
        public int noise = 0;
        public boolean dynamic_thresholding = false;
        public double controlnet_strength = 1;
        public boolean legacy = false;
        public boolean add_original_image = true;
        public int cfg_rescale = 0;
        public String noise_schedule = "native";
        public boolean legacy_v3_extend = false;
        public String image;
        public long extra_noise_seed;
    }

    @Override
    public Img2ImgGenerationParameters getParameters() {
        return (Img2ImgGenerationParameters) super.getParameters();
    }

    public void setParameters(Img2ImgGenerationParameters parameters) {
        super.setParameters(parameters);
    }
}