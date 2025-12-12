package com.devoteam.a1.pdf.compare.model;

public record CompareResult(
        String relativePath,
        String status,
        String reason
) {}


//IDENTICAL_BINARY
//IDENTICAL_TEXT
//TEXT_CHANGED
//MISSING_NEW
//ERROR