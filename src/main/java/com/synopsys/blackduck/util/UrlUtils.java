package com.synopsys.blackduck.util;

import com.synopsys.integration.blackduck.api.core.BlackDuckView;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

/**
 * Helper methods for encoding URL.
 *
 * @author David Nicholls - Synopsys Black Duck Technical Architect
 */
public class UrlUtils {

    /**
     * URL Encode a name.
     * @param name the name to encode.
     * @return encoded name.
     * @throws IllegalArgumentException if the name could not be encoded.
     */
    public static String encode(String name) throws IllegalArgumentException {
        try {
            return java.net.URLEncoder.encode(name, StandardCharsets.UTF_8.displayName());
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("Failed to encode [" + name + "] due to : " + e.getMessage(), e);
        }
    }

    /**
     * Returns the ID for an object (the last part of the URL).
     * @param view BlackDuckView
     * @return String url.
     */
    public static String getId(BlackDuckView view) {
        String id = view.getMeta().getHref().string();
        id = id.substring(id.lastIndexOf("/") + 1);
        return id;
    }
}
