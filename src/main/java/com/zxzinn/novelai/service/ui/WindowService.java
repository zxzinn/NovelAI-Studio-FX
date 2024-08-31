package com.zxzinn.novelai.service.ui;

import com.google.inject.Inject;
import com.zxzinn.novelai.utils.common.PropertiesManager;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;

public class WindowService {
    @Setter
    private Stage stage;
    private final PropertiesManager propertiesManager;
    private double xOffset = 0;
    private double yOffset = 0;
    private Rectangle2D restoreBounds;
    private boolean isMaximized = false;

    @Inject
    public WindowService(PropertiesManager propertiesManager) {
        this.propertiesManager = propertiesManager;
    }

    public void setupStage(Stage stage) {
        this.stage = stage;
        loadWindowSettings();
        setupResizeableWindow();
    }

    public void setupDraggableWindow(@NotNull VBox titleBar) {
        titleBar.setOnMousePressed(this::handleMousePressed);
        titleBar.setOnMouseDragged(this::handleMouseDragged);
        titleBar.setOnMouseReleased(this::handleMouseReleased);
    }

    private void handleMousePressed(@NotNull MouseEvent event) {
        xOffset = event.getSceneX();
        yOffset = event.getSceneY();
    }

    private void handleMouseReleased(@NotNull MouseEvent event) {
        if (event.getScreenY() <= 0) {
            toggleMaximize();
        }
    }

    private void handleMouseDragged(MouseEvent event) {
        if (!isMaximized) {
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        } else {
            double percentX = event.getScreenX() / Screen.getPrimary().getVisualBounds().getWidth();
            restoreBounds = new Rectangle2D(
                    event.getScreenX() - stage.getWidth() * percentX,
                    event.getScreenY(),
                    restoreBounds.getWidth(),
                    restoreBounds.getHeight());
            toggleMaximize();

            xOffset = stage.getWidth() * percentX;
            yOffset = event.getSceneY();
        }
    }

    public void setupResizeableWindow() {
        Cursor defaultCursor = Cursor.DEFAULT;
        Cursor resizeCursor = Cursor.SE_RESIZE;
        AtomicBoolean resizing = new AtomicBoolean(false);

        ResizeAnimationTimer resizeAnimationTimer = new ResizeAnimationTimer();

        stage.getScene().setOnMouseMoved(event -> {
            if (isInResizeZone(event)) {
                stage.getScene().setCursor(resizeCursor);
            } else {
                stage.getScene().setCursor(defaultCursor);
            }
        });

        stage.getScene().setOnMousePressed(event -> {
            if (isInResizeZone(event)) {
                resizing.set(true);
                event.consume();
            }
        });

        stage.getScene().setOnMouseDragged(event -> {
            if (resizing.get()) {
                double newWidth = Math.max(stage.getMinWidth(), event.getSceneX());
                double newHeight = Math.max(stage.getMinHeight(), event.getSceneY());

                resizeAnimationTimer.stop();
                resizeAnimationTimer.setTarget(newWidth, newHeight);
                resizeAnimationTimer.start();

                event.consume();
            }
        });

        stage.getScene().setOnMouseReleased(event -> {
            resizing.set(false);
            stage.getScene().setCursor(defaultCursor);
        });
    }

    private boolean isInResizeZone(@NotNull MouseEvent event) {
        return event.getSceneX() > stage.getWidth() - 20 && event.getSceneY() > stage.getHeight() - 20;
    }

    public void minimizeWindow() {
        stage.setIconified(true);
    }

    private void loadWindowSettings() {
        double width = propertiesManager.getDouble("window.width", 1024);
        double height = propertiesManager.getDouble("window.height", 768);
        double x = propertiesManager.getDouble("window.x", -1);
        double y = propertiesManager.getDouble("window.y", -1);
        boolean maximized = propertiesManager.getBoolean("window.maximized", false);

        if (x >= 0 && y >= 0) {
            stage.setX(x);
            stage.setY(y);
        } else {
            stage.centerOnScreen();
        }

        stage.setWidth(width);
        stage.setHeight(height);

        if (maximized) {
            toggleMaximize();
        }
    }

    public void saveWindowSettings() {
        if (!isMaximized) {
            propertiesManager.setDouble("window.width", stage.getWidth());
            propertiesManager.setDouble("window.height", stage.getHeight());
            propertiesManager.setDouble("window.x", stage.getX());
            propertiesManager.setDouble("window.y", stage.getY());
        }
        propertiesManager.setBoolean("window.maximized", isMaximized);
    }

    public void toggleMaximize() {
        if (!isMaximized) {
            restoreBounds = new Rectangle2D(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
            Screen screen = Screen.getScreensForRectangle(stage.getX(), stage.getY(), 1, 1).get(0);
            Rectangle2D bounds = screen.getVisualBounds();
            stage.setX(bounds.getMinX());
            stage.setY(bounds.getMinY());
            stage.setWidth(bounds.getWidth());
            stage.setHeight(bounds.getHeight());
            isMaximized = true;
        } else {
            stage.setX(restoreBounds.getMinX());
            stage.setY(restoreBounds.getMinY());
            stage.setWidth(restoreBounds.getWidth());
            stage.setHeight(restoreBounds.getHeight());
            isMaximized = false;
        }
    }


    public void closeWindow() {
        propertiesManager.shutdown();
        stage.close();
        Platform.exit();
        System.exit(0);
    }

    private class ResizeAnimationTimer extends AnimationTimer {
        private double targetWidth;
        private double targetHeight;

        @Override
        public void handle(long now) {
            double currentWidth = stage.getWidth();
            double currentHeight = stage.getHeight();

            stage.setWidth(currentWidth + (targetWidth - currentWidth) * 0.2);
            stage.setHeight(currentHeight + (targetHeight - currentHeight) * 0.2);

            if (Math.abs(stage.getWidth() - targetWidth) < 0.1 &&
                    Math.abs(stage.getHeight() - targetHeight) < 0.1) {
                stop();
            }
        }

        public void setTarget(double width, double height) {
            this.targetWidth = width;
            this.targetHeight = height;
        }
    }
}
