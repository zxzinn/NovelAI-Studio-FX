package com.zxzinn.novelai.viewmodel;

import com.zxzinn.novelai.service.generation.AutoCompleteHandler;
import com.zxzinn.novelai.utils.common.ResourcePaths;
import com.zxzinn.novelai.utils.embed.EmbedFileManager;
import com.zxzinn.novelai.utils.tokenizer.SimpleTokenizer;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.scene.input.KeyEvent;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class PromptComponentViewModel {

    private static final int TOKEN_LIMIT = 225;

    private final StringProperty promptLabelText = new SimpleStringProperty("提示詞");
    private final StringProperty promptText = new SimpleStringProperty("");
    private final StringProperty previewLabelText = new SimpleStringProperty("提示詞預覽");
    private final StringProperty previewText = new SimpleStringProperty("");
    private final DoubleProperty tokenProgress = new SimpleDoubleProperty(0);
    private final StringProperty tokenCountText = new SimpleStringProperty("0 / " + TOKEN_LIMIT);
    private final ObjectProperty<TokenLevel> tokenLevel = new SimpleObjectProperty<>(TokenLevel.LOW);
    private final BooleanProperty locked = new SimpleBooleanProperty(false);

    @Getter @Setter private Runnable refreshAction;
    @Getter @Setter private Runnable lockAction;

    private AutoCompleteHandler autoCompleteHandler;
    private SimpleTokenizer tokenizer;

    public PromptComponentViewModel() {
        this.autoCompleteHandler = new AutoCompleteHandler(null);
        initializeTokenizer();
        setupListeners();
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

    private void setupListeners() {
        previewText.addListener((observable, oldValue, newValue) -> updateTokenCount());
    }

    public void setEmbedFileManager(EmbedFileManager embedFileManager) {
        autoCompleteHandler.setEmbedFileManager(embedFileManager);
    }

    public void handleTextChange(String oldValue, String newValue) {
        autoCompleteHandler.handleTextChange(oldValue, newValue);
        previewText.set(newValue); // 簡單起見,這裡直接設置預覽文本,實際應用中可能需要更複雜的處理
    }

    public void handleCaretChange(int newValue) {
        autoCompleteHandler.handleCaretChange(newValue);
    }

    public void handleKeyPress(KeyEvent event) {
        autoCompleteHandler.handleKeyPress(event);
    }

    public void refresh() {
        if (refreshAction != null) {
            refreshAction.run();
        }
    }

    public void toggleLock() {
        locked.set(!locked.get());
        if (lockAction != null) {
            lockAction.run();
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

    // Getters and setters for properties
    public StringProperty promptLabelTextProperty() {
        return promptLabelText;
    }

    public void setPromptLabelText(String text) {
        promptLabelText.set(text);
    }

    public StringProperty promptTextProperty() {
        return promptText;
    }

    public String getPromptText() {
        return promptText.get();
    }

    public void setPromptText(String text) {
        promptText.set(text);
    }

    public StringProperty previewLabelTextProperty() {
        return previewLabelText;
    }

    public void setPreviewLabelText(String text) {
        previewLabelText.set(text);
    }

    public StringProperty previewTextProperty() {
        return previewText;
    }

    public String getPreviewText() {
        return previewText.get();
    }

    public void setPreviewText(String text) {
        previewText.set(text);
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

    public BooleanProperty lockedProperty() {
        return locked;
    }

    public void setLocked(boolean isLocked) {
        locked.set(isLocked);
    }

    public enum TokenLevel {
        LOW, MEDIUM, HIGH, MAX
    }
}