package com.zxzinn.novelai.model;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class TaskInfo {
    private final StringProperty id = new SimpleStringProperty();
    private final StringProperty status = new SimpleStringProperty();
    private final DoubleProperty progress = new SimpleDoubleProperty();
    private final StringProperty message = new SimpleStringProperty();

    public TaskInfo(String id, String status, double progress, String message) {
        setId(id);
        setStatus(status);
        setProgress(progress);
        setMessage(message);
    }

    public String getId() { return id.get(); }
    public void setId(String value) { id.set(value); }
    public StringProperty idProperty() { return id; }

    public String getStatus() { return status.get(); }
    public void setStatus(String value) { status.set(value); }
    public StringProperty statusProperty() { return status; }

    public double getProgress() { return progress.get(); }
    public void setProgress(double value) { progress.set(value); }
    public DoubleProperty progressProperty() { return progress; }

    public String getMessage() { return message.get(); }
    public void setMessage(String value) { message.set(value); }
    public StringProperty messageProperty() { return message; }
}