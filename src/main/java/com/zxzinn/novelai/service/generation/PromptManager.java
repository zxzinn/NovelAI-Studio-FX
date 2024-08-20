package com.zxzinn.novelai.service.generation;

import com.zxzinn.novelai.component.PromptArea;
import com.zxzinn.novelai.component.PromptControls;
import com.zxzinn.novelai.component.PromptPreviewArea;
import com.zxzinn.novelai.utils.embed.EmbedProcessor;

import java.util.concurrent.atomic.AtomicBoolean;

public class PromptManager {
    private final EmbedProcessor embedProcessor;
    private final AtomicBoolean isPositivePromptLocked = new AtomicBoolean(false);
    private final AtomicBoolean isNegativePromptLocked = new AtomicBoolean(false);

    public PromptManager(EmbedProcessor embedProcessor) {
        this.embedProcessor = embedProcessor;
    }

    public void setupPromptControls(PromptControls positivePromptControls, PromptControls negativePromptControls,
                                    PromptArea positivePromptArea, PromptArea negativePromptArea,
                                    PromptPreviewArea positivePromptPreviewArea, PromptPreviewArea negativePromptPreviewArea) {
        setupPromptControl(positivePromptControls, positivePromptArea, positivePromptPreviewArea, isPositivePromptLocked);
        setupPromptControl(negativePromptControls, negativePromptArea, negativePromptPreviewArea, isNegativePromptLocked);
    }

    private void setupPromptControl(PromptControls controls, PromptArea promptArea, PromptPreviewArea previewArea, AtomicBoolean isLocked) {
        controls.setOnRefreshAction(() -> {
            if (!isLocked.get()) {
                refreshPromptPreview(promptArea, previewArea);
            }
        });

        controls.setOnLockAction(() -> {
            isLocked.set(!isLocked.get());
            controls.setLockState(isLocked.get());
        });
    }

    public void refreshPromptPreview(PromptArea promptArea, PromptPreviewArea previewArea) {
        String processedPrompt = embedProcessor.processPrompt(promptArea.getPromptText());
        previewArea.setPreviewText(processedPrompt);
    }

    public void updatePromptPreview(String newValue, PromptPreviewArea previewArea) {
        String processedPrompt = embedProcessor.processPrompt(newValue);
        previewArea.setPreviewText(processedPrompt);
    }

    public boolean isPositivePromptLocked() {
        return isPositivePromptLocked.get();
    }

    public boolean isNegativePromptLocked() {
        return isNegativePromptLocked.get();
    }
}