package com.ieltsmate;

import com.ieltsmate.dao.EssayDAO;
import com.ieltsmate.dao.UserDAO;
import com.ieltsmate.dao.WritingTaskDAO;
import com.ieltsmate.entity.Essay;
import com.ieltsmate.entity.EssayStatus;
import com.ieltsmate.service.AuthService;
import com.ieltsmate.service.BackupService;
import com.ieltsmate.service.EssayService;
import com.ieltsmate.service.ExportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EssayServiceTest extends DatabaseTestSupport {
    @TempDir
    Path temporaryDirectory;

    private AuthService authService;
    private EssayService essayService;
    private EssayDAO essayDAO;

    @BeforeEach
    void createEssayService() throws Exception {
        essayDAO = new EssayDAO(connection);
        authService = new AuthService(new UserDAO(connection));
        authService.login("student-a", "correct-password").orElseThrow();
        essayService = new EssayService(essayDAO, new WritingTaskDAO(connection), authService);
    }

    @Test
    void createsDraftSavesItAndSubmitsItWithoutAllowingLaterEdits() throws Exception {
        Essay essay = essayService.createEssay(1, "First draft", "Initial content");
        assertEquals(EssayStatus.DRAFT, essay.getStatus());
        assertEquals(1, essay.getStudentId());

        essay = essayService.saveDraft((int) essay.getId(), "Updated title", "Updated content");
        assertEquals("Updated title", essay.getTitle());
        assertEquals("Updated content", essay.getContent());
        assertEquals(EssayStatus.DRAFT, essay.getStatus());

        essay = essayService.submitEssay((int) essay.getId(), "Final title", "Final content");
        assertEquals(EssayStatus.SUBMITTED, essay.getStatus());
        assertNotNull(essay.getSubmittedAt());
        int submittedEssayId = (int) essay.getId();
        assertThrows(IllegalStateException.class,
                () -> essayService.saveDraft(submittedEssayId, "Changed", "Changed"));
    }

    @Test
    void preventsStudentFromEditingAnotherStudentsEssay() throws Exception {
        Essay essay = essayService.createEssay(1, "Student A essay", "Content");

        authService.login("student-b", "correct-password").orElseThrow();
        int essayId = (int) essay.getId();
        assertThrows(SecurityException.class,
                () -> essayService.saveDraft(essayId, "Unauthorized", "Changed"));
        assertEquals("Student A essay", essayDAO.findById(essayId).orElseThrow().getTitle());
    }

    @Test
    void exportsUtf8TextAndBacksUpAndRestoresEssays() throws Exception {
        Essay essay = essayService.createEssay(1, "Bài viết tiếng Việt", "Nội dung có dấu: tiếng Việt.");
        Path textFile = temporaryDirectory.resolve("essay.txt");
        new ExportService().exportEssayToTxt(essay, textFile);
        String exported = Files.readString(textFile);
        assertTrue(exported.contains("Bài viết tiếng Việt"));
        assertTrue(exported.contains("Nội dung có dấu: tiếng Việt."));

        Path backupFile = temporaryDirectory.resolve("essays.backup");
        BackupService backupService = new BackupService();
        backupService.backupEssays(List.of(essay), backupFile);
        List<Essay> restoredEssays = backupService.restoreEssays(backupFile);

        assertEquals(1, restoredEssays.size());
        assertEquals(essay.getId(), restoredEssays.getFirst().getId());
        assertEquals(essay.getTitle(), restoredEssays.getFirst().getTitle());
        assertEquals(essay.getContent(), restoredEssays.getFirst().getContent());
    }
}
