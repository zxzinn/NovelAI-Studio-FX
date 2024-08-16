package com.zxzinn.novelai.component;

import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public class TokenCountBar extends VBox {
    private final ProgressBar progressBar;
    private final Label countLabel;
    private static final int MAX_TOKENS = 225;

    public TokenCountBar() {
        progressBar = new ProgressBar(0);
        countLabel = new Label("0 / " + MAX_TOKENS + " tokens");

        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setStyle("-fx-accent: green;");

        getChildren().addAll(progressBar, countLabel);
        setSpacing(5);
    }

    public void updateTokenCount(int count) {
        double progress = (double) count / MAX_TOKENS;
        progressBar.setProgress(progress);
        countLabel.setText(count + " / " + MAX_TOKENS + " tokens");

        if (progress < 0.7) {
            progressBar.setStyle("-fx-accent: green;");
        } else if (progress < 0.9) {
            progressBar.setStyle("-fx-accent: orange;");
        } else {
            progressBar.setStyle("-fx-accent: red;");
        }
    }
}