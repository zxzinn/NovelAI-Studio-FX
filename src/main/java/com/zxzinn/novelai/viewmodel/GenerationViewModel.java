package com.zxzinn.novelai.viewmodel;

import com.zxzinn.novelai.api.GenerationPayload;
import com.zxzinn.novelai.component.*;
import com.zxzinn.novelai.model.GenerationResult;
import com.zxzinn.novelai.model.GenerationTask;
import com.zxzinn.novelai.model.UIComponentsData;
import com.zxzinn.novelai.service.generation.GenerationTaskManager;
import com.zxzinn.novelai.utils.common.PropertiesManager;
import com.zxzinn.novelai.utils.embed.EmbedProcessor;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.ComboBox;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class GenerationViewModel {

    private final PropertiesManager propertiesManager;
    private final EmbedProcessor embedProcessor;
    private final GenerationTaskManager taskManager;

    @Getter private final BooleanProperty generatingProperty = new SimpleBooleanProperty(false);
    @Getter private final BooleanProperty stoppingProperty = new SimpleBooleanProperty(false);
    private final AtomicBoolean stopRequested = new AtomicBoolean(false);
    private final AtomicBoolean isInfiniteMode = new AtomicBoolean(false);
    private AtomicInteger remainingGenerations;

    private final AtomicBoolean isPositivePromptLocked = new AtomicBoolean(false);
    private final AtomicBoolean isNegativePromptLocked = new AtomicBoolean(false);

    public GenerationViewModel() {
        this.propertiesManager = PropertiesManager.getInstance();
        this.embedProcessor = new EmbedProcessor();
        this.taskManager = GenerationTaskManager.getInstance();
    }

    public void loadSettings(PromptComponent positivePromptComponent, PromptComponent negativePromptComponent, ComboBox<String> generateCountComboBox) {
        positivePromptComponent.setPromptText(propertiesManager.getString("positivePrompt", ""));
        negativePromptComponent.setPromptText(propertiesManager.getString("negativePrompt", ""));
        generateCountComboBox.setValue(propertiesManager.getString("generateCount", "1"));
    }

    public void setupListeners(ComboBox<String> generateCountComboBox, PromptComponent positivePromptComponent, PromptComponent negativePromptComponent) {
        generateCountComboBox.valueProperty().addListener((obs, oldVal, newVal) ->
                propertiesManager.setString("generateCount", newVal));
        positivePromptComponent.getPromptTextArea().textProperty().addListener((obs, oldVal, newVal) -> {
            propertiesManager.setString("positivePrompt", newVal);
            updatePromptPreview(newVal, positivePromptComponent, true);
        });
        negativePromptComponent.getPromptTextArea().textProperty().addListener((obs, oldVal, newVal) -> {
            propertiesManager.setString("negativePrompt", newVal);
            updatePromptPreview(newVal, negativePromptComponent, false);
        });
    }

    public void setupPromptControls(PromptComponent positivePromptComponent, PromptComponent negativePromptComponent) {
        setupPromptControl(positivePromptComponent, isPositivePromptLocked, true);
        setupPromptControl(negativePromptComponent, isNegativePromptLocked, false);
    }

    private void setupPromptControl(PromptComponent promptComponent, AtomicBoolean isLocked, boolean isPositive) {
        promptComponent.setOnRefreshAction(() -> forceRefreshPromptPreview(promptComponent));
        promptComponent.setOnLockAction(() -> {
            isLocked.set(!isLocked.get());
            promptComponent.setLockState(isLocked.get());
        });
    }

    public void refreshPromptPreview(PromptComponent promptComponent, boolean isPositive) {
        if (isPromptLocked(isPositive)) {
            String processedPrompt = embedProcessor.processPrompt(promptComponent.getPromptText());
            promptComponent.setPreviewText(processedPrompt);
        }
    }

    public void forceRefreshPromptPreview(@NotNull PromptComponent promptComponent) {
        String processedPrompt = embedProcessor.processPrompt(promptComponent.getPromptText());
        promptComponent.setPreviewText(processedPrompt);
    }

    public void updatePromptPreview(String newValue, PromptComponent promptComponent, boolean isPositive) {
        if (isPromptLocked(isPositive)) {
            String processedPrompt = embedProcessor.processPrompt(newValue);
            promptComponent.setPreviewText(processedPrompt);
        }
    }

    public boolean isPromptLocked(boolean isPositive) {
        return isPositive ? !isPositivePromptLocked.get() : !isNegativePromptLocked.get();
    }

    public void updatePromptPreviews(PromptComponent positivePromptComponent, PromptComponent negativePromptComponent) {
        if (!isPositivePromptLocked.get()) {
            refreshPromptPreview(positivePromptComponent, true);
        }
        if (!isNegativePromptLocked.get()) {
            refreshPromptPreview(negativePromptComponent, false);
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