package com.zxzinn.novelai.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;

public class FileManagerController {
    public Button originalSizeButton;
    public Button fitButton;
    @FXML private TreeView<String> fileTreeView;
    @FXML private ImageView previewImageView;
    @FXML private ListView<String> metadataListView;

    @FXML
    private void initialize() {
        // 初始化文件管理器標籤頁的控件
    }

    @FXML
    private void handleFit() {
        System.out.println("Fit button clicked");
    }

    @FXML
    private void handleOriginalSize() {
        System.out.println("Original Size button clicked");
    }

    // 添加其他需要的方法...
}