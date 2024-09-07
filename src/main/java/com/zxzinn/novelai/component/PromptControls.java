package com.zxzinn.novelai.component;

import com.zxzinn.novelai.viewmodel.PromptControlsViewModel;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

public class PromptControls extends VBox {

    private Button refreshButton;
    private Button lockButton;
    private FontIcon lockIcon;
    private final PromptControlsViewModel viewModel;

    public PromptControls() {
        this.viewModel = new PromptControlsViewModel();
        initializeComponents();
        setupBindings();
    }

    private void initializeComponents() {
        refreshButton = createButton("fas-sync-alt", "refresh-button");
        lockButton = createButton("fas-lock-open", "lock-button");
        lockIcon = (FontIcon) lockButton.getGraphic();

        getChildren().addAll(refreshButton, lockButton);
        setAlignment(javafx.geometry.Pos.CENTER);
        setSpacing(5.0);
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
        refreshButton.setOnAction(event -> viewModel.refresh());
        lockButton.setOnAction(event -> viewModel.toggleLock());

        viewModel.lockedProperty().addListener((observable, oldValue, newValue) -> {
            lockIcon.setIconLiteral(newValue ? "fas-lock" : "fas-lock-open");
            if (newValue) {
                lockButton.getStyleClass().add("locked");
            } else {
                lockButton.getStyleClass().remove("locked");
            }
        });
    }

    public void setOnRefreshAction(Runnable action) {
        viewModel.setRefreshAction(action);
    }

    public void setOnLockAction(Runnable action) {
        viewModel.setLockAction(action);
    }

    public void setLockState(boolean isLocked) {
        viewModel.lockedProperty().set(isLocked);
    }
}