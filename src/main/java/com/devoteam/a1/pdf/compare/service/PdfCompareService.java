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
import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfCompareService {

    private final PdfHashService hashService;
    private final PdfTextService textService;
    private final CompareProperties props;
    private final FilenameKeyExtractor filenameKeyExtractor;

    private final LongAdder processed = new LongAdder();
    private final LongAdder identical = new LongAdder();
    private final LongAdder changed = new LongAdder();
    private final LongAdder missing = new LongAdder();
    private final LongAdder errors = new LongAdder();

    @PostConstruct
    public void logProps() {
        log.info("Configured parallelism = {}", props.parallelism());
        log.info("Resolved parallelism   = {}", props.resolvedParallelism());
        log.info("Log period = {}", props.logPeriod());
    }

    public void run() throws Exception {
        log.info("Comparison started at {}", LocalDateTime.now());

        int threads = props.resolvedParallelism();

        ExecutorService pool = new ThreadPoolExecutor(
                threads,
                threads,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(threads * 2),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        Map<String, Path> newFilesByKey =
                Files.walk(Path.of(props.newRoot()))
                        .filter(p -> p.toString().endsWith(".pdf"))
                        .map(p -> filenameKeyExtractor.extract(p)
                                .map(k -> Map.entry(k, p))
                                .orElse(null))
                        .filter(e -> e != null)
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                (a, b) -> {
                                    throw new IllegalStateException(
                                            "Duplicate filename key in NEW files: " + a
                                    );
                                }
                        ));

        log.info("Indexed {} NEW pdf files", newFilesByKey.size());

        Files.createDirectories(Path.of(props.reportDir()));

        try (PrintWriter out = new PrintWriter(
                Files.newBufferedWriter(Path.of(props.reportDir(), "comparison.csv")));
             var paths = Files.walk(Path.of(props.oldRoot()))) {

            out.println("business_key,status,reason");

            paths.filter(p -> p.toString().endsWith(".pdf"))
                    .forEach(p -> submitAndWait(p, pool, newFilesByKey, out));
        }

        pool.shutdown();
        pool.awaitTermination(1, TimeUnit.HOURS);

        log.info("Comparison ended at {}", LocalDateTime.now());
    }

    private CompareResult compareOne(Path oldFile,
                                     Map<String, Path> newFilesByKey) {


        try {
            var keyOpt = filenameKeyExtractor.extract(oldFile);

            if (keyOpt.isEmpty()) {
                return new CompareResult(
                        oldFile.getFileName().toString(),
                        "SKIPPED",
                        "Cannot extract filename key"
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

            if (oldText.equals(newText)) {
                return new CompareResult(
                        businessKey,
                        "IDENTICAL_TEXT",
                        "Text equal"
                );
            }

            String diff = TextDiffUtil.diffTokens(oldText, newText, 10);

            // Help GC
            oldText = null;
            newText = null;
            oldHash = null;
            newHash = null;

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

    private void submitAndWait(Path oldFile,
                               ExecutorService pool,
                               Map<String, Path> newFilesByKey,
                               PrintWriter out) {

        Future<CompareResult> f =
                pool.submit(() -> compareOne(oldFile, newFilesByKey));

        try {
            CompareResult r = f.get();

            processed.increment();

            switch (r.status()) {
                case "IDENTICAL_BINARY", "IDENTICAL_TEXT" -> identical.increment();
                case "TEXT_CHANGED" -> changed.increment();
                case "MISSING_NEW" -> missing.increment();
                case "ERROR" -> errors.increment();
            }

            if (processed.sum() % props.logPeriod() == 0) {
                log.info(
                        "Processed={}, Identical={}, Changed={}, Missing={}, Errors={}",
                        processed.sum(),
                        identical.sum(),
                        changed.sum(),
                        missing.sum(),
                        errors.sum()
                );
            }

            synchronized (out) {
                out.printf("%s,%s,%s%n",
                        r.relativePath(),
                        r.status(),
                        r.reason().replace(",", " "));
            }
        } catch (Exception e) {
            log.error("Failed processing {}", oldFile, e);
        }
    }

}
