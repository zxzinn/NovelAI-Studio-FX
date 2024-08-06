package com.zxzinn.novelai.api;

import java.io.IOException;

public interface APIClient {
    byte[] generateImage(ImageGenerationPayload payload, String apiKey) throws IOException;
    byte[] generateImg2Img(Img2ImgGenerationPayload payload, String apiKey) throws IOException;
}