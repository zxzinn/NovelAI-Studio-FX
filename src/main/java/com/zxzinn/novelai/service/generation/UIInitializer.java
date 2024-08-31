package com.zxzinn.novelai.service.generation;

import com.zxzinn.novelai.component.PromptArea;
import com.zxzinn.novelai.component.PromptPreviewArea;
import com.zxzinn.novelai.utils.common.NAIConstants;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Slider;
import lombok.Builder;
import org.jetbrains.annotations.NotNull;

@Builder
public class UIInitializer {
    private ComboBox<String> modelComboBox;
    private ComboBox<String> samplerComboBox;
    private ComboBox<String> generateCountComboBox;
    private PromptArea positivePromptArea;
    private PromptArea negativePromptArea;
    private PromptPreviewArea positivePromptPreviewArea;
    private PromptPreviewArea negativePromptPreviewArea;
    private Slider strengthSlider;

    public void initializeFields() {
        initializeModelComboBox();
        initializeSamplerComboBox();
        initializeGenerateCountComboBox();
        initializePromptAreas();
        initializePromptPreviewAreas();
        setupStrengthSlider();
    }

    private void initializeModelComboBox() {
        modelComboBox.setItems(FXCollections.observableArrayList(NAIConstants.MODELS));
        modelComboBox.setValue(NAIConstants.MODELS[0]);
    }

    private void initializeSamplerComboBox() {
        samplerComboBox.setItems(FXCollections.observableArrayList(NAIConstants.SAMPLERS));
        samplerComboBox.setValue(NAIConstants.SAMPLERS[0]);
    }

    private void initializeGenerateCountComboBox() {
        generateCountComboBox.getItems().addAll("1", "2", "3", "4", "無限");
        generateCountComboBox.setValue("1");
    }

    private void initializePromptAreas() {
        positivePromptArea.setPromptLabel("正面提示詞:");
        negativePromptArea.setPromptLabel("負面提示詞:");
    }

    private void initializePromptPreviewAreas() {
        positivePromptPreviewArea.setPreviewLabel("正面提示詞預覽");
        negativePromptPreviewArea.setPreviewLabel("負面提示詞預覽");
    }

    private void setupStrengthSlider() {
        strengthSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            double roundedValue = Math.round(newValue.doubleValue() * 10.0) / 10.0;
            strengthSlider.setValue(roundedValue);
        });
    }

    public static void setupVerticalLayout(@NotNull ComboBox<?>... comboBoxes) {
        for (ComboBox<?> comboBox : comboBoxes) {
            comboBox.setMaxWidth(Double.MAX_VALUE);
        }
    }
}