package com.devoteam.a1.pdf.compare.service;

import java.util.*;

public class TextDiffUtil {

    public static String diffTokens(String oldText, String newText, int maxDiffs) {
        Set<String> oldTokens = new LinkedHashSet<>(List.of(oldText.split(" ")));
        Set<String> newTokens = new LinkedHashSet<>(List.of(newText.split(" ")));

        Set<String> removed = new LinkedHashSet<>(oldTokens);
        removed.removeAll(newTokens);

        Set<String> added = new LinkedHashSet<>(newTokens);
        added.removeAll(oldTokens);

        StringBuilder sb = new StringBuilder();

        if (!removed.isEmpty()) {
            sb.append("REMOVED: ")
                    .append(limit(removed, maxDiffs))
                    .append(" | ");
        }

        if (!added.isEmpty()) {
            sb.append("ADDED: ")
                    .append(limit(added, maxDiffs));
        }

        return sb.length() == 0 ? "NO_DIFFERENCE" : sb.toString();
    }

    private static String limit(Set<String> tokens, int max) {
        return tokens.stream()
                .limit(max)
                .toList()
                .toString();
    }
}