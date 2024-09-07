package com.zxzinn.novelai.viewmodel;

import com.zxzinn.novelai.service.generation.AutoCompleteHandler;
import com.zxzinn.novelai.utils.embed.EmbedFileManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyEvent;

public class PromptAreaViewModel {

    private final StringProperty promptLabelText = new SimpleStringProperty("提示詞");
    private final StringProperty promptText = new SimpleStringProperty("");
    private AutoCompleteHandler autoCompleteHandler;

    public PromptAreaViewModel() {
        this.autoCompleteHandler = new AutoCompleteHandler(null); // We'll set the TextArea later
    }

    public StringProperty promptLabelTextProperty() {
        return promptLabelText;
    }

    public StringProperty promptTextProperty() {
        return promptText;
    }

    public void setEmbedFileManager(EmbedFileManager embedFileManager) {
        autoCompleteHandler.setEmbedFileManager(embedFileManager);
    }

    public void handleTextChange(String oldValue, String newValue) {
        autoCompleteHandler.handleTextChange(oldValue, newValue);
    }

    public void handleCaretChange(int newValue) {
        autoCompleteHandler.handleCaretChange(newValue);
    }

    public void handleKeyPress(KeyEvent event) {
        autoCompleteHandler.handleKeyPress(event);
    }

    public void setTextArea(TextArea textArea) {
        this.autoCompleteHandler = new AutoCompleteHandler(textArea);
    }
}