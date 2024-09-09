package com.zxzinn.novelai.viewmodel;

import com.zxzinn.novelai.api.GenerationPayload;
import com.zxzinn.novelai.component.*;
import com.zxzinn.novelai.controller.generation.GenerationController;
import com.zxzinn.novelai.model.GenerationResult;
import com.zxzinn.novelai.model.GenerationTask;
import com.zxzinn.novelai.model.UIComponentsData;
import com.zxzinn.novelai.service.generation.GenerationTaskManager;
import com.zxzinn.novelai.service.generation.PromptManager;
import com.zxzinn.novelai.utils.common.PropertiesManager;
import com.zxzinn.novelai.utils.embed.EmbedProcessor;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.ComboBox;
import lombok.Getter;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class GenerationViewModel {

    private final PropertiesManager propertiesManager;
    private final PromptManager promptManager;
    private final GenerationTaskManager taskManager;

    @Getter private final BooleanProperty generatingProperty = new SimpleBooleanProperty(false);
    @Getter private final BooleanProperty stoppingProperty = new SimpleBooleanProperty(false);
    private final AtomicBoolean stopRequested = new AtomicBoolean(false);
    private final AtomicBoolean isInfiniteMode = new AtomicBoolean(false);
    private AtomicInteger remainingGenerations;

    public GenerationViewModel() {
        this.propertiesManager = PropertiesManager.getInstance();
        this.promptManager = new PromptManager(new EmbedProcessor());
        this.taskManager = GenerationTaskManager.getInstance();
    }

    public void loadSettings(PromptArea positivePromptArea, PromptArea negativePromptArea, ComboBox<String> generateCountComboBox) {
        positivePromptArea.setPromptText(propertiesManager.getString("positivePrompt", ""));
        negativePromptArea.setPromptText(propertiesManager.getString("negativePrompt", ""));
        generateCountComboBox.setValue(propertiesManager.getString("generateCount", "1"));
    }

    public void setupListeners(ComboBox<String> generateCountComboBox, PromptArea positivePromptArea, PromptArea negativePromptArea,
                               PromptPreviewArea positivePromptPreviewArea, PromptPreviewArea negativePromptPreviewArea) {
        generateCountComboBox.valueProperty().addListener((obs, oldVal, newVal) ->
                propertiesManager.setString("generateCount", newVal));
        positivePromptArea.getPromptTextArea().textProperty().addListener((obs, oldVal, newVal) -> {
            propertiesManager.setString("positivePrompt", newVal);
            promptManager.updatePromptPreview(newVal, positivePromptPreviewArea, true);
        });
        negativePromptArea.getPromptTextArea().textProperty().addListener((obs, oldVal, newVal) -> {
            propertiesManager.setString("negativePrompt", newVal);
            promptManager.updatePromptPreview(newVal, negativePromptPreviewArea, false);
        });
    }

    public void setupPromptControls(PromptControls positivePromptControls, PromptControls negativePromptControls,
                                    PromptArea positivePromptArea, PromptArea negativePromptArea,
                                    PromptPreviewArea positivePromptPreviewArea, PromptPreviewArea negativePromptPreviewArea) {
        promptManager.setupPromptControls(positivePromptControls, negativePromptControls,
                positivePromptArea, negativePromptArea,
                positivePromptPreviewArea, negativePromptPreviewArea);
    }

    public void updatePromptPreviews(PromptArea positivePromptArea, PromptPreviewArea positivePromptPreviewArea,
                                     PromptArea negativePromptArea, PromptPreviewArea negativePromptPreviewArea) {
        if (!promptManager.isPositivePromptLocked()) {
            promptManager.refreshPromptPreview(positivePromptArea, positivePromptPreviewArea, true);
        }
        if (!promptManager.isNegativePromptLocked()) {
            promptManager.refreshPromptPreview(negativePromptArea, negativePromptPreviewArea, false);
        }
    }

    public void startGeneration() {
        generatingProperty.set(true);
        stopRequested.set(false);
        stoppingProperty.set(false);

        int maxCount = getMaxCount();
        isInfiniteMode.set(maxCount == Integer.MAX_VALUE);
        remainingGenerations = new AtomicInteger(isInfiniteMode.get() ? Integer.MAX_VALUE : maxCount);
    }

    public boolean shouldStopGeneration() {
        return stopRequested.get() || (!isInfiniteMode.get() && remainingGenerations.get() <= 0);
    }

    public void stopGeneration() {
        stopRequested.set(true);
        stoppingProperty.set(true);
    }

    public void finishGeneration() {
        generatingProperty.set(false);
        stoppingProperty.set(false);
    }

    public GenerationTask createGenerationTask(UIComponentsData uiData) {
        GenerationPayload payload = createGenerationPayload(uiData);
        return new GenerationTask(payload, uiData.apiKey);
    }

    private GenerationPayload createGenerationPayload(UIComponentsData uiData) {
        GenerationPayload payload = new GenerationPayload();
        GenerationPayload.GenerationParameters params = new GenerationPayload.GenerationParameters();
        payload.setParameters(params);

        payload.setInput(uiData.positivePromptPreviewText);
        payload.setModel(uiData.model);

        if ("Text2Image".equals(uiData.generationMode)) {
            params.setSm(uiData.smea);
            params.setSm_dyn(uiData.smeaDyn);
            payload.setAction("generate");
        } else {
            payload.setAction("img2img");
            params.setStrength(uiData.strength);
            params.setNoise(uiData.noise);
            params.setImage(uiData.base64Image);
            params.setExtra_noise_seed(uiData.extraNoiseSeed);
        }

        params.setParams_version(1);
        params.setWidth(uiData.outputWidth);
        params.setHeight(uiData.outputHeight);
        params.setScale(uiData.ratio);
        params.setSampler(uiData.sampler);
        params.setSteps(uiData.steps);
        params.setN_samples(uiData.count);
        params.setSeed(uiData.seed);

        params.setNegative_prompt(uiData.negativePromptPreviewText);
        return payload;
    }

    public CompletableFuture<GenerationResult> submitTask(GenerationTask task) {
        return taskManager.submitTask(task);
    }

    public void decrementRemainingGenerations() {
        if (!isInfiniteMode.get()) {
            remainingGenerations.decrementAndGet();
        }
    }

    private int getMaxCount() {
        String selectedCount = propertiesManager.getString("generateCount", "1");
        return "無限".equals(selectedCount) ? Integer.MAX_VALUE : Integer.parseInt(selectedCount);
    }

    public boolean isGenerating() {
        return generatingProperty.get();
    }

    public boolean isStopping() {
        return stoppingProperty.get();
    }
}