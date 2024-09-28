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
        private Integer params_version;
        private Integer width;
        private Integer height;
        private Integer scale;
        private String sampler;
        private Integer steps;
        private Integer n_samples;
        private Integer ucPreset;
        private Boolean qualityToggle;
        private Boolean sm;
        private Boolean sm_dyn;
        private Boolean dynamic_thresholding;
        private Double controlnet_strength;
        private Boolean legacy;
        private Boolean add_original_image;
        private Integer cfg_rescale;
        private String noise_schedule;
        private Boolean legacy_v3_extend;
        private Long seed;
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