package com.zxzinn.novelai.service.ui;

import com.zxzinn.novelai.component.NotificationPane;
import javafx.application.Platform;
import javafx.util.Duration;

public class NotificationService {
    private static NotificationPane notificationPane;

    public static void initialize(NotificationPane pane) {
        notificationPane = pane;
    }

    public static void showNotification(String message) {
        Platform.runLater(() -> {
            if (notificationPane != null) {
                notificationPane.showNotification(message, Duration.seconds(3));
            }
        });
    }
}