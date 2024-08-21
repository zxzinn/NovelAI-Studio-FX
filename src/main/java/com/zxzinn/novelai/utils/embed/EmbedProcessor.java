package com.zxzinn.novelai.utils.embed;

import lombok.extern.log4j.Log4j2;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Log4j2
public class EmbedProcessor {
    private static final String EMBEDS_DIRECTORY = "embeds";
    private final EmbedDetector embedDetector;
    private final Random random;

    public EmbedProcessor() {
        this.embedDetector = new EmbedDetector();
        this.random = new Random();
    }
    public String processPrompt(String input) {
        log.info("Processing prompt: {}", input);

        String processedInput = processEmbeds(input);
        String finalResult = processStringPatterns(processedInput);

        log.info("Final processed prompt: {}", finalResult);
        return finalResult;
    }

    private String processEmbeds(String input) {
        List<EmbedDetector.EmbedTag> embeds = embedDetector.detectEmbeds(input);
        StringBuilder result = new StringBuilder(input);

        for (int i = embeds.size() - 1; i >= 0; i--) {
            EmbedDetector.EmbedTag embed = embeds.get(i);
            try {
                List<String> generatedTags = processEmbedFile(embed.name(), embed.sampling(), embed.bracketing());

                if (!generatedTags.isEmpty()) {
                    String replacement = String.join(",", generatedTags);
                    result.replace(embed.start(), embed.end(), replacement);
                    log.debug("Generated tags for {}: {}", embed.name(), generatedTags);
                } else {
                    result.delete(embed.start(), embed.end());
                    log.debug("No tags generated for: {}", embed.name());
                }
            } catch (IOException e) {
                log.error("Error processing file for tag: {}. Error: {}", embed.name(), e.getMessage());
                result.delete(embed.start(), embed.end());
            }
        }

        return result.toString();
    }

    private List<String> processEmbedFile(String tagName, String sampling, String bracketing) throws IOException {
        String filePath = EMBEDS_DIRECTORY + File.separator + tagName.replace("/", File.separator) + ".txt";
        File file = new File(filePath);

        if (!file.exists()) {
            throw new FileNotFoundException("Text file not found: " + filePath);
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            List<String> allTags = reader.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .collect(Collectors.toList());

            if (sampling == null || sampling.isEmpty() || !isValidSampling(sampling)) {
                return applyBracketing(allTags, bracketing);
            } else {
                int sampleSize = getSampleSize(sampling, allTags.size());
                List<String> sampledTags = selectRandomTags(allTags, sampleSize);
                return applyBracketing(sampledTags, bracketing);
            }
        }
    }

    private List<String> applyBracketing(List<String> tags, String bracketing) {
        if (bracketing == null || bracketing.isEmpty() || !isValidBracketing(bracketing)) {
            return tags;
        }

        return tags.stream()
                .map(tag -> applyBracketToTag(tag, bracketing))
                .collect(Collectors.toList());
    }

    private String applyBracketToTag(String tag, String bracketing) {
        int bracketCount = getBracketCount(bracketing);
        if (bracketCount == 0) {
            return tag;
        }
        String openBracket = bracketCount > 0 ? "{" : "[";
        String closeBracket = bracketCount > 0 ? "}" : "]";
        return openBracket.repeat(Math.abs(bracketCount)) + tag + closeBracket.repeat(Math.abs(bracketCount));
    }

    private boolean isValidSampling(String sampling) {
        return sampling.matches("\\d+") || sampling.matches("\\d+~\\d+");
    }

    private boolean isValidBracketing(String bracketing) {
        return bracketing.matches("-?\\d+") || bracketing.matches("-?\\d+~-?\\d+");
    }

    private int getSampleSize(String sampling, int totalTags) {
        try {
            if (sampling.contains("~")) {
                String[] range = sampling.split("~");
                if (range.length != 2) {
                    log.warn("Invalid sampling range: {}", sampling);
                    return totalTags;
                }
                int min = Integer.parseInt(range[0]);
                int max = Integer.parseInt(range[1]);
                return new Random().nextInt(max - min + 1) + min;
            } else {
                return Math.min(Integer.parseInt(sampling), totalTags);
            }
        } catch (NumberFormatException e) {
            log.warn("Invalid sampling format: {}", sampling);
            return totalTags;
        }
    }

    private int getBracketCount(String bracketing) {
        try {
            if (bracketing.contains("~")) {
                String[] range = bracketing.split("~");
                if (range.length != 2) {
                    log.warn("Invalid bracketing range: {}", bracketing);
                    return 0;
                }
                int min = Integer.parseInt(range[0]);
                int max = Integer.parseInt(range[1]);
                return random.nextInt(max - min + 1) + min;
            } else {
                return Integer.parseInt(bracketing);
            }
        } catch (NumberFormatException e) {
            log.warn("Invalid bracketing format: {}", bracketing);
            return 0;
        }
    }

    private List<String> selectRandomTags(List<String> tags, int count) {
        List<String> shuffled = new ArrayList<>(tags);
        Collections.shuffle(shuffled);
        return shuffled.subList(0, Math.min(count, shuffled.size()));
    }

    private String processStringPatterns(String input) {
        Pattern bracketPattern = Pattern.compile("\\{(\\w+)(?:=null)?}");
        Matcher bracketMatcher = bracketPattern.matcher(input);

        StringBuilder stringBuilder = new StringBuilder();
        while (bracketMatcher.find()) {
            String replacement = "{" + bracketMatcher.group(1) + "}";
            bracketMatcher.appendReplacement(stringBuilder, Matcher.quoteReplacement(replacement));
        }
        bracketMatcher.appendTail(stringBuilder);

        String result = stringBuilder.toString();
        result = result.replaceAll(",,+", ",").replaceAll("^,|,$", "");

        return result;
    }
}