package com.zxzinn.novelai.service.generation;

import com.zxzinn.novelai.component.PromptArea;
import com.zxzinn.novelai.utils.common.PropertiesManager;
import javafx.scene.control.*;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;

@RequiredArgsConstructor
public class GenerationSettingsManager {
    private final PropertiesManager propertiesManager;

    public void loadSettings(@NotNull TextField apiKeyField, @NotNull ComboBox<String> modelComboBox, @NotNull TextField widthField,
                             @NotNull TextField heightField, @NotNull ComboBox<String> samplerComboBox, @NotNull TextField stepsField,
                             @NotNull TextField seedField, @NotNull ComboBox<String> generateCountComboBox, @NotNull PromptArea positivePromptArea,
                             @NotNull PromptArea negativePromptArea, @NotNull TextField outputDirectoryField,
                             @NotNull ComboBox<String> generationModeComboBox, @NotNull CheckBox smeaCheckBox, @NotNull CheckBox smeaDynCheckBox,
                             @NotNull Slider strengthSlider, @NotNull TextField extraNoiseSeedField, @NotNull TextField ratioField, @NotNull TextField countField) {
        apiKeyField.setText(propertiesManager.getString("apiKey", ""));
        modelComboBox.setValue(propertiesManager.getString("model", "nai-diffusion-3"));
        widthField.setText(String.valueOf(propertiesManager.getInt("width", 832)));
        heightField.setText(String.valueOf(propertiesManager.getInt("height", 1216)));
        samplerComboBox.setValue(propertiesManager.getString("sampler", "k_euler"));
        stepsField.setText(String.valueOf(propertiesManager.getInt("steps", 28)));
        seedField.setText(String.valueOf(propertiesManager.getInt("seed", 0)));
        generateCountComboBox.setValue(propertiesManager.getString("generateCount", "1"));
        positivePromptArea.setPromptText(propertiesManager.getString("positivePrompt", ""));
        negativePromptArea.setPromptText(propertiesManager.getString("negativePrompt", ""));
        outputDirectoryField.setText(propertiesManager.getString("outputDirectory", "output"));
        generationModeComboBox.setValue(propertiesManager.getString("generationMode", "Text2Image"));
        smeaCheckBox.setSelected(propertiesManager.getBoolean("smea", true));
        smeaDynCheckBox.setSelected(propertiesManager.getBoolean("smeaDyn", false));
        strengthSlider.setValue(propertiesManager.getDouble("strength", 0.5));
        extraNoiseSeedField.setText(String.valueOf(propertiesManager.getLong("extraNoiseSeed", 0)));
        ratioField.setText(String.valueOf(propertiesManager.getInt("ratio", 7))); // 新增
        countField.setText(String.valueOf(propertiesManager.getInt("count", 1))); // 新增
    }

    public void setupListeners(TextField apiKeyField, ComboBox<String> modelComboBox, TextField widthField,
                               TextField heightField, ComboBox<String> samplerComboBox, TextField stepsField,
                               TextField seedField, ComboBox<String> generateCountComboBox, PromptArea positivePromptArea,
                               PromptArea negativePromptArea, TextField outputDirectoryField,
                               ComboBox<String> generationModeComboBox, CheckBox smeaCheckBox, CheckBox smeaDynCheckBox,
                               Slider strengthSlider, TextField extraNoiseSeedField, TextField ratioField, TextField countField) {
        setupTextFieldListener(apiKeyField, "apiKey", propertiesManager::setString);
        setupComboBoxListener(modelComboBox, "model", propertiesManager::setString);
        setupTextFieldListener(widthField, "width", (key, value) -> propertiesManager.setInt(key, Integer.parseInt(value)));
        setupTextFieldListener(heightField, "height", (key, value) -> propertiesManager.setInt(key, Integer.parseInt(value)));
        setupComboBoxListener(samplerComboBox, "sampler", propertiesManager::setString);
        setupTextFieldListener(stepsField, "steps", (key, value) -> propertiesManager.setInt(key, Integer.parseInt(value)));
        setupTextFieldListener(seedField, "seed", (key, value) -> propertiesManager.setInt(key, Integer.parseInt(value)));
        setupComboBoxListener(generateCountComboBox, "generateCount", propertiesManager::setString);
        setupPromptAreaListener(positivePromptArea, "positivePrompt", propertiesManager::setString);
        setupPromptAreaListener(negativePromptArea, "negativePrompt", propertiesManager::setString);
        setupTextFieldListener(outputDirectoryField, "outputDirectory", propertiesManager::setString);
        setupComboBoxListener(generationModeComboBox, "generationMode", propertiesManager::setString);
        setupCheckBoxListener(smeaCheckBox, "smea", propertiesManager::setBoolean);
        setupCheckBoxListener(smeaDynCheckBox, "smeaDyn", propertiesManager::setBoolean);
        setupSliderListener(strengthSlider, propertiesManager::setDouble);
        setupTextFieldListener(extraNoiseSeedField, "extraNoiseSeed", (key, value) -> propertiesManager.setLong(key, Long.parseLong(value)));
        setupTextFieldListener(ratioField, "ratio", (key, value) -> propertiesManager.setInt(key, Integer.parseInt(value))); // 新增
        setupTextFieldListener(countField, "count", (key, value) -> propertiesManager.setInt(key, Integer.parseInt(value))); // 新增
    }

    private void setupTextFieldListener(@NotNull TextField textField, String key, java.util.function.BiConsumer<String, String> setter) {
        textField.textProperty().addListener((obs, oldVal, newVal) -> setter.accept(key, newVal));
    }

    private void setupComboBoxListener(@NotNull ComboBox<String> comboBox, String key, java.util.function.BiConsumer<String, String> setter) {
        comboBox.valueProperty().addListener((obs, oldVal, newVal) -> setter.accept(key, newVal));
    }

    private void setupPromptAreaListener(@NotNull PromptArea promptArea, String key, java.util.function.BiConsumer<String, String> setter) {
        promptArea.getPromptTextArea().textProperty().addListener((obs, oldVal, newVal) -> setter.accept(key, newVal));
    }

    private void setupCheckBoxListener(@NotNull CheckBox checkBox, String key, java.util.function.BiConsumer<String, Boolean> setter) {
        checkBox.selectedProperty().addListener((obs, oldVal, newVal) -> setter.accept(key, newVal));
    }

    private void setupSliderListener(@NotNull Slider slider, BiConsumer<String, Double> setter) {
        slider.valueProperty().addListener((obs, oldVal, newVal) -> setter.accept("strength", newVal.doubleValue()));
    }
}