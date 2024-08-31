package com.zxzinn.novelai.utils.common;

import lombok.extern.log4j.Log4j2;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Log4j2
public class PropertiesManager {
    private static final String PROPERTIES_FILE = CommonPaths.PROPERTIES_FILE;
    private final Map<String, String> propertiesCache;
    private final ReadWriteLock lock;
    private boolean isDirty = false;
    private final ScheduledExecutorService scheduler;

    private static class LazyHolder {
        static final PropertiesManager INSTANCE = new PropertiesManager();
    }

    public static PropertiesManager getInstance() {
        return LazyHolder.INSTANCE;
    }

    private PropertiesManager() {
        this.propertiesCache = new ConcurrentHashMap<>();
        this.lock = new ReentrantReadWriteLock();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        loadSettings();
        scheduleAutoSave();
    }

    private void loadSettings() {
        lock.writeLock().lock();
        try {
            File file = new File(PROPERTIES_FILE);
            if (file.exists()) {
                try (InputStream input = new FileInputStream(file)) {
                    Properties props = new Properties();
                    props.load(input);
                    for (String key : props.stringPropertyNames()) {
                        propertiesCache.put(key, props.getProperty(key));
                    }
                    log.info("設定已從 {} 載入", PROPERTIES_FILE);
                } catch (IOException e) {
                    log.error("無法載入設定檔：{}", PROPERTIES_FILE, e);
                }
            } else {
                log.info("設定檔 {} 不存在，將使用預設值", PROPERTIES_FILE);
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
                props.putAll(propertiesCache);
                try (OutputStream output = new FileOutputStream(PROPERTIES_FILE)) {
                    props.store(output, "NovelAI Generator Settings");
                    isDirty = false;
                    log.info("設定已保存到 {}", PROPERTIES_FILE);
                } catch (IOException e) {
                    log.error("無法保存設定檔：{}", PROPERTIES_FILE, e);
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
            propertiesCache.put(key, value);
            isDirty = true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public String getString(String key, String defaultValue) {
        lock.readLock().lock();
        try {
            return propertiesCache.getOrDefault(key, defaultValue);
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

    public void setLong(String key, long value) {
        setString(key, String.valueOf(value));
    }

    public long getLong(String key, long defaultValue) {
        return Long.parseLong(getString(key, String.valueOf(defaultValue)));
    }

    public void setStringList(String key, List<String> values) {
        setString(key, String.join(",", values));
    }

    public List<String> getStringList(String key, List<String> defaultValue) {
        String value = getString(key, null);
        if (value == null) {
            return new ArrayList<>(defaultValue);
        }
        return new ArrayList<>(Arrays.asList(value.split(",")));
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
            Path valuePath = Paths.get(value);
            boolean removed = currentList.removeIf(item -> {
                Path itemPath = Paths.get(item);
                return itemPath.getFileName().equals(valuePath.getFileName());
            });
            if (removed) {
                setStringList(key, currentList);
                log.info("從 {} 列表中移除了項目: {}", key, value);
            } else {
                log.warn("嘗試從 {} 列表中移除不存在的項目: {}", key, value);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

}