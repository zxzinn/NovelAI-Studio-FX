package com.zxzinn.novelai;

public class NAIConstants {
    public static final String[] MODELS = {"nai-diffusion", "safe-diffusion", "nai-diffusion-furry", "custom",
            "nai-diffusion-inpainting", "nai-diffusion-3-inpainting",
            "safe-diffusion-inpainting", "furry-diffusion-inpainting",
            "kandinsky-vanilla", "nai-diffusion-2", "nai-diffusion-3"};
    public static final String[] ACTIONS = {"generate", "img2img", "infill"};
    public static final String[] SAMPLERS = {"k_euler", "k_euler_ancestral", "k_heun", "k_dpm_2", "k_dpm_2_ancestral", "k_lms", "plms", "ddim"};

}
