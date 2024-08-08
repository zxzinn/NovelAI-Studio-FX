package com.zxzinn.novelai.api;

import java.io.IOException;

public interface APIClient {
    byte[] generateImage(GenerationPayload payload, String apiKey) throws IOException;
}