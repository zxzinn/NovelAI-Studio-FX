module com.zxzinn.novelaidesktopfx {
    requires javafx.controls;
    requires javafx.fxml;
    requires static lombok;
    requires okhttp3;
    requires com.google.gson;
    requires java.desktop;
    requires javafx.swing;
    requires org.apache.logging.log4j;
    requires org.yaml.snakeyaml;

    opens com.zxzinn.novelai to javafx.fxml;
    opens com.zxzinn.novelai.api to com.google.gson;
    exports com.zxzinn.novelai;
    exports com.zxzinn.novelai.controller;
    opens com.zxzinn.novelai.controller to javafx.fxml;
    exports com.zxzinn.novelai.api to com.google.gson;
}