package com.zxzinn.novelai.utils.embed;

import lombok.extern.log4j.Log4j2;

import java.util.*;
import java.util.stream.Collectors;

@Log4j2
@SuppressWarnings("unchecked")
public class TagGroupProcessor {
    private final Map<String, Object> group;
    private final Random random;

    public TagGroupProcessor(Object tagGroup) {
        if (!(tagGroup instanceof Map)) {
            throw new IllegalArgumentException("Invalid tag group format");
        }
        this.group = (Map<String, Object>) tagGroup;
        this.random = new Random();
    }

    public List<String> process() {
        List<String> tags = ((List<?>) group.get("tags")).stream()
                .map(Object::toString)
                .collect(Collectors.toList());
        boolean isArtistTag = (boolean) group.getOrDefault("ArtistTagPrefix", false);
        Object samplingObj = group.getOrDefault("Sampling", 1);

        List<String> selectedTags;
        if (samplingObj instanceof String && "all".equalsIgnoreCase((String) samplingObj)) {
            selectedTags = new ArrayList<>(tags);
        } else {
            int sampling = samplingObj instanceof Number ? ((Number) samplingObj).intValue() : 1;
            selectedTags = selectRandomTags(tags, sampling);
        }

        selectedTags = selectedTags.stream()
                .map(this::processTag)
                .collect(Collectors.toList());

        if (isArtistTag) {
            selectedTags = new ArtistTagProcessor().process(selectedTags);
        }

        log.info("Processed tag group. Selected tags: {}", selectedTags);
        return selectedTags;
    }

    private String processTag(String tag) {
        if (tag.startsWith("+")) {
            int weight = tag.length() - tag.replace("+", "").length();
            String cleanTag = tag.replace("+", "");
            return "{".repeat(weight) + cleanTag + "}".repeat(weight);
        } else if (tag.startsWith("-")) {
            int weight = tag.length() - tag.replace("-", "").length();
            String cleanTag = tag.replace("-", "");
            return "[".repeat(weight) + cleanTag + "]".repeat(weight);
        } else {
            return tag;
        }
    }

    private List<String> selectRandomTags(List<String> tags, int count) {
        List<String> shuffled = new ArrayList<>(tags);
        Collections.shuffle(shuffled, random);
        return shuffled.subList(0, Math.min(count, shuffled.size()));
    }
}