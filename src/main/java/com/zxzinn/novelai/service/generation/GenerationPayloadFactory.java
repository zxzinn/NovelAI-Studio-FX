package com.zxzinn.novelai.service.generation;

import com.zxzinn.novelai.api.GenerationPayload;
import com.zxzinn.novelai.api.ImageGenerationPayload;
import com.zxzinn.novelai.api.Img2ImgGenerationPayload;
import org.jetbrains.annotations.NotNull;

public class GenerationPayloadFactory {
    public static GenerationPayload createPayload(String generationMode, String processedPositivePrompt,
                                                  String processedNegativePrompt, String model, int width, int height,
                                                  int scale, String sampler, int steps, int nSamples, long seed,
                                                  boolean smea, boolean smeaDyn, String base64Image, double strength,
                                                  long extraNoiseSeed) {
        if ("Text2Image".equals(generationMode)) {
            return createText2ImagePayload(processedPositivePrompt, processedNegativePrompt, model, width, height,
                    scale, sampler, steps, nSamples, seed, smea, smeaDyn);
        } else {
            return createImage2ImagePayload(processedPositivePrompt, processedNegativePrompt, model, width, height,
                    scale, sampler, steps, nSamples, seed, base64Image, strength, extraNoiseSeed);
        }
    }

    @NotNull
    private static ImageGenerationPayload createText2ImagePayload(String processedPositivePrompt, String processedNegativePrompt,
                                                                  String model, int width, int height, int scale, String sampler,
                                                                  int steps, int nSamples, long seed, boolean smea, boolean smeaDyn) {
        ImageGenerationPayload payload = new ImageGenerationPayload();
        payload.setInput(processedPositivePrompt);
        payload.setModel(model);
        payload.setAction("generate");

        GenerationPayload.GenerationParameters parameters = new GenerationPayload.GenerationParameters();
        parameters.setWidth(width);
        parameters.setHeight(height);
        parameters.setScale(scale);
        parameters.setSampler(sampler);
        parameters.setSteps(steps);
        parameters.setN_samples(nSamples);
        parameters.setUcPreset(false);
        parameters.setQualityToggle(false);
        parameters.setSm(smea);
        parameters.setSm_dyn(smeaDyn);
        parameters.setSeed(seed);
        parameters.setNegative_prompt(processedNegativePrompt);

        payload.setParameters(parameters);
        return payload;
    }

    @NotNull
    private static Img2ImgGenerationPayload createImage2ImagePayload(String processedPositivePrompt, String processedNegativePrompt,
                                                                     String model, int width, int height, int scale, String sampler,
                                                                     int steps, int nSamples, long seed, String base64Image,
                                                                     double strength, long extraNoiseSeed) {
        Img2ImgGenerationPayload payload = new Img2ImgGenerationPayload();
        payload.setInput(processedPositivePrompt);
        payload.setModel(model);
        payload.setAction("img2img");

        Img2ImgGenerationPayload.Img2ImgGenerationParameters parameters = new Img2ImgGenerationPayload.Img2ImgGenerationParameters();
        parameters.setWidth(width);
        parameters.setHeight(height);
        parameters.setScale(scale);
        parameters.setSampler(sampler);
        parameters.setSteps(steps);
        parameters.setN_samples(nSamples);
        parameters.setUcPreset(false);
        parameters.setQualityToggle(false);
        parameters.setSeed(seed);
        parameters.setNegative_prompt(processedNegativePrompt);
        parameters.setImage(base64Image);
        parameters.setExtra_noise_seed(extraNoiseSeed);

        parameters.setStrength(strength);
        parameters.setNoise(0);
        parameters.setDynamic_thresholding(false);
        parameters.setControlnet_strength(1.0);
        parameters.setLegacy(false);
        parameters.setAdd_original_image(true);
        parameters.setCfg_rescale(0);
        parameters.setNoise_schedule("native");
        parameters.setLegacy_v3_extend(false);

        payload.setParameters(parameters);
        return payload;
    }
}