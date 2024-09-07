package com.zxzinn.novelai.component;

import com.zxzinn.novelai.utils.embed.EmbedFileManager;
import com.zxzinn.novelai.viewmodel.PromptAreaViewModel;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class PromptArea extends VBox {

    private Label promptLabel;
    @Getter private TextArea promptTextArea;
    private PromptAreaViewModel viewModel;

    public PromptArea() {
        this.viewModel = new PromptAreaViewModel();
        initializeComponents();
        setupBindings();
        setupListeners();
    }

    private void initializeComponents() {
        promptLabel = new Label();
        promptTextArea = new TextArea();
        setupPromptTextArea();
        getChildren().addAll(promptLabel, promptTextArea);
        setSpacing(5.0);
    }

    private void setupPromptTextArea() {
        promptTextArea.setWrapText(true);
        promptTextArea.setPrefHeight(100);
        promptTextArea.setMaxHeight(USE_PREF_SIZE);
        promptTextArea.setMinHeight(USE_PREF_SIZE);
        promptTextArea.getStyleClass().add("prompt-area");
    }

    private void setupBindings() {
        promptLabel.textProperty().bind(viewModel.promptLabelTextProperty());
        promptTextArea.textProperty().bindBidirectional(viewModel.promptTextProperty());
    }

    private void setupListeners() {
        viewModel.setTextArea(promptTextArea);
        promptTextArea.textProperty().addListener((observable, oldValue, newValue) ->
                viewModel.handleTextChange(oldValue, newValue));
        promptTextArea.caretPositionProperty().addListener((observable, oldValue, newValue) ->
                viewModel.handleCaretChange(newValue.intValue()));
        promptTextArea.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED,
                viewModel::handleKeyPress);
    }

    public void setEmbedFileManager(EmbedFileManager embedFileManager) {
        viewModel.setEmbedFileManager(embedFileManager);
    }

    public void setPromptLabel(String label) {
        viewModel.promptLabelTextProperty().set(label);
    }

    public String getPromptText() {
        return viewModel.promptTextProperty().get();
    }

    public void setPromptText(String text) {
        viewModel.promptTextProperty().set(text != null ? text : "");
    }
}