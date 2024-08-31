package com.zxzinn.novelai.utils.ui;

import javafx.application.Platform;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class LoadingManager {
    private final List<LoadingTask> tasks;
    private final List<LoadingObserver> observers;
    private int currentTaskIndex;
    private double progress;

    public LoadingManager() {
        this.tasks = new ArrayList<>();
        this.observers = new ArrayList<>();
        this.currentTaskIndex = 0;
        this.progress = 0;
    }

    public void addTask(LoadingTask task) {
        tasks.add(task);
    }

    public void addObserver(LoadingObserver observer) {
        observers.add(observer);
    }

    public void start() {
        new Thread(this::executeTasks).start();
    }

    private void executeTasks() {
        for (int i = 0; i < tasks.size(); i++) {
            LoadingTask task = tasks.get(i);
            updateProgress(i, task.getDescription());
            executeTaskOnFXThread(task);
            currentTaskIndex++;
        }
        updateProgress(tasks.size(), "加載完成");
    }

    private void executeTaskOnFXThread(LoadingTask task) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                task.execute();
                future.complete(null);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        try {
            future.get(); // 等待任务完成
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateProgress(int taskIndex, String message) {
        progress = (double) taskIndex / tasks.size();
        Platform.runLater(() -> {
            for (LoadingObserver observer : observers) {
                observer.onProgressUpdate(progress, message);
            }
        });
    }

    public interface LoadingObserver {
        void onProgressUpdate(double progress, String message);
    }
}