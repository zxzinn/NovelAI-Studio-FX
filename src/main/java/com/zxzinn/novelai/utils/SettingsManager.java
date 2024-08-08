package com.zxzinn.novelai.utils;

import lombok.extern.log4j.Log4j2;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Log4j2
public class SettingsManager {
    private static final String SETTINGS_FILE = "novelai_settings.properties";
    private final Map<String, String> settingsCache;
    private final ReadWriteLock lock;
    private boolean isDirty = false;
    private final ScheduledExecutorService scheduler;

    private static class LazyHolder {
        static final SettingsManager INSTANCE = new SettingsManager();
    }

    public static SettingsManager getInstance() {
        return LazyHolder.INSTANCE;
    }

    private SettingsManager() {
        this.settingsCache = new ConcurrentHashMap<>();
        this.lock = new ReentrantReadWriteLock();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        loadSettings();
        scheduleAutoSave();
    }

    private void loadSettings() {
        lock.writeLock().lock();
        try {
            File file = new File(SETTINGS_FILE);
            if (file.exists()) {
                try (InputStream input = new FileInputStream(file)) {
                    Properties props = new Properties();
                    props.load(input);
                    for (String key : props.stringPropertyNames()) {
                        settingsCache.put(key, props.getProperty(key));
                    }
                    log.info("設定已從 {} 載入", SETTINGS_FILE);
                } catch (IOException e) {
                    log.error("無法載入設定檔：{}", SETTINGS_FILE, e);
                }
            } else {
                log.info("設定檔 {} 不存在，將使用預設值", SETTINGS_FILE);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void saveSettings() {
        lock.readLock().lock();
        try {
            if (isDirty) {
                Properties props = new Properties();
                props.putAll(settingsCache);
                try (OutputStream output = new FileOutputStream(SETTINGS_FILE)) {
                    props.store(output, "NovelAI Generator Settings");
                    isDirty = false;
                    log.info("設定已保存到 {}", SETTINGS_FILE);
                } catch (IOException e) {
                    log.error("無法保存設定檔：{}", SETTINGS_FILE, e);
                }
            }
        } finally {
            lock.readLock().unlock();
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
        lock.writeLock().lock();
        try {
            settingsCache.put(key, value);
            isDirty = true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public String getString(String key, String defaultValue) {
        lock.readLock().lock();
        try {
            return settingsCache.getOrDefault(key, defaultValue);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setInt(String key, int value) {
        setString(key, String.valueOf(value));
    }

    public int getInt(String key, int defaultValue) {
        return Integer.parseInt(getString(key, String.valueOf(defaultValue)));
    }

    public void setDouble(String key, double value) {
        setString(key, String.valueOf(value));
    }

    public double getDouble(String key, double defaultValue) {
        return Double.parseDouble(getString(key, String.valueOf(defaultValue)));
    }

    public void setBoolean(String key, boolean value) {
        setString(key, String.valueOf(value));
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return Boolean.parseBoolean(getString(key, String.valueOf(defaultValue)));
    }

    public void setStringList(String key, List<String> values) {
        setString(key, String.join(",", values));
    }

    public List<String> getStringList(String key, List<String> defaultValue) {
        String value = getString(key, null);
        if (value == null) {
            return defaultValue;
        }
        return Arrays.asList(value.split(","));
    }

    public void addToStringList(String key, String value) {
        lock.writeLock().lock();
        try {
            List<String> currentList = getStringList(key, new ArrayList<>());
            if (!currentList.contains(value)) {
                currentList.add(value);
                setStringList(key, currentList);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void removeFromStringList(String key, String value) {
        lock.writeLock().lock();
        try {
            List<String> currentList = getStringList(key, new ArrayList<>());
            if (currentList.remove(value)) {
                setStringList(key, currentList);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void clearStringList(String key) {
        lock.writeLock().lock();
        try {
            settingsCache.remove(key);
            isDirty = true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Set<String> getKeys() {
        lock.readLock().lock();
        try {
            return new HashSet<>(settingsCache.keySet());
        } finally {
            lock.readLock().unlock();
        }
    }

    public void remove(String key) {
        lock.writeLock().lock();
        try {
            settingsCache.remove(key);
            isDirty = true;
        } finally {
            lock.writeLock().unlock();
        }
    }
}