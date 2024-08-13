package com.zxzinn.novelai.component;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class NotificationPane extends StackPane {
    private final Rectangle background;
    private final Label messageLabel;
    private final Timeline showAnimation;
    private final Timeline hideAnimation;

    public NotificationPane() {
        // 創建背景
        background = new Rectangle();
        background.setArcWidth(10);
        background.setArcHeight(10);
        background.setFill(Color.rgb(60, 63, 65, 0.9));

        // 創建消息標籤
        messageLabel = new Label();
        messageLabel.setTextFill(Color.WHITE);

        // 設置佈局
        setAlignment(Pos.BOTTOM_CENTER);
        getChildren().addAll(background, messageLabel);

        // 初始化為不可見
        setVisible(false);
        setOpacity(0);

        // 創建顯示動畫
        showAnimation = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(opacityProperty(), 0)),
                new KeyFrame(Duration.millis(200), new KeyValue(opacityProperty(), 1))
        );

        // 創建隱藏動畫
        hideAnimation = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(opacityProperty(), 1)),
                new KeyFrame(Duration.millis(200), new KeyValue(opacityProperty(), 0))
        );
        hideAnimation.setOnFinished(event -> setVisible(false));
    }

    public void showNotification(String message, Duration duration) {
        messageLabel.setText(message);
        background.setWidth(messageLabel.getWidth() + 20);
        background.setHeight(messageLabel.getHeight() + 10);

        setVisible(true);
        showAnimation.play();

        Timeline hideTimer = new Timeline(new KeyFrame(duration, event -> hideAnimation.play()));
        hideTimer.play();
    }
}