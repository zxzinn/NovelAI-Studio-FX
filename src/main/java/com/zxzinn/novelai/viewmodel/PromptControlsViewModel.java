package com.zxzinn.novelai.viewmodel;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import lombok.Setter;

public class PromptControlsViewModel {

    private final BooleanProperty locked = new SimpleBooleanProperty(false);
    @Setter
    private Runnable refreshAction;
    @Setter
    private Runnable lockAction;

    public BooleanProperty lockedProperty() {
        return locked;
    }

    public void refresh() {
        if (refreshAction != null) {
            refreshAction.run();
        }
    }

    public void toggleLock() {
        locked.set(!locked.get());
        if (lockAction != null) {
            lockAction.run();
        }
    }
}