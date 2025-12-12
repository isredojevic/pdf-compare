package com.devoteam.a1.pdf.compare.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PdfTextService {

    public String extractNormalized(Path pdf) {
        try (PDDocument doc = PDDocument.load(pdf.toFile())) {

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            stripper.setSuppressDuplicateOverlappingText(true);
            stripper.setLineSeparator("\n");

            String rawText = stripper.getText(doc);
            return normalizePdfText(rawText);

        } catch (Exception e) {
            throw new RuntimeException("Text extraction failed: " + pdf, e);
        }
    }

    private String normalizePdfText(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        // 1. Remove invisible / non-printable PDF characters
        text = removeInvisibleCharacters(text);

        // 2. Normalize case & whitespace
        text = text
                .toLowerCase()
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("\\s+", " ")
                .trim();

        // 3. Normalize numeric formats
        text = text
                .replaceAll("(\\d),(\\d)", "$1.$2")      // 3560,25 -> 3560.25
                .replaceAll("(\\d)\\s+([.,])", "$1$2")  // 3560 ,25 -> 3560,25
                .replaceAll("([.,])\\s+(\\d)", "$1$2"); // 3560, 25 -> 3560,25

        // 4. Fix letter fragmentation (E U R -> EUR)
        text = text.replaceAll("([a-z])\\s+([a-z])", "$1$2");

        // 5. Canonicalize token order (KEY STEP)
        return canonicalize(text);
    }

    private String removeInvisibleCharacters(String text) {
        return text
                .replace('\u00A0', ' ')   // non-breaking space
                .replace('\u2007', ' ')
                .replace('\u202F', ' ')
                .replace("\u200B", "")    // zero-width space
                .replace("\u00AD", "")    // soft hyphen
                .replace("\uFEFF", "");   // BOM
    }

    private String canonicalize(String text) {
        return Arrays.stream(text.split(" "))
                .filter(s -> !s.isBlank())
                .sorted()
                .collect(Collectors.joining(" "));
    }

}
