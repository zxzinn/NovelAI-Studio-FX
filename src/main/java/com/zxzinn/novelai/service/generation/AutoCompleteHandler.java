package com.zxzinn.novelai.service.generation;

import com.google.inject.Inject;
import com.zxzinn.novelai.utils.embed.EmbedFileManager;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Popup;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.text.similarity.LevenshteinDistance;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Log4j2
public class AutoCompleteHandler {
    private final TextArea promptTextArea;
    private final ListView<EmbedFileManager.EmbedFile> autoCompleteList;
    private final Popup autoCompletePopup;
    private final Popup previewPopup;
    @Setter private EmbedFileManager embedFileManager;
    private int autoCompleteStartIndex = -1;

    private static final Pattern EMBED_PATTERN = Pattern.compile("<([^>:]+)(?::([^>]+))?>", Pattern.DOTALL);
    private static final int MAX_LEVENSHTEIN_DISTANCE = 2;

    @Inject
    public AutoCompleteHandler(TextArea promptTextArea) {
        this.promptTextArea = promptTextArea;
        this.autoCompleteList = setupAutoCompleteList();
        this.autoCompletePopup = setupAutoCompletePopup();
        this.previewPopup = setupPreviewPopup();
        setupListeners();
    }

    private ListView<EmbedFileManager.EmbedFile> setupAutoCompleteList() {
        ListView<EmbedFileManager.EmbedFile> list = new ListView<>();
        list.setPrefWidth(400);
        list.setPrefHeight(300);
        list.getStyleClass().add("auto-complete-list");
        list.setCellFactory(listView -> new ListCell<>() {
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
        return list;
    }

    private Popup setupAutoCompletePopup() {
        Popup popup = new Popup();
        popup.getContent().add(autoCompleteList);
        popup.setAutoHide(true);
        return popup;
    }

    private Popup setupPreviewPopup() {
        Popup popup = new Popup();
        popup.setAutoHide(true);
        return popup;
    }

    private void setupListeners() {
        autoCompleteList.setOnMouseClicked(event -> selectAutoComplete());
        autoCompletePopup.addEventFilter(KeyEvent.KEY_PRESSED, this::handlePopupKeyPress);
        autoCompleteList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                showPreview(newValue);
            }
        });
    }

    public void handleTextChange(String oldValue, String newValue) {
        int caretPosition = promptTextArea.getCaretPosition();
        if (caretPosition > 0 && caretPosition <= newValue.length()) {
            if (shouldTriggerAutoComplete(newValue, caretPosition)) {
                updateAutoComplete(caretPosition, newValue);
            } else {
                hideAutoComplete();
            }
        }
    }

    public void handleCaretChange(int newPosition) {
        String text = promptTextArea.getText();
        if (shouldTriggerAutoComplete(text, newPosition)) {
            updateAutoComplete(newPosition, text);
        } else {
            hideAutoComplete();
        }
    }

    private boolean shouldTriggerAutoComplete(String text, int caretPosition) {
        if (caretPosition <= 0 || caretPosition > text.length()) {
            return false;
        }

        // 檢查是否在未完成的標籤內
        int lastOpenBracket = text.lastIndexOf('<', caretPosition - 1);
        int lastCloseBracket = text.lastIndexOf('>', caretPosition - 1);

        if (lastOpenBracket == -1 || lastOpenBracket < lastCloseBracket) {
            return false;
        }

        String currentTag = text.substring(lastOpenBracket, caretPosition);
        return !currentTag.contains(":");
    }

    private void updateAutoComplete(int caretPosition, String text) {
        String currentWord = getCurrentWord(text, caretPosition);
        if (!currentWord.isEmpty()) {
            showAutoComplete(currentWord);
            autoCompleteStartIndex = caretPosition - currentWord.length();
        } else {
            hideAutoComplete();
        }
    }

    private String getCurrentWord(String text, int caretPosition) {
        int start = text.lastIndexOf('<', caretPosition - 1);
        if (start == -1 || start >= caretPosition) {
            return "";
        }
        String currentTag = text.substring(start, caretPosition);
        int colonIndex = currentTag.indexOf(':');
        if (colonIndex != -1) {
            return "";
        }
        return currentTag.substring(1); // 去掉開頭的 '<'
    }

    public void handleKeyPress(KeyEvent event) {
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

    private void handlePopupKeyPress(KeyEvent event) {
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
            List<EmbedFileManager.EmbedFile> filteredMatches = filterAndSortMatches(query, matches);
            if (!filteredMatches.isEmpty()) {
                Platform.runLater(() -> {
                    autoCompleteList.getItems().setAll(filteredMatches);
                    autoCompleteList.getSelectionModel().selectFirst();
                    positionAutoCompletePopup();
                    autoCompletePopup.show(promptTextArea, autoCompletePopup.getX(), autoCompletePopup.getY());
                    showPreview(filteredMatches.getFirst());
                });
                log.info("Showing autocomplete popup with " + filteredMatches.size() + " matches");
            } else {
                hideAutoComplete();
            }
        }).exceptionally(ex -> {
            log.error("Error fetching autocomplete matches: {}", ex.getMessage());
            return null;
        });
    }

    private List<EmbedFileManager.EmbedFile> filterAndSortMatches(String query, List<EmbedFileManager.EmbedFile> matches) {
        LevenshteinDistance levenshteinDistance = new LevenshteinDistance(MAX_LEVENSHTEIN_DISTANCE);
        return matches.stream()
                .filter(file -> levenshteinDistance.apply(query, file.fileName()) <= MAX_LEVENSHTEIN_DISTANCE)
                .sorted(Comparator.comparingInt(file -> levenshteinDistance.apply(query, file.fileName())))
                .collect(Collectors.toList());
    }

    private void positionAutoCompletePopup() {
        Bounds bounds = promptTextArea.localToScreen(promptTextArea.getBoundsInLocal());
        autoCompletePopup.setX(bounds.getMinX());
        autoCompletePopup.setY(bounds.getMaxY());
    }

    public void hideAutoComplete() {
        Platform.runLater(() -> {
            autoCompletePopup.hide();
            previewPopup.hide();
            promptTextArea.requestFocus();
        });
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

    private void showPreview(EmbedFileManager.EmbedFile file) {
        Platform.runLater(() -> {
            Label previewLabel = new Label(file.fullPath());
            previewLabel.setStyle("-fx-background-color: white; -fx-padding: 5px; -fx-border-color: gray; -fx-border-width: 1px;");

            previewPopup.getContent().clear();
            previewPopup.getContent().add(previewLabel);

            Bounds bounds = autoCompleteList.localToScreen(autoCompleteList.getBoundsInLocal());
            previewPopup.setX(bounds.getMaxX() + 10);
            previewPopup.setY(bounds.getMinY());

            previewPopup.show(autoCompleteList.getScene().getWindow());
        });
    }
}