package com.zxzinn.novelai.api;

import lombok.Getter;

@Getter
public enum Endpoint {
    GENERATE_IMAGE("https://api.novelai.net/ai/generate-image"),
    SUGGEST_TAGS("https://api.novelai.net/ai/generate-image/suggest-tags"),
    SUBSCRIPTION("https://api.novelai.net/user/subscription")
    ;
    // OTHER_API("https://api.other.com/generate");

    private final String url;

    Endpoint(String url) {
        this.url = url;
    }
}