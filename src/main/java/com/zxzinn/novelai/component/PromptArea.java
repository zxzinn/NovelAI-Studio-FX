package com.zxzinn.novelai.component;

import com.zxzinn.novelai.utils.common.ResourcePaths;
import com.zxzinn.novelai.utils.embed.EmbedFileManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Log4j2
public class PromptArea extends VBox {

    @FXML private Label promptLabel;
    @Getter @FXML private TextArea promptTextArea;
    private ListView<String> autoCompleteList;
    private Popup autoCompletePopup;
    @Setter private EmbedFileManager embedFileManager;
    private boolean isAutoCompleteActive = false;
    private String lastPrefix = "";

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
        setupAutoCompleteList();
        setupAutoCompletePopup();
    }

    private void setupPromptTextArea() {
        promptTextArea.setWrapText(true);
        promptTextArea.setPrefHeight(100);
        promptTextArea.setMaxHeight(USE_PREF_SIZE);
        promptTextArea.setMinHeight(USE_PREF_SIZE);
        promptTextArea.getStyleClass().add("prompt-area");
    }

    private void setupAutoCompleteList() {
        autoCompleteList = new ListView<>();
        autoCompleteList.setPrefWidth(200);
        autoCompleteList.setPrefHeight(200);
        autoCompleteList.getStyleClass().add("auto-complete-list");
    }

    private void setupAutoCompletePopup() {
        autoCompletePopup = new Popup();
        autoCompletePopup.getContent().add(autoCompleteList);
        autoCompletePopup.setAutoHide(true);
    }

    private void setupListeners() {
        promptTextArea.textProperty().addListener((observable, oldValue, newValue) -> handleTextChange(oldValue, newValue));
        promptTextArea.caretPositionProperty().addListener((observable, oldValue, newValue) -> handleCaretChange(newValue.intValue()));
        promptTextArea.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPress);
        autoCompleteList.setOnMouseClicked(event -> selectAutoComplete());
        autoCompletePopup.addEventFilter(KeyEvent.KEY_PRESSED, this::handlePopupKeyPress);
    }

    private void handleTextChange(String oldValue, String newValue) {
        int caretPosition = promptTextArea.getCaretPosition();
        updateAutoComplete(caretPosition, oldValue, newValue);
    }

    private void handleCaretChange(int newPosition) {
        String text = promptTextArea.getText();
        updateAutoComplete(newPosition, text, text);
    }

    private void updateAutoComplete(int caretPosition, String oldValue, String newValue) {
        int start = newValue.lastIndexOf('<', caretPosition - 1);
        if (start != -1 && start < caretPosition) {
            String prefix = newValue.substring(start + 1, caretPosition);
            if (!prefix.equals(lastPrefix)) {
                showAutoComplete(prefix);
                lastPrefix = prefix;
            }
            isAutoCompleteActive = true;
        } else {
            hideAutoComplete();
        }
    }

    private void handleKeyPress(KeyEvent event) {
        if (autoCompletePopup.isShowing()) {
            switch (event.getCode()) {
                case UP:
                    navigateAutoComplete(-1);
                    event.consume();
                    break;
                case DOWN:
                    navigateAutoComplete(1);
                    event.consume();
                    break;
                case ESCAPE:
                    hideAutoComplete();
                    event.consume();
                    break;
            }
        } else if (event.isControlDown() && event.getCode() == KeyCode.SPACE) {
            showAutoComplete("");
            event.consume();
        }
    }

    private void handlePopupKeyPress(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.TAB) {
            selectAutoComplete();
            event.consume();
        }
    }

    private void navigateAutoComplete(int direction) {
        int size = autoCompleteList.getItems().size();
        int currentIndex = autoCompleteList.getSelectionModel().getSelectedIndex();
        int newIndex = (currentIndex + direction + size) % size;
        autoCompleteList.getSelectionModel().select(newIndex);
        autoCompleteList.scrollTo(newIndex);
    }

    private void showAutoComplete(String prefix) {
        if (embedFileManager == null) {
            log.warn("EmbedFileManager is null");
            return;
        }
        CompletableFuture<List<String>> futureMatches = embedFileManager.getMatchingEmbedsAsync(prefix);
        futureMatches.thenAcceptAsync(matches -> {
            if (!matches.isEmpty()) {
                Platform.runLater(() -> {
                    autoCompleteList.getItems().setAll(matches);
                    autoCompleteList.getSelectionModel().selectFirst();
                    positionAutoCompletePopup();
                    autoCompletePopup.show(promptTextArea, autoCompletePopup.getX(), autoCompletePopup.getY());
                });
                log.info("Showing autocomplete popup with " + matches.size() + " matches");
            } else {
                hideAutoComplete();
            }
        }).exceptionally(ex -> {
            log.error("Error fetching autocomplete matches: {}", ex.getMessage());
            return null;
        });
    }

    private void positionAutoCompletePopup() {
        Bounds bounds = promptTextArea.localToScreen(promptTextArea.getBoundsInLocal());
        autoCompletePopup.setX(bounds.getMinX());
        autoCompletePopup.setY(bounds.getMaxY());
    }

    private void hideAutoComplete() {
        Platform.runLater(() -> {
            autoCompletePopup.hide();
            promptTextArea.requestFocus();
        });
        isAutoCompleteActive = false;
        lastPrefix = "";
    }

    private void selectAutoComplete() {
        String selected = autoCompleteList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            insertAutoComplete(selected);
            hideAutoComplete();
        }
    }

    private void insertAutoComplete(String selected) {
        String text = promptTextArea.getText();
        int caretPosition = promptTextArea.getCaretPosition();
        int start = text.lastIndexOf('<', caretPosition - 1);
        if (start != -1) {
            String newText = text.substring(0, start + 1) + selected + ":1>" + text.substring(caretPosition);
            promptTextArea.setText(newText);
            promptTextArea.positionCaret(start + selected.length() + 3);
        }
    }

    public void setPromptLabel(String label) {
        promptLabel.setText(label);
    }

    public String getPromptText() {
        return promptTextArea.getText();
    }

    public void setPromptText(String text) {
        promptTextArea.setText(text);
    }
}