package com.zxzinn.novelai.component;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;

public class PromptControls extends VBox {

    @FXML private Button refreshButton;
    @FXML private Button lockButton;
    @FXML private FontIcon lockIcon;

    public PromptControls() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/zxzinn/novelai/PromptControls.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    public void setOnRefreshAction(Runnable action) {
        refreshButton.setOnAction(event -> action.run());
    }

    public void setOnLockAction(Runnable action) {
        lockButton.setOnAction(event -> action.run());
    }

    public void setLockIcon(boolean isLocked) {
        lockIcon.setIconLiteral(isLocked ? "fas-lock" : "fas-lock-open");
    }
}