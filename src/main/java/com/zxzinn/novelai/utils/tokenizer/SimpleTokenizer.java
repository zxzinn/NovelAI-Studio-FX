package com.zxzinn.novelai.utils.tokenizer;

import com.google.inject.Inject;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.zip.GZIPInputStream;

public class SimpleTokenizer {
    private static final int VOCAB_SIZE = 49152 - 256 - 2 + 1;
    private static final String START_OF_TEXT = "<|startoftext|>";
    private static final String END_OF_TEXT = "<|endoftext|>";
    private static final String END_OF_WORD = "</w>";

    private final Map<Integer, String> byteEncoder;
    private final Map<String, Integer> encoder;
    private final Map<String, Integer> bpeRanks;
    private final Map<String, String> cache;
    private final Pattern pattern;

    @Inject
    public SimpleTokenizer(String bpePath) throws IOException {
        this.byteEncoder = bytesToUnicode();
        List<String[]> merges = readMerges(bpePath);
        this.encoder = buildEncoder(merges);
        this.bpeRanks = buildBpeRanks(merges);
        this.cache = new HashMap<>();
        this.pattern = compilePattern();
    }

    @NotNull
    private Map<Integer, String> bytesToUnicode() {
        List<Integer> bs = new ArrayList<>();
        IntStream.rangeClosed('!', '~').forEach(bs::add);
        IntStream.rangeClosed('¡', '¬').forEach(bs::add);
        IntStream.rangeClosed('®', 'ÿ').forEach(bs::add);

        List<Integer> cs = new ArrayList<>(bs);
        int n = 0;
        for (int b = 0; b < 256; b++) {
            if (!bs.contains(b)) {
                bs.add(b);
                cs.add(256 + n++);
            }
        }

        return IntStream.range(0, bs.size()).boxed()
                .collect(Collectors.toMap(bs::get, i -> String.valueOf((char) cs.get(i).intValue())));
    }

    @NotNull
    private List<String[]> readMerges(String path) throws IOException {
        List<String[]> merges = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(path)), StandardCharsets.UTF_8))) {
            reader.readLine(); // Skip first line
            String line;
            while ((line = reader.readLine()) != null && merges.size() < VOCAB_SIZE) {
                merges.add(line.split(" "));
            }
        }
        return merges;
    }

    private Map<String, Integer> buildEncoder(List<String[]> merges) {
        List<String> vocab = new ArrayList<>(byteEncoder.values());
        vocab.addAll(vocab.stream().map(v -> v + END_OF_WORD).toList());
        merges.forEach(merge -> vocab.add(String.join("", merge)));
        vocab.add(START_OF_TEXT);
        vocab.add(END_OF_TEXT);

        return IntStream.range(0, vocab.size()).boxed()
                .collect(Collectors.toMap(vocab::get, i -> i));
    }

    private Map<String, Integer> buildBpeRanks(List<String[]> merges) {
        return IntStream.range(0, merges.size()).boxed()
                .collect(Collectors.toMap(i -> String.join("", merges.get(i)), i -> i));
    }

    private Pattern compilePattern() {
        return Pattern.compile(START_OF_TEXT + "|" + END_OF_TEXT + "|'s|'t|'re|'ve|'m|'ll|'d|[\\p{L}]+|[\\p{N}]|[^\\s\\p{L}\\p{N}{}\\[\\]\n]+", Pattern.CASE_INSENSITIVE);
    }

    private String bpe(String token) {
        return cache.computeIfAbsent(token, this::computeBpe);
    }

    private String computeBpe(String token) {
        List<String> word = new ArrayList<>(Arrays.asList(token.split("")));
        word.set(word.size() - 1, word.get(word.size() - 1) + END_OF_WORD);

        while (word.size() > 1) {
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
        }

        return String.join(" ", word);
    }

    public List<Integer> encode(String text) {
        List<Integer> bpeTokens = new ArrayList<>();
        text = text.replaceAll("\\s+", " ").trim().toLowerCase();
        Matcher matcher = pattern.matcher(text);

        StringBuilder currentToken = new StringBuilder();
        while (matcher.find()) {
            if (!currentToken.isEmpty()) {
                processToken(currentToken.toString(), bpeTokens);
                currentToken = new StringBuilder();
            }
            String group = matcher.group();
            if (!group.matches("[{}\\[\\]\n]")) {
                currentToken.append(group);
            }
        }

        if (!currentToken.isEmpty()) {
            processToken(currentToken.toString(), bpeTokens);
        }

        return bpeTokens;
    }

    private void processToken(String token, List<Integer> bpeTokens) {
        String encodedToken = token.chars()
                .mapToObj(byteEncoder::get)
                .collect(Collectors.joining());
        for (String bpeToken : bpe(encodedToken).split(" ")) {
            bpeTokens.add(encoder.getOrDefault(bpeToken, encoder.get(END_OF_TEXT)));
        }
    }
}