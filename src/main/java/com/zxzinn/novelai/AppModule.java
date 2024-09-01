package com.zxzinn.novelai;

import com.google.gson.Gson;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.zxzinn.novelai.api.APIClient;
import com.zxzinn.novelai.service.filemanager.FileManagerService;
import com.zxzinn.novelai.service.generation.ImageGenerationService;
import com.zxzinn.novelai.service.ui.WindowService;
import com.zxzinn.novelai.utils.common.PropertiesManager;
import com.zxzinn.novelai.utils.embed.EmbedProcessor;
import com.zxzinn.novelai.utils.image.ImageUtils;

import java.io.IOException;

public class AppModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(EmbedProcessor.class).in(Singleton.class);
        bind(ImageUtils.class).in(Singleton.class);
        bind(PropertiesManager.class).toInstance(PropertiesManager.getInstance());
    }

    @Provides
    @Singleton
    APIClient provideAPIClient(Gson gson) {
        return new APIClient(gson);
    }

    @Provides
    @Singleton
    Gson provideGson() {
        return new Gson();
    }

    @Provides
    @Singleton
    ImageGenerationService provideImageGenerationService(APIClient apiClient) {
        return new ImageGenerationService(apiClient);
    }

    @Provides
    @Singleton
    FileManagerService provideFileManagerService(PropertiesManager propertiesManager) throws IOException {
        return new FileManagerService(propertiesManager);
    }

    @Provides
    @Singleton
    WindowService provideWindowService(PropertiesManager propertiesManager) {
        return new WindowService(propertiesManager);
    }
}