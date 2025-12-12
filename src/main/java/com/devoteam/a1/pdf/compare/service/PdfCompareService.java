package com.devoteam.a1.pdf.compare.service;

import com.devoteam.a1.pdf.compare.config.CompareProperties;
import com.devoteam.a1.pdf.compare.model.CompareResult;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfCompareService {

    private final PdfHashService hashService;
    private final PdfTextService textService;
    private final CompareProperties props;
    private final BusinessKeyExtractor businessKeyExtractor;

    @PostConstruct
    public void logProps() {
        System.out.println("Parallelism = " + props.parallelism());
    }

    public void run() throws Exception {
        log.info("Started " + LocalDateTime.now());

        ExecutorService pool = Executors.newFixedThreadPool(props.parallelism());

        Map<String, Path> newFilesByKey =
                Files.walk(Path.of(props.newRoot()))
                        .filter(p -> p.toString().endsWith(".pdf"))
                        .map(p -> businessKeyExtractor.extract(p)
                                .map(k -> Map.entry(k, p))
                                .orElse(null))
                        .filter(e -> e != null)
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                (a, b) -> {
                                    throw new IllegalStateException(
                                            "Duplicate business key in NEW files: " + a
                                    );
                                }
                        ));

        log.info("Indexed {} NEW pdf files", newFilesByKey.size());

        List<Path> oldFiles = Files.walk(Path.of(props.oldRoot()))
                .filter(p -> p.toString().endsWith(".pdf"))
                .collect(Collectors.toList());

        log.info("Found {} OLD pdf files", oldFiles.size());

        Files.createDirectories(Path.of(props.reportDir()));

        try (PrintWriter out = new PrintWriter(
                Files.newBufferedWriter(Path.of(props.reportDir(), "comparison.csv")))) {

            out.println("business_key,status,reason");

            List<Future<CompareResult>> futures = oldFiles.stream()
                    .map(p -> pool.submit(() -> compareOne(p, newFilesByKey)))
                    .toList();

            for (Future<CompareResult> f : futures) {
                CompareResult r = f.get();
                out.printf("%s,%s,%s%n",
                        r.relativePath(),
                        r.status(),
                        r.reason().replace(",", " "));
            }
        }

        log.info("Ended " + LocalDateTime.now());
        pool.shutdown();
    }

    private CompareResult compareOne(Path oldFile,
                                     Map<String, Path> newFilesByKey) {
        try {
            var keyOpt = businessKeyExtractor.extract(oldFile);

            if (keyOpt.isEmpty()) {
                return new CompareResult(
                        oldFile.getFileName().toString(),
                        "SKIPPED",
                        "Cannot extract business key"
                );
            }

            String businessKey = keyOpt.get();
            Path newFile = newFilesByKey.get(businessKey);

            if (newFile == null) {
                return new CompareResult(
                        businessKey,
                        "MISSING_NEW",
                        "No matching file in NEW set"
                );
            }

            String oldHash = hashService.sha256(oldFile);
            String newHash = hashService.sha256(newFile);

            if (oldHash.equals(newHash)) {
                return new CompareResult(
                        businessKey,
                        "IDENTICAL_BINARY",
                        "Exact match"
                );
            }

            String oldText = textService.extractNormalized(oldFile);
            String newText = textService.extractNormalized(newFile);

            if(oldText.contains("98600269148392")) {
                log.info(oldText);
                log.info("***********");
                log.info(newText);
                log.info("OLD hash: {}", DigestUtils.sha256Hex(oldText));
                log.info("NEW hash: {}", DigestUtils.sha256Hex(newText));

            }

            if (oldText.equals(newText)) {
                return new CompareResult(
                        businessKey,
                        "IDENTICAL_TEXT",
                        "Text equal"
                );
            }

            String diff = TextDiffUtil.diffTokens(oldText, newText, 10);

            return new CompareResult(
                    businessKey,
                    "TEXT_CHANGED",
                    diff
            );

        } catch (Exception e) {
            return new CompareResult(
                    oldFile.getFileName().toString(),
                    "ERROR",
                    e.getMessage()
            );
        }
    }

}
