package com.zxzinn.novelai.test;

import com.zxzinn.novelai.utils.tokenizer.SimpleTokenizer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class TokenizerTest extends Application {

    private SimpleTokenizer tokenizer;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            tokenizer = new SimpleTokenizer("D:\\IdeaProjects\\NovelAIDesktopFX\\src\\main\\resources\\com\\zxzinn\\novelai\\tokenizers\\bpe_simple_vocab_16e6.txt.gz");
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        primaryStage.setTitle("SimpleTokenizer Test");

        TextArea inputArea = new TextArea();
        inputArea.setPromptText("輸入文本...");
        TextArea outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.setPromptText("向量結果將顯示在這裡...");

        // 添加監聽器以動態更新輸出
        inputArea.textProperty().addListener((observable, oldValue, newValue) -> {
            processInput(newValue, outputArea);
        });

        HBox textAreasBox = new HBox(10, inputArea, outputArea);
        HBox.setHgrow(inputArea, Priority.ALWAYS);
        HBox.setHgrow(outputArea, Priority.ALWAYS);

        VBox root = new VBox(10, textAreasBox);
        root.setPadding(new Insets(10));

        Scene scene = new Scene(root, 800, 400);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void processInput(String input, TextArea outputArea) {
        if (input.isEmpty()) {
            outputArea.clear();
            return;
        }
        List<Integer> tokens = tokenizer.encode(input);
        outputArea.setText(tokens.toString());
    }
}