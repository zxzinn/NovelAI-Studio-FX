package com.zxzinn.novelai.utils.embed;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class EmbedFileManager {
    private static final String EMBEDS_DIRECTORY = "embeds";
    private List<String> embedFiles;

    public EmbedFileManager() {
        scanEmbedFiles();
    }

    public void scanEmbedFiles() {
        try {
            Path embedsPath = Paths.get(EMBEDS_DIRECTORY);
            if (!Files.exists(embedsPath)) {
                log.warn("Embeds directory does not exist: {}", EMBEDS_DIRECTORY);
                embedFiles = new ArrayList<>();
                return;
            }

            embedFiles = Files.walk(embedsPath)
                    .filter(Files::isRegularFile)
                    .map(path -> embedsPath.relativize(path).toString())
                    .map(path -> path.replaceAll("\\\\", "/"))
                    .map(path -> path.replaceAll("\\.txt$", ""))
                    .collect(Collectors.toList());

            log.info("Scanned {} embed files", embedFiles.size());
        } catch (IOException e) {
            log.error("Error scanning embed files", e);
            embedFiles = new ArrayList<>();
        }
    }

    public List<String> getMatchingEmbeds(String prefix) {
        String lowercasePrefix = prefix.toLowerCase();
        return embedFiles.stream()
                .filter(file -> fuzzyMatch(file.toLowerCase(), lowercasePrefix))
                .sorted(Comparator.comparingInt(file -> calculateLevenshteinDistance(file.toLowerCase(), lowercasePrefix)))
                .limit(10)  // Limit to top 10 matches
                .collect(Collectors.toList());
    }

    private boolean fuzzyMatch(String str, String pattern) {
        int i = 0, j = 0;
        while (i < str.length() && j < pattern.length()) {
            if (str.charAt(i) == pattern.charAt(j)) {
                j++;
            }
            i++;
        }
        return j == pattern.length();
    }

    private int calculateLevenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = Math.min(dp[i - 1][j - 1]
                                    + (s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1),
                            Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1));
                }
            }
        }

        return dp[s1.length()][s2.length()];
    }
}