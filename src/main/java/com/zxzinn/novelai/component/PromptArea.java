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

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

public class PromptArea extends VBox {

    private static final Logger LOGGER = Logger.getLogger(PromptArea.class.getName());

    @FXML private Label promptLabel;
    @Getter @FXML private TextArea promptTextArea;
    private ListView<String> autoCompleteList;
    private Popup autoCompletePopup;
    @Setter private EmbedFileManager embedFileManager;

    public PromptArea() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(ResourcePaths.PROMPT_AREA));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
            initializeComponents();
        } catch (IOException exception) {
            LOGGER.severe("Failed to load FXML: " + exception.getMessage());
            throw new RuntimeException(exception);
        }
    }

    private void initializeComponents() {
        promptTextArea.setWrapText(true);
        promptTextArea.setPrefHeight(100);
        promptTextArea.setMaxHeight(USE_PREF_SIZE);
        promptTextArea.setMinHeight(USE_PREF_SIZE);
        promptTextArea.getStyleClass().add("prompt-area");

        autoCompleteList = new ListView<>();
        autoCompleteList.setPrefWidth(200);
        autoCompleteList.setPrefHeight(200);
        autoCompleteList.getStyleClass().add("auto-complete-list");

        autoCompletePopup = new Popup();
        autoCompletePopup.getContent().add(autoCompleteList);
        autoCompletePopup.setAutoHide(true);

        setupAutoComplete();
    }

    private void setupAutoComplete() {
        promptTextArea.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPress);
        promptTextArea.textProperty().addListener((observable, oldValue, newValue) -> updateAutoComplete(newValue));
        autoCompleteList.setOnMouseClicked(event -> selectAutoComplete());

        // Add this new event handler
        autoCompletePopup.addEventFilter(KeyEvent.KEY_PRESSED, this::handlePopupKeyPress);
    }

    private void handleKeyPress(KeyEvent event) {
        LOGGER.info("Key pressed in TextArea: " + event.getCode() + ", Popup showing: " + autoCompletePopup.isShowing());

        if (event.getCode() == KeyCode.ENTER && autoCompletePopup.isShowing()) {
            LOGGER.info("Enter pressed in TextArea while popup is showing");
            selectAutoComplete();
            event.consume();
        } else if (autoCompletePopup.isShowing()) {
            switch (event.getCode()) {
                case UP -> {
                    navigateAutoComplete(-1);
                    event.consume();
                }
                case DOWN -> {
                    navigateAutoComplete(1);
                    event.consume();
                }
                case ESCAPE -> {
                    hideAutoComplete();
                    event.consume();
                }
            }
        }
    }

    private void handlePopupKeyPress(KeyEvent event) {
        LOGGER.info("Key pressed in Popup: " + event.getCode());
        if (event.getCode() == KeyCode.ENTER) {
            LOGGER.info("Enter pressed in Popup");
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

    private void updateAutoComplete(String newValue) {
        int caretPosition = promptTextArea.getCaretPosition();
        int start = newValue.lastIndexOf('<', caretPosition - 1);
        if (start != -1 && start < caretPosition) {
            String prefix = newValue.substring(start + 1, caretPosition);
            showAutoComplete(prefix);
        } else {
            hideAutoComplete();
        }
    }

    private void showAutoComplete(String prefix) {
        if (embedFileManager == null) {
            LOGGER.warning("EmbedFileManager is null");
            return;
        }
        List<String> matches = embedFileManager.getMatchingEmbeds(prefix);
        if (!matches.isEmpty()) {
            autoCompleteList.getItems().setAll(matches);
            autoCompleteList.getSelectionModel().selectFirst();
            positionAutoCompletePopup();
            autoCompletePopup.show(promptTextArea,
                    autoCompletePopup.getX(),
                    autoCompletePopup.getY());
            Platform.runLater(() -> {
                promptTextArea.requestFocus();
                autoCompleteList.requestFocus();
            });
            LOGGER.info("Showing autocomplete popup with " + matches.size() + " matches");
        } else {
            hideAutoComplete();
        }
    }

    private void positionAutoCompletePopup() {
        Bounds bounds = promptTextArea.localToScreen(promptTextArea.getBoundsInLocal());
        autoCompletePopup.setX(bounds.getMinX());
        autoCompletePopup.setY(bounds.getMaxY());
    }

    private void hideAutoComplete() {
        autoCompletePopup.hide();
        LOGGER.info("Hiding autocomplete popup");
    }

    private void selectAutoComplete() {
        String selected = autoCompleteList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            insertAutoComplete(selected);
            hideAutoComplete();
            LOGGER.info("Autocomplete selected: " + selected);
        }
    }

    private void insertAutoComplete(String selected) {
        String text = promptTextArea.getText();
        int caretPosition = promptTextArea.getCaretPosition();
        int start = text.lastIndexOf('<', caretPosition - 1);
        if (start != -1) {
            String newText = text.substring(0, start + 1) + selected + ":1>" + text.substring(caretPosition);
            promptTextArea.setText(newText);
            promptTextArea.positionCaret(start + selected.length() + 3); // +3 for ":1>"
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