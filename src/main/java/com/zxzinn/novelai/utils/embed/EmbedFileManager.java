package com.zxzinn.novelai.utils.embed;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class EmbedFileManager {
    private static final String EMBEDS_DIRECTORY = "embeds";
    private List<EmbedFile> allEmbeds;

    public EmbedFileManager() {
        allEmbeds = new ArrayList<>();
        CompletableFuture.runAsync(this::scanEmbedFiles)
                .exceptionally(ex -> {
                    log.error("Error scanning embed files", ex);
                    return null;
                });
    }

    public void scanEmbedFiles() {
        try {
            Path embedsPath = Paths.get(EMBEDS_DIRECTORY);
            if (!Files.exists(embedsPath)) {
                log.warn("Embeds directory does not exist: {}", EMBEDS_DIRECTORY);
                return;
            }

            allEmbeds = Files.walk(embedsPath)
                    .filter(Files::isRegularFile)
                    .map(path -> {
                        String relativePath = embedsPath.relativize(path).toString().replaceAll("\\\\", "/");
                        String fileName = path.getFileName().toString().replaceAll("\\.txt$", "");
                        String folder = relativePath.contains("/")
                                ? relativePath.substring(0, relativePath.lastIndexOf('/'))
                                : "";
                        return new EmbedFile(fileName, folder, relativePath);
                    })
                    .collect(Collectors.toList());

            log.info("Scanned and indexed {} embed files", allEmbeds.size());
        } catch (IOException e) {
            log.error("Error scanning embed files", e);
        }
    }

    public CompletableFuture<List<EmbedFile>> getMatchingEmbedsAsync(String query) {
        return CompletableFuture.supplyAsync(() -> getMatchingEmbeds(query));
    }

    private List<EmbedFile> getMatchingEmbeds(String query) {
        String lowercaseQuery = query.toLowerCase();
        return allEmbeds.stream()
                .filter(embed -> fuzzyMatch(embed.fullPath().toLowerCase(), lowercaseQuery))
                .sorted((a, b) -> {
                    int scoreA = calculateMatchScore(a.fullPath().toLowerCase(), lowercaseQuery);
                    int scoreB = calculateMatchScore(b.fullPath().toLowerCase(), lowercaseQuery);
                    return Integer.compare(scoreB, scoreA); // Descending order
                })
                .limit(10)
                .collect(Collectors.toList());
    }

    private boolean fuzzyMatch(String embed, String query) {
        int embedIndex = 0;
        int queryIndex = 0;
        while (embedIndex < embed.length() && queryIndex < query.length()) {
            if (embed.charAt(embedIndex) == query.charAt(queryIndex)) {
                queryIndex++;
            }
            embedIndex++;
        }
        return queryIndex == query.length();
    }

    private int calculateMatchScore(String embed, String query) {
        int score = 0;
        int embedIndex = 0;
        int queryIndex = 0;
        boolean lastMatched = false;

        while (embedIndex < embed.length() && queryIndex < query.length()) {
            if (embed.charAt(embedIndex) == query.charAt(queryIndex)) {
                score += lastMatched ? 2 : 1;
                lastMatched = true;
                queryIndex++;
            } else {
                lastMatched = false;
            }
            embedIndex++;
        }

        if (queryIndex == query.length()) {
            score += 5;
        }

        if (embed.startsWith(query)) {
            score += 3;
        }

        return score;
    }

    public record EmbedFile(String fileName, String folder, String fullPath) { }
}