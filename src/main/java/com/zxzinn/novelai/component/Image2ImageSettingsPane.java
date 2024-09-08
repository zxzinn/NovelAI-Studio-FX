package com.zxzinn.novelai.component;

import com.zxzinn.novelai.utils.common.PropertiesManager;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import lombok.Getter;

import java.io.File;
import java.util.Base64;
import java.nio.file.Files;

public class Image2ImageSettingsPane extends TitledPane {

    private final PropertiesManager propertiesManager;
    @Getter private Slider strengthSlider;
    @Getter private Slider noiseSlider;
    @Getter private TextField extraNoiseSeedField;
    @Getter private Button uploadImageButton;
    @Getter private Label selectedImageLabel;
    @Getter private String base64Image;

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

        noiseSlider = new Slider(0, 1, 0);
        noiseSlider.setBlockIncrement(0.1);

        extraNoiseSeedField = new TextField();
        extraNoiseSeedField.getStyleClass().add("right-aligned-text");

        uploadImageButton = new Button("選擇圖片");
        uploadImageButton.setMaxWidth(Double.MAX_VALUE);
        uploadImageButton.getStyleClass().add("upload-button");

        selectedImageLabel = new Label("未選擇圖片");

        content.getChildren().addAll(
                new Label("Strength"),
                strengthSlider,
                new Label("Noise"),
                noiseSlider,
                new Label("Extra Noise Seed"),
                extraNoiseSeedField,
                uploadImageButton,
                selectedImageLabel
        );

        setContent(content);
    }

    private void loadSettings() {
        strengthSlider.setValue(propertiesManager.getDouble("strength", 0.5));
        noiseSlider.setValue(propertiesManager.getDouble("noise", 0));
        extraNoiseSeedField.setText(String.valueOf(propertiesManager.getLong("extraNoiseSeed", 0)));
    }

    private void setupListeners() {
        strengthSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double roundedValue = Math.round(newVal.doubleValue() * 10.0) / 10.0;
            strengthSlider.setValue(roundedValue);
            propertiesManager.setDouble("strength", roundedValue);
        });

        noiseSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double roundedValue = Math.round(newVal.doubleValue() * 10.0) / 10.0;
            noiseSlider.setValue(roundedValue);
            propertiesManager.setDouble("noise", roundedValue);
        });

        extraNoiseSeedField.textProperty().addListener((obs, oldVal, newVal) ->
                propertiesManager.setLong("extraNoiseSeed", Long.parseLong(newVal)));

        uploadImageButton.setOnAction(event -> uploadImage());
    }

    private void uploadImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("選擇圖片");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );
        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            try {
                byte[] fileContent = Files.readAllBytes(selectedFile.toPath());
                base64Image = Base64.getEncoder().encodeToString(fileContent);
                selectedImageLabel.setText(selectedFile.getName());
            } catch (Exception e) {
                e.printStackTrace();
                selectedImageLabel.setText("圖片上傳失敗");
            }
        }
    }

    public double getStrength() {
        return strengthSlider.getValue();
    }

    public double getNoise() {
        return noiseSlider.getValue();
    }

    public long getExtraNoiseSeed() {
        return Long.parseLong(extraNoiseSeedField.getText());
    }
}