package com.zxzinn.novelai.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zxzinn.novelai.utils.common.PropertiesManager;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.digest.DigestUtils;
import org.kohsuke.github.GHAsset;
import org.kohsuke.github.GHRelease;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.semver4j.Semver;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.concurrent.*;

@Log4j2
@Singleton
public class VersionCheckService {

    private static final String REPO_OWNER = "zxzinn";
    private static final String REPO_NAME = "NovelAI-Studio-FX";
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_SECONDS = 5;

    private final PropertiesManager propertiesManager;
    private final ScheduledExecutorService executorService;
    private final String currentJarName;

    @Inject
    public VersionCheckService(PropertiesManager propertiesManager) {
        this.propertiesManager = propertiesManager;
        this.executorService = Executors.newScheduledThreadPool(1);
        this.currentJarName = getCurrentJarName();
    }

    private String getCurrentJarName() {
        try {
            String path = VersionCheckService.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            path = path.replace("/", "\\");
            return Paths.get(path).getFileName().toString();
        } catch (Exception e) {
            log.error("無法獲取當前 JAR 文件名", e);
            return "NovelAI-Studio-FX.jar"; // 默認名稱
        }
    }

    public void checkForUpdates() {
        CompletableFuture.runAsync(this::performUpdateCheck, executorService)
                .exceptionally(e -> {
                    log.error("檢查更新時發生錯誤", e);
                    return null;
                });
    }

    private void performUpdateCheck() {
        try {
            String currentVersion = getCurrentVersion();
            String latestVersion = getLatestVersionWithRetry();

            if (isUpdateAvailable(currentVersion, latestVersion)) {
                promptForUpdate(latestVersion);
            } else {
                log.info("當前版本已是最新版本。");
            }
        } catch (Exception e) {
            log.error("檢查更新時發生錯誤", e);
        }
    }

    private String getCurrentVersion() {
        return propertiesManager.getString("app.version", "0.0.0");
    }

    private String getLatestVersionWithRetry() throws IOException {
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                return getLatestVersion();
            } catch (IOException e) {
                if (attempt == MAX_RETRIES - 1) {
                    throw e;
                }
                log.warn("獲取最新版本失敗，將在 {} 秒後重試", RETRY_DELAY_SECONDS);
                try {
                    Thread.sleep(RETRY_DELAY_SECONDS * 1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("獲取最新版本被中斷", ie);
                }
            }
        }
        throw new IOException("無法獲取最新版本");
    }

    private String getLatestVersion() throws IOException {
        GitHub github = GitHub.connectAnonymously();
        GHRepository repo = github.getRepository(REPO_OWNER + "/" + REPO_NAME);
        GHRelease latestRelease = repo.getLatestRelease();
        return latestRelease.getTagName().replaceFirst("v", "");
    }

    private boolean isUpdateAvailable(String currentVersion, String latestVersion) {
        Semver current = new Semver(currentVersion);
        Semver latest = new Semver(latestVersion);
        return latest.isGreaterThan(current);
    }

    private void promptForUpdate(String latestVersion) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("更新可用");
            alert.setHeaderText("發現新版本：" + latestVersion);
            alert.setContentText("是否要更新到最新版本？");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                downloadAndInstallUpdate(latestVersion);
            }
        });
    }

    private void downloadAndInstallUpdate(String version) {
        CompletableFuture.runAsync(() -> {
            try {
                GitHub github = GitHub.connectAnonymously();
                GHRepository repo = github.getRepository(REPO_OWNER + "/" + REPO_NAME);
                GHRelease release = repo.getReleaseByTagName("v" + version);

                if (release == null) {
                    log.error("找不到版本 {} 的發布", version);
                    showErrorAlert("更新錯誤", "找不到指定版本的發布。");
                    return;
                }

                Optional<GHAsset> jarAsset = release.listAssets().toList().stream()
                        .filter(asset -> asset.getName().endsWith(".jar"))
                        .findFirst();

                Optional<GHAsset> checksumAsset = release.listAssets().toList().stream()
                        .filter(asset -> asset.getName().endsWith(".jar.sha256"))
                        .findFirst();

                if (jarAsset.isEmpty() || checksumAsset.isEmpty()) {
                    log.error("在發布 {} 中找不到 JAR 文件或校驗和文件", version);
                    showErrorAlert("更新錯誤", "在發布中找不到 JAR 文件或校驗和文件。");
                    return;
                }

                String jarUrl = jarAsset.get().getBrowserDownloadUrl();
                String checksumUrl = checksumAsset.get().getBrowserDownloadUrl();

                Path updatePath = Path.of("update.jar");
                Path checksumPath = Path.of("update.jar.sha256");

                downloadFile(jarUrl, updatePath);
                downloadFile(checksumUrl, checksumPath);

                if (verifyChecksum(updatePath, checksumPath)) {
                    Path currentJar = Path.of(currentJarName);
                    Files.move(updatePath, currentJar, StandardCopyOption.REPLACE_EXISTING);
                    log.info("更新成功下載並安裝。");
                    propertiesManager.setString("app.version", version);
                    propertiesManager.saveSettings();
                    restartApplication();
                } else {
                    log.error("更新文件校驗和驗證失敗");
                    showErrorAlert("更新錯誤", "更新文件校驗和驗證失敗，請重試。");
                }
            } catch (Exception e) {
                log.error("下載或安裝更新時發生錯誤", e);
                showErrorAlert("更新錯誤", "下載或安裝更新時發生錯誤：" + e.getMessage());
            }
        }, executorService);
    }

    private void downloadFile(String url, Path savePath) throws IOException {
        try (InputStream in = new URL(url).openStream()) {
            Files.copy(in, savePath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private boolean verifyChecksum(Path filePath, Path checksumPath) throws IOException {
        String expectedChecksum = Files.readString(checksumPath).trim().split(" ")[0];
        String actualChecksum = DigestUtils.sha256Hex(Files.newInputStream(filePath));
        return actualChecksum.equalsIgnoreCase(expectedChecksum);
    }

    private void showErrorAlert(String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }

    private void restartApplication() {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("更新完成");
            alert.setHeaderText("應用程序將重新啟動");
            alert.setContentText("更新已安裝完成，應用程序將重新啟動以應用更新。");
            alert.showAndWait();

            Platform.exit();
            try {
                Runtime.getRuntime().exec("java -jar " + currentJarName);
            } catch (IOException e) {
                log.error("重新啟動應用程序時發生錯誤", e);
            }
            System.exit(0);
        });
    }

    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }
}