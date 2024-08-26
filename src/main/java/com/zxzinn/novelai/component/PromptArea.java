package com.zxzinn.novelai.component;

import com.zxzinn.novelai.utils.common.ResourcePaths;
import com.zxzinn.novelai.utils.embed.EmbedFileManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Log4j2
public class PromptArea extends VBox {

    @FXML private Label promptLabel;
    @Getter @FXML private TextArea promptTextArea;
    private ListView<EmbedFileManager.EmbedFile> autoCompleteList;
    private Popup autoCompletePopup;
    @Setter private EmbedFileManager embedFileManager;
    private boolean isAutoCompleteActive = false;
    private String lastQuery = "";
    private int autoCompleteStartIndex = -1;

    private static final Pattern EMBED_PATTERN = Pattern.compile(",?<([^>]+)>(?=,|$)");

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
        autoCompleteList.setPrefWidth(400);
        autoCompleteList.setPrefHeight(300);
        autoCompleteList.getStyleClass().add("auto-complete-list");
        autoCompleteList.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(EmbedFileManager.EmbedFile item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox hbox = new HBox(10);
                    hbox.setAlignment(Pos.CENTER_LEFT);

                    Label fileNameLabel = new Label(item.fileName());
                    fileNameLabel.setMaxWidth(Double.MAX_VALUE);
                    HBox.setHgrow(fileNameLabel, Priority.ALWAYS);

                    Label folderLabel = new Label(item.folder());
                    folderLabel.getStyleClass().add("folder-label");
                    folderLabel.setAlignment(Pos.CENTER_RIGHT);
                    folderLabel.setMaxWidth(Double.MAX_VALUE);
                    HBox.setHgrow(folderLabel, Priority.ALWAYS);

                    hbox.getChildren().addAll(fileNameLabel, folderLabel);
                    setGraphic(hbox);
                }
            }
        });
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

    private void handleTextChange(@NotNull String oldValue, @NotNull String newValue) {
        int caretPosition = promptTextArea.getCaretPosition();
        if (newValue.length() > oldValue.length() && caretPosition > 0 && caretPosition <= newValue.length()) {
            char lastChar = newValue.charAt(caretPosition - 1);
            if (lastChar == '<') {
                updateAutoComplete(caretPosition, oldValue, newValue);
            }
        } else {
            if (!isValidAutoCompletePosition(caretPosition)) {
                hideAutoComplete();
            }
        }
    }

    private void handleCaretChange(int newPosition) {
        if (isValidAutoCompletePosition(newPosition)) {
            updateAutoComplete(newPosition, promptTextArea.getText(), promptTextArea.getText());
        } else {
            hideAutoComplete();
        }
    }

    private void updateAutoComplete(int caretPosition, String oldValue, String newValue) {
        String currentWord = getCurrentWord(newValue, caretPosition);
        if (!currentWord.isEmpty() && !currentWord.equals(lastQuery)) {
            showAutoComplete(currentWord);
            lastQuery = currentWord;
            autoCompleteStartIndex = caretPosition - currentWord.length();
            isAutoCompleteActive = true;
        }
    }

    @NotNull
    private String getCurrentWord(@NotNull String text, int caretPosition) {
        if (caretPosition <= 0 || caretPosition > text.length()) {
            return "";
        }
        int start = text.lastIndexOf('<', caretPosition - 1);
        if (start == -1 || start >= caretPosition) {
            return "";
        }
        return text.substring(start + 1, caretPosition);
    }

    private boolean isValidAutoCompletePosition(int caretPosition) {
        String text = promptTextArea.getText();
        if (text.isEmpty() || caretPosition <= 0 || caretPosition > text.length()) {
            return false;
        }

        int lastOpenBracket = text.lastIndexOf('<', caretPosition - 1);
        if (lastOpenBracket == -1) {
            return false;
        }

        int nextCloseBracket = text.indexOf('>', lastOpenBracket);
        if (nextCloseBracket != -1 && nextCloseBracket < caretPosition) {
            return false;
        }

        // 檢查是否在已完成的 embed 標籤內
        Matcher matcher = EMBED_PATTERN.matcher(text);
        while (matcher.find()) {
            if (matcher.start() < caretPosition && matcher.end() > caretPosition) {
                // 在已完成的 embed 標籤內，不觸發自動補全
                return false;
            }
        }

        // 檢查是否在未完成的 embed 標籤內
        matcher = Pattern.compile("<([^>]*)").matcher(text);
        while (matcher.find()) {
            if (matcher.start() < caretPosition && matcher.end() >= caretPosition) {
                return true;
            }
        }

        return text.charAt(caretPosition - 1) == '<';
    }

    private void handleKeyPress(@NotNull KeyEvent event) {
        if (event.getCode() == KeyCode.ESCAPE) {
            hideAutoComplete();
            event.consume();
            return;
        }

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
                case ENTER:
                case TAB:
                    selectAutoComplete();
                    event.consume();
                    break;
            }
        } else if (event.isControlDown() && event.getCode() == KeyCode.SPACE) {
            String currentWord = getCurrentWord(promptTextArea.getText(), promptTextArea.getCaretPosition());
            showAutoComplete(currentWord);
            event.consume();
        }
    }

    private void handlePopupKeyPress(@NotNull KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.TAB) {
            selectAutoComplete();
            event.consume();
        } else if (event.getCode() == KeyCode.ESCAPE) {
            hideAutoComplete();
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

    private void showAutoComplete(String query) {
        if (embedFileManager == null) {
            log.warn("EmbedFileManager is null");
            return;
        }
        CompletableFuture<List<EmbedFileManager.EmbedFile>> futureMatches = embedFileManager.getMatchingEmbedsAsync(query);
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
        lastQuery = "";
    }

    private void selectAutoComplete() {
        EmbedFileManager.EmbedFile selected = autoCompleteList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            insertAutoComplete(selected.fullPath());
            hideAutoComplete();
        }
    }

    private void insertAutoComplete(String selected) {
        if (autoCompleteStartIndex != -1) {
            String text = promptTextArea.getText();
            int caretPosition = promptTextArea.getCaretPosition();
            if (autoCompleteStartIndex <= text.length() && caretPosition <= text.length()) {
                if (selected.endsWith(".txt")) {
                    selected = selected.substring(0, selected.length() - 4);
                }
                String newText = text.substring(0, autoCompleteStartIndex) + selected + ":1>" + text.substring(caretPosition);
                promptTextArea.setText(newText);
                promptTextArea.positionCaret(autoCompleteStartIndex + selected.length() + 3);
            }
            autoCompleteStartIndex = -1;
        }
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