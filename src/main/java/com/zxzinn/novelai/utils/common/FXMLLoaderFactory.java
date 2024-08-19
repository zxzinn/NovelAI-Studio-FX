package com.zxzinn.novelai.utils.common;

import javafx.fxml.FXMLLoader;
import lombok.extern.log4j.Log4j2;

import java.net.URL;

@Log4j2
public final class FXMLLoaderFactory {

    private FXMLLoaderFactory() {
    }

    public static FXMLLoader createLoader(String fxmlPath) {
        URL resource = FXMLLoaderFactory.class.getResource(fxmlPath);
        if (resource == null) {
            log.error("無法找到 FXML 文件: {}", fxmlPath);
            throw new RuntimeException("FXML 資源不存在: " + fxmlPath);
        }
        return new FXMLLoader(resource);
    }

}