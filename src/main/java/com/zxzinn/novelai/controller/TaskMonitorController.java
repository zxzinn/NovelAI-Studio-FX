package com.zxzinn.novelai.controller;

import com.zxzinn.novelai.model.TaskInfo;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.ProgressBarTableCell;
import javafx.scene.control.cell.PropertyValueFactory;

public class TaskMonitorController {

    @FXML private TableView<TaskInfo> taskTable;
    @FXML private TableColumn<TaskInfo, String> idColumn;
    @FXML private TableColumn<TaskInfo, String> statusColumn;
    @FXML private TableColumn<TaskInfo, Double> progressColumn;
    @FXML private TableColumn<TaskInfo, String> messageColumn;

    private ObservableList<TaskInfo> tasks = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        progressColumn.setCellValueFactory(new PropertyValueFactory<>("progress"));
        messageColumn.setCellValueFactory(new PropertyValueFactory<>("message"));

        progressColumn.setCellFactory(ProgressBarTableCell.forTableColumn());

        taskTable.setItems(tasks);
    }

    public void addTask(TaskInfo task) {
        tasks.add(task);
    }

    public void updateTask(String id, String status, double progress, String message) {
        for (TaskInfo task : tasks) {
            if (task.getId().equals(id)) {
                task.setStatus(status);
                task.setProgress(progress);
                task.setMessage(message);
                break;
            }
        }
    }

    @FXML
    private void clearCompletedTasks() {
        tasks.removeIf(task -> task.getStatus().equals("完成"));
    }
}