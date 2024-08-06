package com.zxzinn.novelai.utils.embed;

import lombok.extern.log4j.Log4j2;

import java.util.*;
import java.util.stream.Collectors;

@Log4j2
public class ConditionProcessor {
    private final Map<String, Object> tagset;
    private final Random random;

    public ConditionProcessor(Map<String, Object> tagset) {
        this.tagset = tagset;
        this.random = new Random();
    }

    public List<String> processConditions(String condition) {
        if (condition == null || condition.trim().isEmpty()) {
            log.warn("No condition specified, processing all tag groups");
            return tagset.values().stream()
                    .flatMap(tagGroup -> new TagGroupProcessor(tagGroup).process().stream())
                    .collect(Collectors.toList());
        }

        return evaluateCondition(condition);
    }

    private List<String> evaluateCondition(String condition) {
        String[] parts = condition.split("\\s+and\\s+");
        List<String> result = new ArrayList<>();

        for (String part : parts) {
            if (part.contains(" or ")) {
                result.addAll(evaluateOrCondition(part));
            } else {
                result.addAll(new TagGroupProcessor(tagset.get(part.trim())).process());
            }
        }

        return result;
    }

    private List<String> evaluateOrCondition(String orCondition) {
        String[] groups = orCondition.replaceAll("[()]", "").split("\\s+or\\s+");
        List<String> validGroups = new ArrayList<>();

        for (String group : groups) {
            Object tagGroup = tagset.get(group.trim());
            if (tagGroup != null) {
                validGroups.add(group.trim());
            }
        }

        if (validGroups.isEmpty()) {
            log.warn("Warning: No valid groups found in or condition");
            return Collections.emptyList();
        }

        String selectedGroup = validGroups.get(random.nextInt(validGroups.size()));
        log.info("Selected group from or condition: {}", selectedGroup);
        return new TagGroupProcessor(tagset.get(selectedGroup)).process();
    }
}