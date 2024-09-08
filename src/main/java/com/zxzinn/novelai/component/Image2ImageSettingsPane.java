package com.zxzinn.novelai.component;

import com.zxzinn.novelai.utils.common.PropertiesManager;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import lombok.Getter;

public class Image2ImageSettingsPane extends TitledPane {

    private final PropertiesManager propertiesManager;
    @Getter private Slider strengthSlider;
    private TextField extraNoiseSeedField;
    @Getter private Button uploadImageButton;

    public Image2ImageSettingsPane(PropertiesManager propertiesManager) {
        this.propertiesManager = propertiesManager;
        setText("Image2Image Settings");
        getStyleClass().add("settings-section");
        initializeComponents();
        loadSettings();
        setupListeners();
    }

    private void initializeComponents() {
        VBox content = new VBox(5);
        content.getStyleClass().add("settings-content");

        strengthSlider = new Slider(0, 1, 0.5);
        strengthSlider.setBlockIncrement(0.1);

        extraNoiseSeedField = new TextField();
        extraNoiseSeedField.getStyleClass().add("right-aligned-text");

        uploadImageButton = new Button("選擇圖片");
        uploadImageButton.setMaxWidth(Double.MAX_VALUE);
        uploadImageButton.getStyleClass().add("upload-button");

        content.getChildren().addAll(
                new Label("Strength"),
                strengthSlider,
                new Label("Extra Noise Seed"),
                extraNoiseSeedField,
                uploadImageButton
        );

        setContent(content);
    }

    private void loadSettings() {
        strengthSlider.setValue(propertiesManager.getDouble("strength", 0.5));
        extraNoiseSeedField.setText(String.valueOf(propertiesManager.getLong("extraNoiseSeed", 0)));
    }

    private void setupListeners() {
        strengthSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double roundedValue = Math.round(newVal.doubleValue() * 10.0) / 10.0;
            strengthSlider.setValue(roundedValue);
            propertiesManager.setDouble("strength", roundedValue);
        });
        extraNoiseSeedField.textProperty().addListener((obs, oldVal, newVal) ->
                propertiesManager.setLong("extraNoiseSeed", Long.parseLong(newVal)));
    }

    public double getStrength() {
        return strengthSlider.getValue();
    }

    public long getExtraNoiseSeed() {
        return Long.parseLong(extraNoiseSeedField.getText());
    }
}