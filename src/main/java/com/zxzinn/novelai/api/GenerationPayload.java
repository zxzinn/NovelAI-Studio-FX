package com.zxzinn.novelai.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GenerationPayload {
    private String input;
    private String model;
    private String action;
    private GenerationParameters parameters;

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class GenerationParameters {
        private int params_version;
        private int width;
        private int height;
        private int scale;
        private String sampler;
        private int steps;
        private int n_samples;
        private int ucPreset;
        private boolean qualityToggle;
        private boolean sm;
        private boolean sm_dyn;
        private boolean dynamic_thresholding;
        private double controlnet_strength = 1;
        private boolean legacy;
        private boolean add_original_image = true;
        private int cfg_rescale;
        private String noise_schedule;
        private boolean legacy_v3_extend;
        private long seed;
        private String negative_prompt;

        // Img2Img specific fields
        private Double strength;
        private Double noise;
        private String image;
        private Long extra_noise_seed;

        private String[] reference_image_multiple;
        private String[] reference_information_extracted_multiple;
        private Double[] reference_strength_multiple;
    }
}