package com.zxzinn.novelai.utils.embed;

import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Log4j2
public class EmbedDetector {
    private static final Pattern EMBED_PATTERN = Pattern.compile("<([\\w\\s/]+)>");

    public List<EmbedTag> detectEmbeds(String input) {
        if (input == null || input.isEmpty()) {
            return Collections.emptyList();
        }
        List<EmbedTag> embeds = new ArrayList<>();
        Matcher matcher = EMBED_PATTERN.matcher(input);
        while (matcher.find()) {
            String tagName = matcher.group(1);
            int start = matcher.start();
            int end = matcher.end();
            embeds.add(new EmbedTag(tagName, start, end));
            log.debug("Detected embed tag: {} at position {}-{}", tagName, start, end);
        }
        return embeds;
    }

    public record EmbedTag(String name, int start, int end) {
    }
}