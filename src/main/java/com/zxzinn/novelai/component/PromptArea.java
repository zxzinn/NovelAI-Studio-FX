package com.zxzinn.novelai.component;

import com.zxzinn.novelai.utils.common.ResourcePaths;
import com.zxzinn.novelai.utils.embed.EmbedFileManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;

@Log4j2
public class PromptArea extends VBox {

    @FXML private Label promptLabel;
    @Getter @FXML private TextArea promptTextArea;
    private AutoCompleteHandler autoCompleteHandler;

    public PromptArea() {
        loadFXML();
        initializeComponents();
        setupListeners();
    }

    private void loadFXML() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(ResourcePaths.PROMPT_AREA));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            log.error("Failed to load FXML: {}", exception.getMessage());
            throw new RuntimeException(exception);
        }
    }

    private void initializeComponents() {
        setupPromptTextArea();
        autoCompleteHandler = new AutoCompleteHandler(promptTextArea);
    }

    private void setupPromptTextArea() {
        promptTextArea.setWrapText(true);
        promptTextArea.setPrefHeight(100);
        promptTextArea.setMaxHeight(USE_PREF_SIZE);
        promptTextArea.setMinHeight(USE_PREF_SIZE);
        promptTextArea.getStyleClass().add("prompt-area");
    }

    private void setupListeners() {
        promptTextArea.textProperty().addListener((observable, oldValue, newValue) ->
                autoCompleteHandler.handleTextChange(oldValue, newValue));
        promptTextArea.caretPositionProperty().addListener((observable, oldValue, newValue) ->
                autoCompleteHandler.handleCaretChange(newValue.intValue()));
        promptTextArea.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED,
                autoCompleteHandler::handleKeyPress);
    }

    public void setEmbedFileManager(EmbedFileManager embedFileManager) {
        autoCompleteHandler.setEmbedFileManager(embedFileManager);
    }

    public void setPromptLabel(String label) {
        promptLabel.setText(label);
    }

    public String getPromptText() {
        return promptTextArea.getText();
    }

    public void setPromptText(String text) {
        if (text == null) {
            text = "";
        }
        promptTextArea.setText(text);
    }
}