package com.ieltsmate.service;

import com.ieltsmate.entity.Essay;
import com.ieltsmate.util.FileManager;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

public class ExportService {
    public void exportEssayToTxt(Essay essay, Path outputPath) throws IOException {
        FileManager.exportEssayToTxt(
                Objects.requireNonNull(essay, "essay must not be null"),
                Objects.requireNonNull(outputPath, "outputPath must not be null"));
    }
}
