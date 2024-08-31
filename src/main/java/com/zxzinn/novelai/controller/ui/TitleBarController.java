package com.zxzinn.novelai.controller.ui;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import java.util.Optional;

@Log4j2
public class TitleBarController extends HBox {
    private double xOffset = 0;
    private double yOffset = 0;
    @Setter private Stage stage;
    private Rectangle2D previousBounds;
    private static final long ANIMATION_DURATION = 20_000_000L;

    @FXML private Label titleLabel;
    @FXML private Button minimizeButton;
    @FXML private Button maximizeButton;
    @FXML private Button closeButton;

    private final StringProperty title = new SimpleStringProperty();

    @FXML
    private void initialize() {
        titleLabel.textProperty().bind(titleProperty());
        setupEventHandlers();
    }

    private void setupEventHandlers() {
        minimizeButton.setOnAction(event -> minimizeStage());
        maximizeButton.setOnAction(event -> toggleMaximize());
        closeButton.setOnAction(event -> closeStage());

        this.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        this.setOnMouseDragged(event -> {
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });
        this.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                toggleMaximize();
            }
        });
    }

    private void minimizeStage() {
        stage.setIconified(true);
    }

    private void toggleMaximize() {
        Scene scene = stage.getScene();
        if (scene == null) return;

        if (!stage.isMaximized()) {
            previousBounds = new Rectangle2D(stage.getX(), stage.getY(), scene.getWidth(), scene.getHeight());
            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            animateResize(screenBounds.getMinX(), screenBounds.getMinY(),
                    screenBounds.getWidth(), screenBounds.getHeight(), true);
        } else {
            animateResize(previousBounds.getMinX(), previousBounds.getMinY(),
                    previousBounds.getWidth(), previousBounds.getHeight(), false);
        }
    }

    private void animateResize(double toX, double toY, double toWidth, double toHeight, boolean maximize) {
        final double startX = stage.getX();
        final double startY = stage.getY();
        final double startWidth = stage.getWidth();
        final double startHeight = stage.getHeight();
        final long startTime = System.nanoTime();

        new AnimationTimer() {
            @Override
            public void handle(long now) {
                long elapsedTime = now - startTime;
                if (elapsedTime > ANIMATION_DURATION) {
                    stage.setX(toX);
                    stage.setY(toY);
                    stage.setWidth(toWidth);
                    stage.setHeight(toHeight);
                    stage.setMaximized(maximize);
                    this.stop();
                    log.info("Animation completed in {} ms", Optional.of(elapsedTime / 1_000_000.0));
                } else {
                    double fraction = (double) elapsedTime / ANIMATION_DURATION;
                    stage.setX(startX + (toX - startX) * fraction);
                    stage.setY(startY + (toY - startY) * fraction);
                    stage.setWidth(startWidth + (toWidth - startWidth) * fraction);
                    stage.setHeight(startHeight + (toHeight - startHeight) * fraction);
                }
            }
        }.start();
    }

    private void closeStage() {
        stage.close();
        Platform.exit();
        System.exit(0);
    }

    public StringProperty titleProperty() {
        return title;
    }
}