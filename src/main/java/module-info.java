module com.zxzinn.novelaidesktopfx {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.zxzinn.novelai to javafx.fxml;
    exports com.zxzinn.novelai;
    exports com.zxzinn.novelai.controller;
    opens com.zxzinn.novelai.controller to javafx.fxml;
}