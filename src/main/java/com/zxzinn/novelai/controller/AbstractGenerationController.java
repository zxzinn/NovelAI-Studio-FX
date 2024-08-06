package com.zxzinn.novelai.controller;

import com.zxzinn.novelai.NAIConstants;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public abstract class AbstractGenerationController {

    @FXML private TextField apiKeyField;
    @FXML private ComboBox<String> modelComboBox;
    @FXML private TextField widthField;
    @FXML private TextField heightField;
    @FXML private TextField ratioField;
    @FXML private TextField countField;
    @FXML private ComboBox<String> samplerComboBox;
    @FXML private CheckBox smeaCheckBox;
    @FXML private CheckBox smeaDynCheckBox;
    @FXML private TextField stepsField;
    @FXML private TextField seedField;
    @FXML private TextArea positivePromptArea;
    @FXML private TextArea negativePromptArea;
    @FXML private TextArea positivePromptPreviewArea;
    @FXML private TextArea negativePromptPreviewArea;
    @FXML private ComboBox<String> generateCountComboBox;

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
    private void handleGenerate() {
        // 處理生成按鈕點擊事件
        System.out.println("生成按鈕被點擊");
    }

    @FXML
    private void handleRefreshEmbed() {
        // 處理刷新嵌入按鈕點擊事件
        System.out.println("刷新嵌入按鈕被點擊");
    }

    @FXML
    private void handleLockEmbed() {
        // 處理鎖定嵌入按鈕點擊事件
        System.out.println("鎖定嵌入按鈕被點擊");
    }

    @FXML
    private void handleFit() {
        // 處理適應按鈕點擊事件
        System.out.println("適應按鈕被點擊");
    }

    @FXML
    private void handleOriginalSize() {
        // 處理原始大小按鈕點擊事件
        System.out.println("原始大小按鈕被點擊");
    }
}