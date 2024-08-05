module com.zxzinn.novelaidesktopfx {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.zxzinn.novelaidesktopfx to javafx.fxml;
    exports com.zxzinn.novelaidesktopfx;
}