package com.zxzinn.novelai.component;

import com.zxzinn.novelai.utils.common.NAIConstants;
import com.zxzinn.novelai.utils.common.PropertiesManager;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.Value;

public class ApiSettingsPane extends TitledPane {

    private final PropertiesManager propertiesManager;
    private TextField apiKeyField;
    private ComboBox<String> modelComboBox;
    @Getter private ComboBox<String> generationModeComboBox;

    public ApiSettingsPane() {
        this.propertiesManager = PropertiesManager.getInstance();
        setText("API Settings");
        getStyleClass().add("settings-section");
        initializeComponents();
        loadSettings();
        setupListeners();
    }

    private void initializeComponents() {
        VBox content = new VBox(5);
        content.getStyleClass().add("settings-content");

        apiKeyField = new TextField();
        apiKeyField.getStyleClass().add("right-aligned-text");

        modelComboBox = new ComboBox<>();
        modelComboBox.setMaxWidth(Double.MAX_VALUE);
        modelComboBox.getStyleClass().add("right-aligned-text");

        generationModeComboBox = new ComboBox<>();
        generationModeComboBox.setMaxWidth(Double.MAX_VALUE);
        generationModeComboBox.getStyleClass().add("right-aligned-text");

        content.getChildren().addAll(
                new Label("API Key"),
                apiKeyField,
                new Label("模型"),
                modelComboBox,
                new Label("生成模式"),
                generationModeComboBox
        );

        setContent(content);
    }

    private void loadSettings() {
        apiKeyField.setText(propertiesManager.getString("apiKey", ""));
        modelComboBox.setItems(FXCollections.observableArrayList(NAIConstants.MODELS));
        modelComboBox.setValue(propertiesManager.getString("model", "nai-diffusion-3"));
        generationModeComboBox.getItems().addAll("Text2Image", "Image2Image");
        generationModeComboBox.setValue(propertiesManager.getString("generationMode", "Text2Image"));
    }

    private void setupListeners() {
        apiKeyField.textProperty().addListener((obs, oldVal, newVal) -> propertiesManager.setString("apiKey", newVal));
        modelComboBox.valueProperty().addListener((obs, oldVal, newVal) -> propertiesManager.setString("model", newVal));
        generationModeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> propertiesManager.setString("generationMode", newVal));
    }

    public String getApiKey() {
        return apiKeyField.getText();
    }

    public String getModel() {
        return modelComboBox.getValue();
    }
}