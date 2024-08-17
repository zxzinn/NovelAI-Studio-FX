package com.zxzinn.novelai.service.generation;

import com.zxzinn.novelai.component.PromptArea;
import com.zxzinn.novelai.utils.common.SettingsManager;
import javafx.scene.control.*;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GenerationSettingsManager {
    private final SettingsManager settingsManager;

    public void loadSettings(TextField apiKeyField, ComboBox<String> modelComboBox, TextField widthField,
                             TextField heightField, ComboBox<String> samplerComboBox, TextField stepsField,
                             TextField seedField, ComboBox<String> generateCountComboBox, PromptArea positivePromptArea,
                             PromptArea negativePromptArea, TextField outputDirectoryField,
                             ComboBox<String> generationModeComboBox, CheckBox smeaCheckBox, CheckBox smeaDynCheckBox,
                             Slider strengthSlider, TextField extraNoiseSeedField) {
        apiKeyField.setText(settingsManager.getString("apiKey", ""));
        modelComboBox.setValue(settingsManager.getString("model", "nai-diffusion-3"));
        widthField.setText(String.valueOf(settingsManager.getInt("width", 832)));
        heightField.setText(String.valueOf(settingsManager.getInt("height", 1216)));
        samplerComboBox.setValue(settingsManager.getString("sampler", "k_euler"));
        stepsField.setText(String.valueOf(settingsManager.getInt("steps", 28)));
        seedField.setText(String.valueOf(settingsManager.getInt("seed", 0)));
        generateCountComboBox.setValue(settingsManager.getString("generateCount", "1"));
        positivePromptArea.setPromptText(settingsManager.getString("positivePrompt", ""));
        negativePromptArea.setPromptText(settingsManager.getString("negativePrompt", ""));
        outputDirectoryField.setText(settingsManager.getString("outputDirectory", "output"));
        generationModeComboBox.setValue(settingsManager.getString("generationMode", "Text2Image"));
        smeaCheckBox.setSelected(settingsManager.getBoolean("smea", true));
        smeaDynCheckBox.setSelected(settingsManager.getBoolean("smeaDyn", false));
        strengthSlider.setValue(settingsManager.getDouble("strength", 0.5));
        extraNoiseSeedField.setText(String.valueOf(settingsManager.getLong("extraNoiseSeed", 0)));
    }

    public void setupListeners(TextField apiKeyField, ComboBox<String> modelComboBox, TextField widthField,
                               TextField heightField, ComboBox<String> samplerComboBox, TextField stepsField,
                               TextField seedField, ComboBox<String> generateCountComboBox, PromptArea positivePromptArea,
                               PromptArea negativePromptArea, TextField outputDirectoryField,
                               ComboBox<String> generationModeComboBox, CheckBox smeaCheckBox, CheckBox smeaDynCheckBox,
                               Slider strengthSlider, TextField extraNoiseSeedField) {
        setupTextFieldListener(apiKeyField, "apiKey", settingsManager::setString);
        setupComboBoxListener(modelComboBox, "model", settingsManager::setString);
        setupTextFieldListener(widthField, "width", (key, value) -> settingsManager.setInt(key, Integer.parseInt(value)));
        setupTextFieldListener(heightField, "height", (key, value) -> settingsManager.setInt(key, Integer.parseInt(value)));
        setupComboBoxListener(samplerComboBox, "sampler", settingsManager::setString);
        setupTextFieldListener(stepsField, "steps", (key, value) -> settingsManager.setInt(key, Integer.parseInt(value)));
        setupTextFieldListener(seedField, "seed", (key, value) -> settingsManager.setInt(key, Integer.parseInt(value)));
        setupComboBoxListener(generateCountComboBox, "generateCount", settingsManager::setString);
        setupPromptAreaListener(positivePromptArea, "positivePrompt", settingsManager::setString);
        setupPromptAreaListener(negativePromptArea, "negativePrompt", settingsManager::setString);
        setupTextFieldListener(outputDirectoryField, "outputDirectory", settingsManager::setString);
        setupComboBoxListener(generationModeComboBox, "generationMode", settingsManager::setString);
        setupCheckBoxListener(smeaCheckBox, "smea", settingsManager::setBoolean);
        setupCheckBoxListener(smeaDynCheckBox, "smeaDyn", settingsManager::setBoolean);
        setupSliderListener(strengthSlider, "strength", settingsManager::setDouble);
        setupTextFieldListener(extraNoiseSeedField, "extraNoiseSeed", (key, value) -> settingsManager.setLong(key, Long.parseLong(value)));
    }

    private void setupTextFieldListener(TextField textField, String key, java.util.function.BiConsumer<String, String> setter) {
        textField.textProperty().addListener((obs, oldVal, newVal) -> setter.accept(key, newVal));
    }

    private void setupComboBoxListener(ComboBox<String> comboBox, String key, java.util.function.BiConsumer<String, String> setter) {
        comboBox.valueProperty().addListener((obs, oldVal, newVal) -> setter.accept(key, newVal));
    }

    private void setupPromptAreaListener(PromptArea promptArea, String key, java.util.function.BiConsumer<String, String> setter) {
        promptArea.getPromptTextArea().textProperty().addListener((obs, oldVal, newVal) -> setter.accept(key, newVal));
    }

    private void setupCheckBoxListener(CheckBox checkBox, String key, java.util.function.BiConsumer<String, Boolean> setter) {
        checkBox.selectedProperty().addListener((obs, oldVal, newVal) -> setter.accept(key, newVal));
    }

    private void setupSliderListener(Slider slider, String key, java.util.function.BiConsumer<String, Double> setter) {
        slider.valueProperty().addListener((obs, oldVal, newVal) -> setter.accept(key, newVal.doubleValue()));
    }
}