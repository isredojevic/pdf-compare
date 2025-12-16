package com.devoteam.a1.pdf.compare.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "compare")
public record CompareProperties(
        String oldRoot,
        String newRoot,
        String reportDir,
        Integer parallelism,
        Integer logPeriod,
        FilenameKeyProperties filenameKey
) {
    public int resolvedParallelism() {
        return parallelism != null && parallelism > 0
                ? parallelism
                : Runtime.getRuntime().availableProcessors();
    }

    public record FilenameKeyProperties(
            Mode mode,
            Integer startIndex,
            Integer endIndex
    ) {
        public enum Mode {
            FROM_BEGINNING_TO_INDEX,
            FROM_INDEX_TO_INDEX,
            FROM_INDEX_TO_END
        }
    }
}
