package com.devoteam.a1.pdf.compare.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Optional;

@Component
@Slf4j
public class BusinessKeyExtractor {

    private static final int KEY_LENGTH = 29;

    public Optional<String> extract(Path file) {
        String name = file.getFileName().toString();

        int dotIndex = name.lastIndexOf('.');
        if (dotIndex < 0) {
            return Optional.empty();
        }

        if (name.length() < KEY_LENGTH) {
            return Optional.empty();
        }

        if (!name.substring(0, KEY_LENGTH).matches("\\d+_\\d+")) {
            log.warn("Unexpected filename format: {}", name);
        }

        return Optional.of(name.substring(0, KEY_LENGTH));
    }
}
