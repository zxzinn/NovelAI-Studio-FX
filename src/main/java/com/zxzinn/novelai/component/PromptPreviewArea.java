package com.zxzinn.novelai.component;

import com.zxzinn.novelai.test.SimpleTokenizer;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import lombok.Getter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class PromptPreviewArea extends VBox {

    @FXML private Label previewLabel;
    @Getter @FXML private TextArea previewTextArea;
    @FXML private Label tokenCountLabel;

    private SimpleTokenizer tokenizer;
    private static final int TOKEN_LIMIT = 225;

    public PromptPreviewArea() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/zxzinn/novelai/PromptPreviewArea.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
            initializeComponents();
            initializeTokenizer();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    private void initializeComponents() {
        previewTextArea.setWrapText(true);
        previewTextArea.setPrefHeight(100);
        previewTextArea.setMaxHeight(USE_PREF_SIZE);
        previewTextArea.setMinHeight(USE_PREF_SIZE);
        previewTextArea.setEditable(false);

        previewTextArea.textProperty().addListener((observable, oldValue, newValue) -> updateTokenCount());

        tokenCountLabel = new Label();
        tokenCountLabel.getStyleClass().add("token-count-label");
        getChildren().add(tokenCountLabel);
    }

    private void initializeTokenizer() {
        try {
            String resourcePath = "/com/zxzinn/novelai/tokenizers/bpe_simple_vocab_16e6.txt.gz";
            InputStream inputStream = getClass().getResourceAsStream(resourcePath);
            if (inputStream == null) {
                throw new IOException("Cannot find resource: " + resourcePath);
            }

            // 創建一個臨時文件
            Path tempFile = Files.createTempFile("bpe_vocab", ".txt.gz");
            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);

            // 使用臨時文件初始化tokenizer
            tokenizer = new SimpleTokenizer(tempFile.toString());

            // 在JVM退出時刪除臨時文件
            tempFile.toFile().deleteOnExit();
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize tokenizer", e);
        }
    }

    private void updateTokenCount() {
        String text = previewTextArea.getText();
        int tokenCount = tokenizer.encode(text).size();
        tokenCountLabel.setText(String.format("Token Count: %d / %d", tokenCount, TOKEN_LIMIT));
        updateBackgroundColor(tokenCount);
    }

    private void updateBackgroundColor(int tokenCount) {
        double ratio = (double) tokenCount / TOKEN_LIMIT;
        String color;
        if (ratio < 0.5) {
            color = "lightgreen";
        } else if (ratio < 0.75) {
            color = "yellow";
        } else if (ratio < 1) {
            color = "orange";
        } else {
            color = "red";
        }
        previewTextArea.setStyle("-fx-control-inner-background: " + color + ";");
    }

    public void setPreviewLabel(String label) {
        previewLabel.setText(label);
    }

    public String getPreviewText() {
        return previewTextArea.getText();
    }

    public void setPreviewText(String text) {
        previewTextArea.setText(text);
    }
}