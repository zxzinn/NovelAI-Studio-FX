package com.zxzinn.novelai.utils.tokenizer;

import com.google.inject.Inject;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

public class SimpleTokenizer {
    private final Map<Integer, String> byteEncoder;
    private final Map<String, Integer> encoder;
    private final Map<String, Integer> bpeRanks;
    private final Map<String, String> cache;
    private final Pattern pattern;

    @Inject
    public SimpleTokenizer(String bpePath) throws IOException {
        byteEncoder = bytesToUnicode();
        List<String[]> merges = readMerges(bpePath);
        List<String> vocab = new ArrayList<>(byteEncoder.values());
        vocab.addAll(vocab.stream().map(v -> v + "</w>").toList());
        merges.forEach(merge -> vocab.add(String.join("", merge)));
        vocab.add("<|startoftext|>");
        vocab.add("<|endoftext|>");

        encoder = new HashMap<>();
        for (int i = 0; i < vocab.size(); i++) {
            encoder.put(vocab.get(i), i);
        }

        bpeRanks = new HashMap<>();
        for (int i = 0; i < merges.size(); i++) {
            bpeRanks.put(String.join("", merges.get(i)), i);
        }

        cache = new HashMap<>();
        // 修改正則表達式，排除 { } [ ] 和換行符
        pattern = Pattern.compile("<\\|startoftext\\|>|<\\|endoftext\\|>|'s|'t|'re|'ve|'m|'ll|'d|[\\p{L}]+|[\\p{N}]|[^\\s\\p{L}\\p{N}{}\\[\\]\n]+", Pattern.CASE_INSENSITIVE);
    }

    @NotNull
    private Map<Integer, String> bytesToUnicode() {
        List<Integer> bs = new ArrayList<>();
        for (int i = '!'; i <= '~'; i++) bs.add(i);
        for (int i = '¡'; i <= '¬'; i++) bs.add(i);
        for (int i = '®'; i <= 'ÿ'; i++) bs.add(i);

        List<Integer> cs = new ArrayList<>(bs);
        int n = 0;
        for (int b = 0; b < 256; b++) {
            if (!bs.contains(b)) {
                bs.add(b);
                cs.add(256 + n);
                n++;
            }
        }

        Map<Integer, String> byteEncoder = new HashMap<>();
        for (int i = 0; i < bs.size(); i++) {
            byteEncoder.put(bs.get(i), String.valueOf((char) cs.get(i).intValue()));
        }
        return byteEncoder;
    }

    @NotNull
    private List<String[]> readMerges(String path) throws IOException {
        List<String[]> merges = new ArrayList<>();
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(path)), StandardCharsets.UTF_8))) {
            bufferedReader.readLine(); // Skip first line
            String line;
            while ((line = bufferedReader.readLine()) != null && merges.size() < 49152 - 256 - 2 + 1) {
                merges.add(line.split(" "));
            }
        }
        return merges;
    }

    private String bpe(String token) {
        if (cache.containsKey(token)) {
            return cache.get(token);
        }

        List<String> word = new ArrayList<>(Arrays.asList(token.split("")));
        word.set(word.size() - 1, word.getLast() + "</w>");

        while (true) {
            int minRank = Integer.MAX_VALUE;
            String bestPair = null;
            for (int i = 0; i < word.size() - 1; i++) {
                String pair = word.get(i) + word.get(i + 1);
                int rank = bpeRanks.getOrDefault(pair, Integer.MAX_VALUE);
                if (rank < minRank) {
                    minRank = rank;
                    bestPair = pair;
                }
            }

            if (bestPair == null) break;

            List<String> newWord = new ArrayList<>();
            for (int i = 0; i < word.size(); i++) {
                if (i < word.size() - 1 && (word.get(i) + word.get(i + 1)).equals(bestPair)) {
                    newWord.add(bestPair);
                    i++;
                } else {
                    newWord.add(word.get(i));
                }
            }
            word = newWord;

            if (word.size() == 1) break;
        }

        String result = String.join(" ", word);
        cache.put(token, result);
        return result;
    }

    public List<Integer> encode(String text) {
        List<Integer> bpeTokens = new ArrayList<>();
        text = text.replaceAll("\\s+", " ").trim().toLowerCase();
        Matcher matcher = pattern.matcher(text);

        StringBuilder currentToken = new StringBuilder();
        while (matcher.find()) {
            if (!currentToken.isEmpty()) {
                String encodedToken = currentToken.toString().chars()
                        .mapToObj(byteEncoder::get)
                        .collect(Collectors.joining());
                for (String bpeToken : bpe(encodedToken).split(" ")) {
                    bpeTokens.add(encoder.getOrDefault(bpeToken, encoder.get("<|endoftext|>")));
                }
                currentToken = new StringBuilder();
            }
            String group = matcher.group();
            // 檢查是否為 { } [ ] 或換行符，如果是則跳過
            if (!group.matches("[{}\\[\\]\n]")) {
                currentToken.append(group);
            }
        }

        if (!currentToken.isEmpty()) {
            String encodedToken = currentToken.toString().chars()
                    .mapToObj(byteEncoder::get)
                    .collect(Collectors.joining());
            for (String bpeToken : bpe(encodedToken).split(" ")) {
                bpeTokens.add(encoder.getOrDefault(bpeToken, encoder.get("<|endoftext|>")));
            }
        }

        return bpeTokens;
    }
}