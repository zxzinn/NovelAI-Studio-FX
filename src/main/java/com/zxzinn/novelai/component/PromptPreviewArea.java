package com.zxzinn.novelai.component;

import com.zxzinn.novelai.viewmodel.PromptPreviewAreaViewModel;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.Getter;

public class PromptPreviewArea extends VBox {

    private Label previewLabel;
    @Getter private TextArea previewTextArea;
    private ProgressBar tokenProgressBar;
    private Label tokenCountLabel;

    private final PromptPreviewAreaViewModel viewModel;

    public PromptPreviewArea() {
        this.viewModel = new PromptPreviewAreaViewModel();
        initializeComponents();
        setupBindings();
    }

    private void initializeComponents() {
        previewLabel = new Label();
        previewTextArea = new TextArea();
        tokenProgressBar = new ProgressBar();
        tokenCountLabel = new Label();

        previewTextArea.setWrapText(true);
        previewTextArea.setPrefHeight(100);
        previewTextArea.setMaxHeight(USE_PREF_SIZE);
        previewTextArea.setMinHeight(USE_PREF_SIZE);
        previewTextArea.setEditable(false);
        previewTextArea.getStyleClass().add("prompt-preview");

        tokenProgressBar.setPrefHeight(6);
        tokenProgressBar.getStyleClass().add("token-progress-bar");
        tokenProgressBar.setMaxWidth(Double.MAX_VALUE);

        StackPane progressContainer = new StackPane(tokenProgressBar, tokenCountLabel);
        progressContainer.getStyleClass().add("token-progress-container");

        getChildren().addAll(previewLabel, previewTextArea, progressContainer);
        setSpacing(5.0);
    }

    private void setupBindings() {
        previewLabel.textProperty().bind(viewModel.previewLabelTextProperty());
        previewTextArea.textProperty().bindBidirectional(viewModel.previewTextProperty());
        tokenProgressBar.progressProperty().bind(viewModel.tokenProgressProperty());
        tokenCountLabel.textProperty().bind(viewModel.tokenCountTextProperty());

        viewModel.tokenLevelProperty().addListener((observable, oldValue, newValue) -> updateStyles(newValue));
    }

    private void updateStyles(PromptPreviewAreaViewModel.TokenLevel tokenLevel) {
        previewTextArea.getStyleClass().removeAll("prompt-preview-low", "prompt-preview-medium", "prompt-preview-high", "prompt-preview-max");
        tokenProgressBar.getStyleClass().remove("token-progress-bar-warning");

        switch (tokenLevel) {
            case LOW:
                previewTextArea.getStyleClass().add("prompt-preview-low");
                break;
            case MEDIUM:
                previewTextArea.getStyleClass().add("prompt-preview-medium");
                break;
            case HIGH:
                previewTextArea.getStyleClass().add("prompt-preview-high");
                break;
            case MAX:
                previewTextArea.getStyleClass().add("prompt-preview-max");
                tokenProgressBar.getStyleClass().add("token-progress-bar-warning");
                break;
        }
    }

    public void setPreviewLabel(String label) {
        viewModel.previewLabelTextProperty().set(label);
    }

    public String getPreviewText() {
        return viewModel.previewTextProperty().get();
    }

    public void setPreviewText(String text) {
        viewModel.previewTextProperty().set(text);
    }
}