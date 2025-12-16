package com.devoteam.a1.pdf.compare.service;


import com.devoteam.a1.pdf.compare.config.CompareProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Optional;

import static com.devoteam.a1.pdf.compare.config.CompareProperties.FilenameKeyProperties.Mode.*;

@Component
@RequiredArgsConstructor
public class FilenameKeyExtractor {

    private final CompareProperties props;

    public Optional<String> extract(Path file) {
        String name = file.getFileName().toString();

        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            name = name.substring(0, dot); // remove extension
        }

        var cfg = props.filenameKey();
        if (cfg == null || cfg.mode() == null) {
            return Optional.empty();
        }

        return switch (cfg.mode()) {

            case FROM_BEGINNING_TO_INDEX -> {
                if (cfg.endIndex() == null || name.length() < cfg.endIndex()) {
                    yield Optional.empty();
                }
                yield Optional.of(name.substring(0, cfg.endIndex()));
            }

            case FROM_INDEX_TO_INDEX -> {
                if (cfg.startIndex() == null || cfg.endIndex() == null ||
                        name.length() < cfg.endIndex()) {
                    yield Optional.empty();
                }
                yield Optional.of(name.substring(cfg.startIndex(), cfg.endIndex()));
            }

            case FROM_INDEX_TO_END -> {
                if (cfg.startIndex() == null || name.length() < cfg.startIndex()) {
                    yield Optional.empty();
                }
                yield Optional.of(name.substring(cfg.startIndex()));
            }
        };
    }
}