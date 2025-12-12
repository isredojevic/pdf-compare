package com.devoteam.a1.pdf.compare.service;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class PdfHashService {

    public String sha256(Path pdf) {
        try (InputStream is = Files.newInputStream(pdf)) {
            return DigestUtils.sha256Hex(is);
        } catch (Exception e) {
            throw new RuntimeException("Hash failed: " + pdf, e);
        }
    }

}
