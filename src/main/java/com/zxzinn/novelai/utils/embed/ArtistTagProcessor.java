package com.zxzinn.novelai.utils.embed;

import java.util.List;
import java.util.stream.Collectors;

public class ArtistTagProcessor {
    public List<String> process(List<String> tags) {
        return tags.stream()
                .map(tag -> "artist:" + tag)
                .collect(Collectors.toList());
    }
}