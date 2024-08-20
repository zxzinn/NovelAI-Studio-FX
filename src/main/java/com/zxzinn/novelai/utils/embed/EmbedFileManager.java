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
    private TrieNode root;

    public EmbedFileManager() {
        root = new TrieNode();
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

            Files.walk(embedsPath)
                    .filter(Files::isRegularFile)
                    .map(path -> embedsPath.relativize(path).toString())
                    .map(path -> path.replaceAll("\\\\", "/"))
                    .map(path -> path.replaceAll("\\.txt$", ""))
                    .forEach(this::addToTrie);

            log.info("Scanned and indexed embed files");
        } catch (IOException e) {
            log.error("Error scanning embed files", e);
        }
    }

    private void addToTrie(String word) {
        TrieNode node = root;
        for (char c : word.toLowerCase().toCharArray()) {
            node.children.putIfAbsent(c, new TrieNode());
            node = node.children.get(c);
        }
        node.isEndOfWord = true;
        node.word = word;
    }

    public CompletableFuture<List<String>> getMatchingEmbedsAsync(String prefix) {
        return CompletableFuture.supplyAsync(() -> getMatchingEmbeds(prefix));
    }

    private List<String> getMatchingEmbeds(String prefix) {
        String lowercasePrefix = prefix.toLowerCase();
        TrieNode node = root;
        for (char c : lowercasePrefix.toCharArray()) {
            if (!node.children.containsKey(c)) {
                return Collections.emptyList();
            }
            node = node.children.get(c);
        }
        List<String> results = new ArrayList<>();
        collectWords(node, results);
        results.sort((a, b) -> {
            int prefixDiff = Integer.compare(
                    commonPrefixLength(b, lowercasePrefix),
                    commonPrefixLength(a, lowercasePrefix)
            );
            return prefixDiff != 0 ? prefixDiff : a.compareTo(b);
        });
        return results.stream().limit(10).collect(Collectors.toList());
    }

    private void collectWords(TrieNode node, List<String> results) {
        if (node.isEndOfWord) {
            results.add(node.word);
        }
        for (TrieNode child : node.children.values()) {
            collectWords(child, results);
        }
    }

    private int commonPrefixLength(String s, String prefix) {
        int minLength = Math.min(s.length(), prefix.length());
        for (int i = 0; i < minLength; i++) {
            if (s.charAt(i) != prefix.charAt(i)) {
                return i;
            }
        }
        return minLength;
    }

    private static class TrieNode {
        Map<Character, TrieNode> children = new HashMap<>();
        boolean isEndOfWord;
        String word;
    }
}