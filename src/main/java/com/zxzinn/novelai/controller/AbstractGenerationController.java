package com.zxzinn.novelai.controller;

import com.zxzinn.novelai.NAIConstants;
import com.zxzinn.novelai.api.NovelAIAPIClient;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

public abstract class AbstractGenerationController {
    public VBox historyImagesContainer;
    public ImageView mainImageView;
    NovelAIAPIClient apiClient = new NovelAIAPIClient();

    @FXML public TextField apiKeyField;
    @FXML public ComboBox<String> modelComboBox;
    @FXML public TextField widthField;
    @FXML public TextField heightField;
    @FXML public TextField ratioField;
    @FXML public TextField countField;
    @FXML public ComboBox<String> samplerComboBox;
    @FXML public CheckBox smeaCheckBox;
    @FXML public CheckBox smeaDynCheckBox;
    @FXML public TextField stepsField;
    @FXML public TextField seedField;
    @FXML public TextArea positivePromptArea;
    @FXML public TextArea negativePromptArea;
    @FXML public TextArea positivePromptPreviewArea;
    @FXML public TextArea negativePromptPreviewArea;
    @FXML public ComboBox<String> generateCountComboBox;

    @FXML
    public void initialize() {
        if (apiKeyField != null) apiKeyField.setText("");

        if (modelComboBox != null) {
            modelComboBox.setItems(FXCollections.observableArrayList(NAIConstants.MODELS));
            modelComboBox.setValue(NAIConstants.MODELS[0]);
        }

        if (widthField != null) widthField.setText("832");
        if (heightField != null) heightField.setText("1216");
        if (ratioField != null) ratioField.setText("7");
        if (countField != null) countField.setText("1");

        if (samplerComboBox != null) {
            samplerComboBox.setItems(FXCollections.observableArrayList(NAIConstants.SAMPLERS));
            samplerComboBox.setValue(NAIConstants.SAMPLERS[0]);
        }

        if (stepsField != null) stepsField.setText("28");
        if (seedField != null) seedField.setText("0");

        if (generateCountComboBox != null) {
            generateCountComboBox.getItems().addAll("1", "2", "3", "4", "無限");
            generateCountComboBox.setValue("1");
        }
    }

    @FXML
    protected abstract void handleGenerate();
}