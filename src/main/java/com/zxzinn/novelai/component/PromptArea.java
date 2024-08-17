package com.zxzinn.novelai.component;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import lombok.Getter;

import java.io.IOException;

public class PromptArea extends VBox {

    @FXML private Label promptLabel;
    @Getter @FXML private TextArea promptTextArea;

    public PromptArea() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/zxzinn/novelai/PromptArea.fxml"));
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
        promptTextArea.setWrapText(true);
        promptTextArea.setMinHeight(100);
        promptTextArea.setPrefRowCount(5);
        promptTextArea.setMaxHeight(Double.MAX_VALUE);
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