package com.ieltsmate.util;

import com.ieltsmate.entity.Essay;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class FileManager {
    private FileManager() {
    }

    public static synchronized void exportEssayToTxt(Essay essay, Path outputPath) throws IOException {
        Objects.requireNonNull(essay, "essay must not be null");
        Objects.requireNonNull(outputPath, "outputPath must not be null");
        createParentDirectories(outputPath);

        String text = "Title: " + nullToEmpty(essay.getTitle())
                + System.lineSeparator()
                + "Status: " + (essay.getStatus() == null ? "" : essay.getStatus())
                + System.lineSeparator()
                + System.lineSeparator()
                + nullToEmpty(essay.getContent());
        Files.writeString(outputPath, text, StandardCharsets.UTF_8);
    }

    public static synchronized void backupEssays(List<Essay> essays, Path backupPath)
            throws IOException {
        Objects.requireNonNull(essays, "essays must not be null");
        Objects.requireNonNull(backupPath, "backupPath must not be null");
        List<Essay> snapshot = new ArrayList<>(essays);
        createParentDirectories(backupPath);

        try (ObjectOutputStream output = new ObjectOutputStream(Files.newOutputStream(backupPath))) {
            output.writeObject(snapshot);
        }
    }

    public static synchronized List<Essay> restoreEssays(Path backupPath)
            throws IOException, ClassNotFoundException {
        Objects.requireNonNull(backupPath, "backupPath must not be null");
        try (ObjectInputStream input = new ObjectInputStream(Files.newInputStream(backupPath))) {
            Object restored = input.readObject();
            if (!(restored instanceof List<?> values)) {
                throw new IOException("Backup file does not contain a list of essays");
            }

            List<Essay> essays = new ArrayList<>(values.size());
            for (Object value : values) {
                if (!(value instanceof Essay essay)) {
                    throw new IOException("Backup file contains an invalid essay entry");
                }
                essays.add(essay);
            }
            return essays;
        }
    }

    private static void createParentDirectories(Path path) throws IOException {
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
