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
        setupPromptControl(positivePromptControls, positivePromptArea, positivePromptPreviewArea, isPositivePromptLocked, true);
        setupPromptControl(negativePromptControls, negativePromptArea, negativePromptPreviewArea, isNegativePromptLocked, false);
    }

    private void setupPromptControl(PromptControls controls, PromptArea promptArea, PromptPreviewArea previewArea, AtomicBoolean isLocked, boolean isPositive) {
        controls.setOnRefreshAction(() -> {
            if (!isLocked.get()) {
                refreshPromptPreview(promptArea, previewArea, isPositive);
            }
        });

        controls.setOnLockAction(() -> {
            isLocked.set(!isLocked.get());
            controls.setLockState(isLocked.get());
        });
    }

    public void refreshPromptPreview(PromptArea promptArea, PromptPreviewArea previewArea, boolean isPositive) {
        if (!isPromptLocked(isPositive)) {
            String processedPrompt = embedProcessor.processPrompt(promptArea.getPromptText());
            previewArea.setPreviewText(processedPrompt);
        }
    }

    public void updatePromptPreview(String newValue, PromptPreviewArea previewArea, boolean isPositive) {
        if (!isPromptLocked(isPositive)) {
            String processedPrompt = embedProcessor.processPrompt(newValue);
            previewArea.setPreviewText(processedPrompt);
        }
    }

    public boolean isPromptLocked(boolean isPositive) {
        return isPositive ? isPositivePromptLocked.get() : isNegativePromptLocked.get();
    }

    public boolean isPositivePromptLocked() {
        return isPositivePromptLocked.get();
    }

    public boolean isNegativePromptLocked() {
        return isNegativePromptLocked.get();
    }
}