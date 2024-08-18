package com.zxzinn.novelai.component;

import com.zxzinn.novelai.utils.tokenizer.SimpleTokenizer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
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
    @FXML private ProgressBar tokenProgressBar;
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

        tokenProgressBar.setPrefHeight(6);
        tokenProgressBar.getStyleClass().add("token-progress-bar");
    }

    private void initializeTokenizer() {
        try {
            String resourcePath = "/com/zxzinn/novelai/tokenizers/bpe_simple_vocab_16e6.txt.gz";
            InputStream inputStream = getClass().getResourceAsStream(resourcePath);
            if (inputStream == null) {
                throw new IOException("Cannot find resource: " + resourcePath);
            }

            Path tempFile = Files.createTempFile("bpe_vocab", ".txt.gz");
            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);

            tokenizer = new SimpleTokenizer(tempFile.toString());

            tempFile.toFile().deleteOnExit();
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize tokenizer", e);
        }
    }

    private void updateTokenCount() {
        String text = previewTextArea.getText();
        int tokenCount = tokenizer.encode(text).size();
        double ratio = (double) tokenCount / TOKEN_LIMIT;

        Platform.runLater(() -> {
            tokenProgressBar.setProgress(ratio);
            tokenCountLabel.setText(String.format("%d / %d", tokenCount, TOKEN_LIMIT));
            updateStyles(ratio);
        });
    }

    private void updateStyles(double ratio) {
        previewTextArea.getStyleClass().removeAll("prompt-preview-low", "prompt-preview-medium", "prompt-preview-high", "prompt-preview-max");
        tokenProgressBar.getStyleClass().remove("token-progress-bar-warning");

        if (ratio < 0.5) {
            previewTextArea.getStyleClass().add("prompt-preview-low");
        } else if (ratio < 0.75) {
            previewTextArea.getStyleClass().add("prompt-preview-medium");
        } else if (ratio < 0.9) {
            previewTextArea.getStyleClass().add("prompt-preview-high");
        } else {
            previewTextArea.getStyleClass().add("prompt-preview-max");
            tokenProgressBar.getStyleClass().add("token-progress-bar-warning");
        }
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