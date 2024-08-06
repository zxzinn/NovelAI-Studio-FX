package com.zxzinn.novelai.ui;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;

public class TitleBarController extends HBox {
    private static final Logger logger = LogManager.getLogger(TitleBarController.class);
    private double xOffset = 0;
    private double yOffset = 0;
    private Stage stage;
    private Rectangle2D previousBounds;
    private static final long ANIMATION_DURATION = 20_000_000L; // 150ms in nanoseconds

    // 添加title屬性
    private StringProperty title = new SimpleStringProperty();

    public TitleBarController() {
        Label titleLabel = new Label();
        titleLabel.textProperty().bind(titleProperty());
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        Button minimizeButton = createButton("fas-window-minimize", this::minimizeStage);
        Button maximizeButton = createButton("fas-window-maximize", this::toggleMaximize);
        Button closeButton = createButton("fas-times", this::closeStage);

        this.getChildren().addAll(titleLabel, minimizeButton, maximizeButton, closeButton);
    }

    private Button createButton(String iconLiteral, Runnable action) {
        Button button = new Button();
        button.setGraphic(new FontIcon(iconLiteral));
        button.getStyleClass().add("window-button");
        button.setOnAction(e -> action.run());
        return button;
    }

    private void setupDraggable() {
        this.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        this.setOnMouseDragged(event -> {
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });
    }

    private void setupDoubleClickMaximize() {
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
                    logger.info("Animation completed in {} ms", Optional.of(elapsedTime / 1_000_000.0));
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
    // 添加setStage()方法
    public void setStage(Stage stage) {
        this.stage = stage;
        setupDraggable();
        setupDoubleClickMaximize();
    }

    private void closeStage() {
        stage.close();
        Platform.exit();
        System.exit(0);
    }

    // 添加title屬性的getter和setter方法
    public String getTitle() {
        return title.get();
    }

    public StringProperty titleProperty() {
        return title;
    }

    public void setTitle(String title) {
        this.title.set(title);
    }


}