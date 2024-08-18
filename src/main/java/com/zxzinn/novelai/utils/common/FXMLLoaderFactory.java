package com.zxzinn.novelai.utils;

import javafx.fxml.FXMLLoader;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.net.URL;

@Log4j2
public final class FXMLLoaderFactory {

    private FXMLLoaderFactory() {
        // 私有構造函數防止實例化
    }

    public static FXMLLoader createLoader(String fxmlPath) {
        URL resource = FXMLLoaderFactory.class.getResource(fxmlPath);
        if (resource == null) {
            log.error("無法找到 FXML 文件: {}", fxmlPath);
            throw new RuntimeException("FXML 資源不存在: " + fxmlPath);
        }
        return new FXMLLoader(resource);
    }

    public static <T> T loadFXML(String fxmlPath) {
        try {
            return createLoader(fxmlPath).load();
        } catch (IOException e) {
            log.error("加載 FXML 文件時發生錯誤: {}", fxmlPath, e);
            throw new RuntimeException("無法加載 FXML: " + fxmlPath, e);
        }
    }
}