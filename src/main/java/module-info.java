module com.zxzinn.novelai {
    requires javafx.controls;
    requires javafx.fxml;
    requires static lombok;
    requires okhttp3;
    requires com.google.gson;
    requires java.desktop;
    requires javafx.swing;
    requires org.apache.logging.log4j;
    requires org.yaml.snakeyaml;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome5;
    requires org.kordamp.ikonli.core;
    requires atlantafx.base;

    opens com.zxzinn.novelai to javafx.fxml;
    opens com.zxzinn.novelai.api to com.google.gson;
    exports com.zxzinn.novelai;
    exports com.zxzinn.novelai.api to com.google.gson;
    exports com.zxzinn.novelai.controller.generation;
    opens com.zxzinn.novelai.controller.generation to javafx.fxml;
    opens com.zxzinn.novelai.controller.generation.img2img to javafx.fxml;
    exports com.zxzinn.novelai.controller.generation.img2img;
    exports com.zxzinn.novelai.controller.generation.text2img;
    opens com.zxzinn.novelai.controller.generation.text2img to javafx.fxml;
    exports com.zxzinn.novelai.controller.filemanager;
    opens com.zxzinn.novelai.controller.filemanager to javafx.fxml;
    opens com.zxzinn.novelai.controller.ui to javafx.fxml;
    exports com.zxzinn.novelai.controller.ui;
    exports com.zxzinn.novelai.utils.common;
    opens com.zxzinn.novelai.utils.common to javafx.fxml;
}