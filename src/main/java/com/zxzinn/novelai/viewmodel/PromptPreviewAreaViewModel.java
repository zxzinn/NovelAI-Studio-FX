package com.zxzinn.novelai.viewmodel;

import com.zxzinn.novelai.utils.common.ResourcePaths;
import com.zxzinn.novelai.utils.tokenizer.SimpleTokenizer;
import javafx.beans.property.*;
import javafx.application.Platform;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class PromptPreviewAreaViewModel {

    private static final int TOKEN_LIMIT = 225;

    private final StringProperty previewLabelText = new SimpleStringProperty("提示詞預覽");
    private final StringProperty previewText = new SimpleStringProperty("");
    private final DoubleProperty tokenProgress = new SimpleDoubleProperty(0);
    private final StringProperty tokenCountText = new SimpleStringProperty("0 / " + TOKEN_LIMIT);
    private final ObjectProperty<TokenLevel> tokenLevel = new SimpleObjectProperty<>(TokenLevel.LOW);

    private SimpleTokenizer tokenizer;

    public PromptPreviewAreaViewModel() {
        initializeTokenizer();
        previewText.addListener((observable, oldValue, newValue) -> updateTokenCount());
    }

    private void initializeTokenizer() {
        try {
            InputStream inputStream = getClass().getResourceAsStream(ResourcePaths.SIMPLE_TOKENIZER);
            if (inputStream == null) {
                throw new IOException("Cannot find resource");
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
        String text = previewText.get();
        int tokenCount = tokenizer.encode(text).size();
        double ratio = (double) tokenCount / TOKEN_LIMIT;

        Platform.runLater(() -> {
            tokenProgress.set(ratio);
            tokenCountText.set(String.format("%d / %d", tokenCount, TOKEN_LIMIT));
            updateTokenLevel(ratio);
        });
    }

    private void updateTokenLevel(double ratio) {
        if (ratio < 0.5) {
            tokenLevel.set(TokenLevel.LOW);
        } else if (ratio < 0.75) {
            tokenLevel.set(TokenLevel.MEDIUM);
        } else if (ratio < 0.9) {
            tokenLevel.set(TokenLevel.HIGH);
        } else {
            tokenLevel.set(TokenLevel.MAX);
        }
    }

    public StringProperty previewLabelTextProperty() {
        return previewLabelText;
    }

    public StringProperty previewTextProperty() {
        return previewText;
    }

    public DoubleProperty tokenProgressProperty() {
        return tokenProgress;
    }

    public StringProperty tokenCountTextProperty() {
        return tokenCountText;
    }

    public ObjectProperty<TokenLevel> tokenLevelProperty() {
        return tokenLevel;
    }

    public enum TokenLevel {
        LOW, MEDIUM, HIGH, MAX
    }
}