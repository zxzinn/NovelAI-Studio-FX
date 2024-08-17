package com.zxzinn.novelai.component;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import lombok.Getter;

import java.io.IOException;

public class PromptPreviewArea extends VBox {

    @FXML
    private Label previewLabel;

    @Getter
    @FXML
    private TextArea previewTextArea;

    public PromptPreviewArea() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/zxzinn/novelai/PromptPreviewArea.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
            initializeTextArea();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    private void initializeTextArea() {
        previewTextArea.setWrapText(true);
        previewTextArea.setMinHeight(100);
        previewTextArea.setPrefRowCount(5);
        previewTextArea.setMaxHeight(Double.MAX_VALUE);
        previewTextArea.setEditable(false);
    }

    public void setPreviewLabel(String label) {
        previewLabel.setText(label);
    }

    public String getPreviewText() {
        return previewTextArea.getText();
    }

    public void setPreviewText(String text) {
        previewTextArea.setText(text);
    }
}