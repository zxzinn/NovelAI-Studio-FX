package com.zxzinn.novelai.component;

import com.zxzinn.novelai.utils.common.ResourcePaths;
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
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(ResourcePaths.PROMPT_CONTROLS));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
            initializeButtons();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    private void initializeButtons() {
        refreshButton.setPrefSize(30, 30);
        lockButton.setPrefSize(30, 30);
    }

    public void setOnRefreshAction(Runnable action) {
        refreshButton.setOnAction(event -> action.run());
    }

    public void setOnLockAction(Runnable action) {
        lockButton.setOnAction(event -> action.run());
    }

    public void setLockState(boolean isLocked) {
        lockIcon.setIconLiteral(isLocked ? "fas-lock" : "fas-lock-open");
        if (isLocked) {
            lockButton.getStyleClass().add("locked");
        } else {
            lockButton.getStyleClass().remove("locked");
        }
    }
}