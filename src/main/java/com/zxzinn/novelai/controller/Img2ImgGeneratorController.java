package com.zxzinn.novelai.controller;

import com.zxzinn.novelai.service.ImageGenerationService;
import com.zxzinn.novelai.utils.image.ImageUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;

@Log4j2
public class Img2ImgGeneratorController extends AbstractGenerationController {
    public VBox historyImagesContainer;
    public ImageView mainImageView;
    public Button generateButton;
    @FXML public TextField extraNoiseSeedField;
    @FXML public Button uploadImageButton;

    public String base64Image;
    private ImageGenerationService imageGenerationService;

    @FXML
    @Override
    protected void handleGenerate() {
        generateButton.setDisable(true);
        new Thread(() -> {
            try {
                Image image = imageGenerationService.generateImg2Img(this);
                if (image == null) {
                    return;
                }

                Platform.runLater(() -> {
                    mainImageView.setImage(image);

                    ImageView historyImageView = new ImageView(image);
                    historyImageView.setFitWidth(150);
                    historyImageView.setFitHeight(150);
                    historyImagesContainer.getChildren().add(historyImageView);

                    try {
                        ImageUtils.saveImage(image, "generated_image.png");
                    } catch (IOException e) {
                        log.error("保存圖像時發生錯誤：" + e.getMessage(), e);
                    }
                });
            } catch (IOException e) {
                log.error("生成圖像時發生錯誤：" + e.getMessage(), e);
            } finally {
                Platform.runLater(() -> generateButton.setDisable(false));
            }
        }).start();
    }

    @FXML
    private void handleUploadImage() {
        // 這裡添加處理圖片上傳的邏輯
        // 將選擇的圖片轉換為 base64 編碼並存儲在 base64Image 字段中
        System.out.println("Upload Image button clicked");
    }
}
