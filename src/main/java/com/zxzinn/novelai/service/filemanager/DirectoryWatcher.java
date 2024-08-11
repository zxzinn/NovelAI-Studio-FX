package com.zxzinn.novelai.service.filemanager;

import lombok.extern.log4j.Log4j2;

import java.nio.file.*;
import java.util.Map;
import java.util.function.BiConsumer;

@Log4j2
public class DirectoryWatcher {
    private final WatchService watchService;
    private final Map<WatchKey, Path> watchKeyToPath;
    private final BiConsumer<Path, WatchEvent.Kind<?>> changeListener;

    public DirectoryWatcher(WatchService watchService, Map<WatchKey, Path> watchKeyToPath,
                            BiConsumer<Path, WatchEvent.Kind<?>> changeListener) {
        this.watchService = watchService;
        this.watchKeyToPath = watchKeyToPath;
        this.changeListener = changeListener;
    }

    public void processEvents() {
        WatchKey key;
        while ((key = watchService.poll()) != null) {
            Path dir = watchKeyToPath.get(key);
            if (dir == null) {
                continue;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();
                if (kind == StandardWatchEventKinds.OVERFLOW) {
                    continue;
                }

                @SuppressWarnings("unchecked")
                WatchEvent<Path> ev = (WatchEvent<Path>) event;
                Path fileName = ev.context();
                Path fullPath = dir.resolve(fileName);

                changeListener.accept(fullPath, kind);
            }

            boolean valid = key.reset();
            if (!valid) {
                watchKeyToPath.remove(key);
                if (watchKeyToPath.isEmpty()) {
                    break;
                }
            }
        }
    }
}