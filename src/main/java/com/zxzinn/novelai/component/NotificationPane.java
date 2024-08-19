package com.zxzinn.novelai.component;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class NotificationPane extends StackPane {
    private final Rectangle background;
    private final Label messageLabel;
    private final Timeline showAnimation;
    private final Timeline hideAnimation;

    public NotificationPane() {
        background = new Rectangle();
        background.setArcWidth(20);
        background.setArcHeight(20);

        Stop[] stops = new Stop[] {
                new Stop(0, Color.rgb(66, 165, 245, 0.9)),
                new Stop(1, Color.rgb(21, 101, 192, 0.9))
        };
        LinearGradient gradient = new LinearGradient(0, 0, 1, 0, true, javafx.scene.paint.CycleMethod.NO_CYCLE, stops);
        background.setFill(gradient);

        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.3));
        shadow.setRadius(10);
        shadow.setOffsetY(3);
        background.setEffect(shadow);

        messageLabel = new Label();
        messageLabel.setTextFill(Color.WHITE);
        messageLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10px 20px;");

        setAlignment(Pos.TOP_CENTER);
        getChildren().addAll(background, messageLabel);

        setVisible(false);
        setOpacity(0);
        setTranslateY(-50);

        setMouseTransparent(true);

        showAnimation = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(opacityProperty(), 0),
                        new KeyValue(translateYProperty(), -50)
                ),
                new KeyFrame(Duration.millis(300),
                        new KeyValue(opacityProperty(), 1, Interpolator.EASE_OUT),
                        new KeyValue(translateYProperty(), 20, Interpolator.EASE_OUT)
                )
        );

        hideAnimation = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(opacityProperty(), 1),
                        new KeyValue(translateYProperty(), 20)
                ),
                new KeyFrame(Duration.millis(300),
                        new KeyValue(opacityProperty(), 0, Interpolator.EASE_IN),
                        new KeyValue(translateYProperty(), -50, Interpolator.EASE_IN)
                )
        );
        hideAnimation.setOnFinished(event -> setVisible(false));
    }

    public void showNotification(String message, Duration duration) {
        messageLabel.setText(message);

        Platform.runLater(() -> {
            messageLabel.applyCss();
            messageLabel.layout();

            background.setWidth(messageLabel.getWidth() + 40);
            background.setHeight(messageLabel.getHeight() + 20);

            this.applyCss();
            this.layout();

            setVisible(true);
            showAnimation.play();
        });

        PauseTransition pause = new PauseTransition(duration);
        pause.setOnFinished(event -> hideAnimation.play());
        pause.play();
    }
}