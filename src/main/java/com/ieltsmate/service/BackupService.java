package com.ieltsmate.service;

import com.ieltsmate.entity.Essay;
import com.ieltsmate.util.FileManager;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class BackupService {
    public synchronized void backupEssays(List<Essay> essays, Path backupPath) throws IOException {
        FileManager.backupEssays(
                Objects.requireNonNull(essays, "essays must not be null"),
                Objects.requireNonNull(backupPath, "backupPath must not be null"));
    }

    public synchronized List<Essay> restoreEssays(Path backupPath)
            throws IOException, ClassNotFoundException {
        return FileManager.restoreEssays(Objects.requireNonNull(backupPath, "backupPath must not be null"));
    }
}
