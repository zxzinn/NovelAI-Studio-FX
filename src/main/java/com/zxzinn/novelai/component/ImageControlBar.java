package com.zxzinn.novelai.component;

import com.zxzinn.novelai.utils.common.ResourcePaths;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;

public class ImageControlBar extends HBox {

    @FXML private Button zoomInButton;
    @FXML private Button zoomOutButton;
    @FXML private Button fitButton;
    @FXML private Button lockButton;
    @FXML private Button infoButton;
    @FXML private Button saveButton;

    public ImageControlBar() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(ResourcePaths.IMAGE_CONTROL_BAR_FXML));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        setupIcons();
    }

    private void setupIcons() {
        zoomInButton.setGraphic(new FontIcon("fas-search-plus"));
        zoomOutButton.setGraphic(new FontIcon("fas-search-minus"));
        fitButton.setGraphic(new FontIcon("fas-expand"));
        lockButton.setGraphic(new FontIcon("fas-lock"));
        infoButton.setGraphic(new FontIcon("fas-info-circle"));
        saveButton.setGraphic(new FontIcon("fas-save"));
    }

}