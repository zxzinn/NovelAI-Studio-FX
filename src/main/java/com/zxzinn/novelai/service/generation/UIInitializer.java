package com.zxzinn.novelai.service.generation;

import com.zxzinn.novelai.component.PromptArea;
import com.zxzinn.novelai.component.PromptPreviewArea;
import com.zxzinn.novelai.utils.common.NAIConstants;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Slider;
import org.jetbrains.annotations.NotNull;

public class UIInitializer {
    public static void initializeFields(@NotNull ComboBox<String> modelComboBox, @NotNull ComboBox<String> samplerComboBox,
                                        @NotNull ComboBox<String> generateCountComboBox, @NotNull PromptArea positivePromptArea,
                                        @NotNull PromptArea negativePromptArea, @NotNull PromptPreviewArea positivePromptPreviewArea,
                                        @NotNull PromptPreviewArea negativePromptPreviewArea, Slider strengthSlider) {
        modelComboBox.setItems(FXCollections.observableArrayList(NAIConstants.MODELS));
        modelComboBox.setValue(NAIConstants.MODELS[0]);
        samplerComboBox.setItems(FXCollections.observableArrayList(NAIConstants.SAMPLERS));
        samplerComboBox.setValue(NAIConstants.SAMPLERS[0]);
        generateCountComboBox.getItems().addAll("1", "2", "3", "4", "無限");
        generateCountComboBox.setValue("1");
        positivePromptArea.setPromptLabel("正面提示詞:");
        negativePromptArea.setPromptLabel("負面提示詞:");
        positivePromptPreviewArea.setPreviewLabel("正面提示詞預覽");
        negativePromptPreviewArea.setPreviewLabel("負面提示詞預覽");
        setupStrengthSlider(strengthSlider);
    }

    private static void setupStrengthSlider(@NotNull Slider strengthSlider) {
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
