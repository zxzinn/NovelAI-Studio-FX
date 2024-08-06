package com.zxzinn.novelai.utils;

import lombok.extern.log4j.Log4j2;

import java.io.*;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Log4j2
public class SettingsManager {
    private static final String SETTINGS_FILE = "novelai_settings.properties";
    private final Properties properties;
    private boolean isDirty = false;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public SettingsManager() {
        properties = new Properties();
        loadSettings();
        scheduleAutoSave();
    }

    private void loadSettings() {
        File file = new File(SETTINGS_FILE);
        if (file.exists()) {
            try (InputStream input = new FileInputStream(file)) {
                properties.load(input);
                log.info("設定已從 {} 載入", SETTINGS_FILE);
            } catch (IOException e) {
                log.error("無法載入設定檔：{}", SETTINGS_FILE, e);
            }
        } else {
            log.info("設定檔 {} 不存在，將使用預設值", SETTINGS_FILE);
        }
    }

    public void saveSettings() {
        if (isDirty) {
            try (OutputStream output = new FileOutputStream(SETTINGS_FILE)) {
                properties.store(output, "NovelAI Generator Settings");
                isDirty = false;
                log.info("設定已保存到 {}", SETTINGS_FILE);
            } catch (IOException e) {
                log.error("無法保存設定檔：{}", SETTINGS_FILE, e);
            }
        }
    }

    private void scheduleAutoSave() {
        scheduler.scheduleAtFixedRate(this::saveSettings, 1, 1, TimeUnit.MINUTES);
    }

    public void shutdown() {
        scheduler.shutdown();
        saveSettings();
    }

    public void setString(String key, String value) {
        properties.setProperty(key, value);
        isDirty = true;
    }

    public String getString(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public void setInt(String key, int value) {
        properties.setProperty(key, String.valueOf(value));
        isDirty = true;
    }

    public int getInt(String key, int defaultValue) {
        return Integer.parseInt(properties.getProperty(key, String.valueOf(defaultValue)));
    }

    public void setDouble(String key, double value) {
        properties.setProperty(key, String.valueOf(value));
        isDirty = true;
    }

    public double getDouble(String key, double defaultValue) {
        return Double.parseDouble(properties.getProperty(key, String.valueOf(defaultValue)));
    }

    public void setBoolean(String key, boolean value) {
        properties.setProperty(key, String.valueOf(value));
        isDirty = true;
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return Boolean.parseBoolean(properties.getProperty(key, String.valueOf(defaultValue)));
    }
}