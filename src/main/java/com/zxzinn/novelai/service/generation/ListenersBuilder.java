package com.zxzinn.novelai.service.generation;

import com.zxzinn.novelai.component.PromptArea;
import javafx.scene.control.*;
import lombok.Builder;

@Builder
public class ListenersBuilder {
    private TextField apiKeyField;
    private ComboBox<String> modelComboBox;
    private TextField widthField;
    private TextField heightField;
    private ComboBox<String> samplerComboBox;
    private TextField stepsField;
    private TextField seedField;
    private ComboBox<String> generateCountComboBox;
    private PromptArea positivePromptArea;
    private PromptArea negativePromptArea;
    private TextField outputDirectoryField;
    private ComboBox<String> generationModeComboBox;
    private CheckBox smeaCheckBox;
    private CheckBox smeaDynCheckBox;
    private Slider strengthSlider;
    private TextField extraNoiseSeedField;

    public void setupListeners(GenerationSettingsManager settingsManager) {
        settingsManager.setupListeners(
                apiKeyField, modelComboBox, widthField, heightField, samplerComboBox,
                stepsField, seedField, generateCountComboBox, positivePromptArea, negativePromptArea,
                outputDirectoryField, generationModeComboBox, smeaCheckBox, smeaDynCheckBox,
                strengthSlider, extraNoiseSeedField
        );
    }
}