package com.zxzinn.novelai.component;

import com.zxzinn.novelai.service.generation.AutoCompleteHandler;
import com.zxzinn.novelai.utils.embed.EmbedFileManager;
import com.zxzinn.novelai.viewmodel.PromptComponentViewModel;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lombok.Getter;
import org.kordamp.ikonli.javafx.FontIcon;

public class PromptComponent extends HBox {

    private final PromptComponentViewModel viewModel;
    private Label promptLabel;
    @Getter private TextArea promptTextArea;
    private Button refreshButton;
    private Button lockButton;
    private FontIcon lockIcon;
    private Label previewLabel;
    @Getter private TextArea previewTextArea;
    private ProgressBar tokenProgressBar;
    private Label tokenCountLabel;

    public PromptComponent() {
        this.viewModel = new PromptComponentViewModel();
        initializeComponents();
        setupLayout();
        setupBindings();
        setupListeners();
    }

    private void initializeComponents() {
        promptLabel = new Label();
        promptTextArea = new TextArea();
        setupPromptTextArea();

        refreshButton = createButton("fas-sync-alt", "refresh-button");
        lockButton = createButton("fas-lock-open", "lock-button");
        lockIcon = (FontIcon) lockButton.getGraphic();

        previewLabel = new Label();
        previewTextArea = new TextArea();
        setupPreviewTextArea();

        tokenProgressBar = new ProgressBar();
        tokenCountLabel = new Label();
    }

    private void setupLayout() {
        setSpacing(10);
        setAlignment(Pos.TOP_CENTER);

        VBox promptBox = new VBox(5);
        promptBox.getChildren().addAll(promptLabel, promptTextArea);
        HBox.setHgrow(promptBox, Priority.ALWAYS);

        VBox controlsBox = new VBox(5);
        controlsBox.setAlignment(Pos.CENTER);
        controlsBox.getChildren().addAll(refreshButton, lockButton);

        VBox previewBox = new VBox(5);
        StackPane progressContainer = new StackPane(tokenProgressBar, tokenCountLabel);
        progressContainer.getStyleClass().add("token-progress-container");
        previewBox.getChildren().addAll(previewLabel, previewTextArea, progressContainer);
        HBox.setHgrow(previewBox, Priority.ALWAYS);

        getChildren().addAll(promptBox, controlsBox, previewBox);
    }

    private void setupPromptTextArea() {
        promptTextArea.setWrapText(true);
        promptTextArea.setPrefHeight(100);
        promptTextArea.setMaxHeight(USE_PREF_SIZE);
        promptTextArea.setMinHeight(USE_PREF_SIZE);
        promptTextArea.getStyleClass().add("prompt-area");
    }

    private void setupPreviewTextArea() {
        previewTextArea.setWrapText(true);
        previewTextArea.setPrefHeight(100);
        previewTextArea.setMaxHeight(USE_PREF_SIZE);
        previewTextArea.setMinHeight(USE_PREF_SIZE);
        previewTextArea.setEditable(false);
        previewTextArea.getStyleClass().add("prompt-preview");
    }

    private Button createButton(String iconLiteral, String styleClass) {
        FontIcon icon = new FontIcon(iconLiteral);
        icon.setIconSize(16);

        Button button = new Button();
        button.setGraphic(icon);
        button.getStyleClass().add(styleClass);
        button.setPrefSize(30, 30);

        return button;
    }

    private void setupBindings() {
        promptLabel.textProperty().bind(viewModel.promptLabelTextProperty());
        promptTextArea.textProperty().bindBidirectional(viewModel.promptTextProperty());
        previewLabel.textProperty().bind(viewModel.previewLabelTextProperty());
        previewTextArea.textProperty().bindBidirectional(viewModel.previewTextProperty());
        tokenProgressBar.progressProperty().bind(viewModel.tokenProgressProperty());
        tokenCountLabel.textProperty().bind(viewModel.tokenCountTextProperty());

        viewModel.lockedProperty().addListener((observable, oldValue, newValue) -> {
            lockIcon.setIconLiteral(newValue ? "fas-lock" : "fas-lock-open");
            if (newValue) {
                lockButton.getStyleClass().add("locked");
            } else {
                lockButton.getStyleClass().remove("locked");
            }
        });

        viewModel.tokenLevelProperty().addListener((observable, oldValue, newValue) -> updateStyles(newValue));
    }

    private void setupListeners() {
        promptTextArea.textProperty().addListener((observable, oldValue, newValue) ->
                viewModel.handleTextChange(oldValue, newValue));
        promptTextArea.caretPositionProperty().addListener((observable, oldValue, newValue) ->
                viewModel.handleCaretChange(newValue.intValue()));
        promptTextArea.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED,
                viewModel::handleKeyPress);

        refreshButton.setOnAction(event -> viewModel.refresh());
        lockButton.setOnAction(event -> viewModel.toggleLock());
    }

    private void updateStyles(PromptComponentViewModel.TokenLevel tokenLevel) {
        previewTextArea.getStyleClass().removeAll("prompt-preview-low", "prompt-preview-medium", "prompt-preview-high", "prompt-preview-max");
        tokenProgressBar.getStyleClass().remove("token-progress-bar-warning");

        switch (tokenLevel) {
            case LOW -> previewTextArea.getStyleClass().add("prompt-preview-low");
            case MEDIUM -> previewTextArea.getStyleClass().add("prompt-preview-medium");
            case HIGH -> previewTextArea.getStyleClass().add("prompt-preview-high");
            case MAX -> {
                previewTextArea.getStyleClass().add("prompt-preview-max");
                tokenProgressBar.getStyleClass().add("token-progress-bar-warning");
            }
        }
    }

    public void setEmbedFileManager(EmbedFileManager embedFileManager) {
        viewModel.setEmbedFileManager(embedFileManager);
    }

    public void setPromptLabel(String label) {
        viewModel.setPromptLabelText(label);
    }

    public void setPreviewLabel(String label) {
        viewModel.setPreviewLabelText(label);
    }

    public String getPromptText() {
        return viewModel.getPromptText();
    }

    public void setPromptText(String text) {
        viewModel.setPromptText(text);
    }

    public String getPreviewText() {
        return viewModel.getPreviewText();
    }

    public void setPreviewText(String text) {
        viewModel.setPreviewText(text);
    }

    public void setOnRefreshAction(Runnable action) {
        viewModel.setRefreshAction(action);
    }

    public void setOnLockAction(Runnable action) {
        viewModel.setLockAction(action);
    }

    public void setLockState(boolean isLocked) {
        viewModel.setLocked(isLocked);
    }
}