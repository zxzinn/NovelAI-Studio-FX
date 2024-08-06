package com.zxzinn.novelai.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

public class Img2ImgGeneratorController extends AbstractGenerationController{
    @FXML private TextField extraNoiseSeedField;
    @FXML private Button uploadImageButton;

    private String base64Image;
    @FXML
    public void initialize() {
        super.initialize();
        if (extraNoiseSeedField != null) extraNoiseSeedField.setText("0");
    }

    @FXML
    private void handleUploadImage() {
        // 這裡添加處理圖片上傳的邏輯
        // 將選擇的圖片轉換為 base64 編碼並存儲在 base64Image 字段中
        System.out.println("Upload Image button clicked");
    }
}
