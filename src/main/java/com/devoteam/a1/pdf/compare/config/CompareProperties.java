package com.devoteam.a1.pdf.compare.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "compare")
public record CompareProperties(
        String oldRoot,
        String newRoot,
        String reportDir,
        Integer parallelism
) {
    public int resolvedParallelism() {
        return parallelism != null && parallelism > 0
                ? parallelism
                : Runtime.getRuntime().availableProcessors();
    }
}
