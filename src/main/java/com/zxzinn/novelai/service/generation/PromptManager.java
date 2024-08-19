package com.zxzinn.novelai.service.generation;

import com.zxzinn.novelai.component.PromptArea;
import com.zxzinn.novelai.component.PromptControls;
import com.zxzinn.novelai.component.PromptPreviewArea;
import com.zxzinn.novelai.utils.embed.EmbedProcessor;

public class PromptManager {
    private final EmbedProcessor embedProcessor;

    public PromptManager(EmbedProcessor embedProcessor) {
        this.embedProcessor = embedProcessor;
    }

    public void setupPromptControls(PromptControls positivePromptControls, PromptControls negativePromptControls,
                                    PromptArea positivePromptArea, PromptArea negativePromptArea,
                                    PromptPreviewArea positivePromptPreviewArea, PromptPreviewArea negativePromptPreviewArea) {
        positivePromptControls.setOnRefreshAction(() -> refreshPromptPreview(positivePromptArea, positivePromptPreviewArea));
        negativePromptControls.setOnRefreshAction(() -> refreshPromptPreview(negativePromptArea, negativePromptPreviewArea));
    }

    public void refreshPromptPreview(PromptArea promptArea, PromptPreviewArea previewArea) {
        String processedPrompt = embedProcessor.processPrompt(promptArea.getPromptText());
        previewArea.setPreviewText(processedPrompt);
    }

    public void updatePromptPreview(String newValue, PromptPreviewArea previewArea) {
        String processedPrompt = embedProcessor.processPrompt(newValue);
        previewArea.setPreviewText(processedPrompt);
    }
}
