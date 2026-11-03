package com.ieltsmate.service;

import com.ieltsmate.annotation.RubricCriterion;
import com.ieltsmate.entity.TaskType;
import com.ieltsmate.entity.TeacherAssessment;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class RubricService {
    public List<RubricField> getRubricCriteria() {
        return Arrays.stream(TeacherAssessment.class.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(RubricCriterion.class))
                .sorted(Comparator.comparing(Field::getName))
                .map(field -> new RubricField(field.getName(),
                        field.getAnnotation(RubricCriterion.class)))
                .toList();
    }

    public boolean validateCriteria() {
        List<RubricField> criteria = getRubricCriteria();
        if (criteria.size() != 4) {
            return false;
        }
        return criteria.stream().allMatch(criterion -> {
            RubricCriterion annotation = criterion.annotation();
            return isScoreField(criterion.fieldName())
                    && !annotation.name().isBlank()
                    && !annotation.description().isBlank()
                    && Double.isFinite(annotation.minScore())
                    && Double.isFinite(annotation.maxScore())
                    && Double.isFinite(annotation.step())
                    && annotation.minScore() == 0.0
                    && annotation.maxScore() == 9.0
                    && annotation.step() == 0.5
                    && (criterion.fieldName().equals("taskScore")
                    ? !annotation.task1Name().isBlank()
                    : annotation.task1Name().isEmpty());
        });
    }

    public CriterionGuidance getGuidance(TaskType taskType, String fieldName) {
        return switch (fieldName) {
            case "taskScore" -> taskType == TaskType.TASK_1
                    ? new CriterionGuidance("Task Achievement",
                    "Mở bài: diễn đạt lại yêu cầu và xác định đối tượng so sánh.",
                    "Thân bài: chọn đặc điểm chính, so sánh/nhóm số liệu và tránh liệt kê mọi con số.",
                    "Kết/overview: nêu xu hướng hoặc đặc điểm nổi bật; Task 1 không bắt buộc kết luận ý kiến.",
                    "5: bao quát hạn chế hoặc có thiếu sót đáng kể; 6: các đặc điểm chính được chọn nhưng phát triển chưa đều; "
                            + "7: bao quát rõ các đặc điểm chính với so sánh phù hợp; 8: chọn lọc và làm nổi bật thông tin chính chính xác.")
                    : new CriterionGuidance("Task Response",
                    "Mở bài: xác định trọng tâm và trả lời trực tiếp mọi phần của câu hỏi.",
                    "Thân bài: phát triển luận điểm liên quan, giải thích và hỗ trợ bằng ví dụ phù hợp.",
                    "Kết bài: tổng hợp lập trường/đáp án nhất quán, không đưa luận điểm mới.",
                    "5: trả lời một phần, phát triển ý hạn chế; 6: xử lý các phần chính nhưng hỗ trợ có chỗ chưa đủ; "
                            + "7: lập trường rõ, luận điểm được mở rộng và hỗ trợ; 8: trả lời đầy đủ, phát triển sâu và nhất quán.");
            case "coherenceScore" -> new CriterionGuidance("Coherence & Cohesion",
                    "Mở bài: tạo hướng triển khai rõ ràng, định vị đúng chủ đề.",
                    "Thân bài: mỗi đoạn có trọng tâm, trình tự ý hợp lý và liên kết vừa đủ.",
                    "Kết bài: khép lại mạch lập luận/miêu tả phù hợp, không đột ngột.",
                    "5: tổ chức còn thiếu mạch lạc, liên kết lặp hoặc gượng; 6: nhìn chung có trình tự nhưng liên kết/đoạn còn chưa đều; "
                            + "7: tiến triển logic, chia đoạn hợp lý, phương tiện liên kết khá linh hoạt; "
                            + "8: mạch ý rõ xuyên suốt, chia đoạn hiệu quả và liên kết tự nhiên.");
            case "lexicalScore" -> new CriterionGuidance("Lexical Resource",
                    "Mở bài: dùng từ diễn đạt lại chủ đề chính xác, không sao chép máy móc.",
                    "Thân bài: từ vựng đủ đa dạng, chính xác về nghĩa và kết hợp từ; tránh lặp không cần thiết.",
                    "Kết bài: lựa chọn từ ngữ tổng kết/diễn đạt xu hướng hoặc lập trường tự nhiên.",
                    "5: vốn từ còn hạn chế, lỗi lựa chọn từ có thể gây khó hiểu; 6: đủ dùng cho yêu cầu nhưng độ chính xác/đa dạng chưa ổn định; "
                            + "7: vốn từ khá linh hoạt, có nhận thức về sắc thái và lỗi ít cản trở; "
                            + "8: rộng và chính xác, dùng kết hợp từ tự nhiên, lỗi hiếm.");
            case "grammarScore" -> new CriterionGuidance("Grammatical Range & Accuracy",
                    "Mở bài: cấu trúc câu diễn đạt đúng trọng tâm và kiểm soát được câu phức.",
                    "Thân bài: kết hợp cấu trúc đơn/phức phù hợp; kiểm tra mệnh đề, hòa hợp, thì và dấu câu.",
                    "Kết bài: câu tổng kết rõ, đúng ngữ pháp và không làm thay đổi ý.",
                    "5: cấu trúc lặp, lỗi ngữ pháp thường xuyên; 6: có câu phức nhưng phạm vi/độ chính xác chưa đồng đều; "
                            + "7: dùng đa dạng cấu trúc, phần lớn câu không lỗi; 8: phạm vi cấu trúc rộng, kiểm soát tốt và lỗi chỉ thỉnh thoảng.");
            default -> throw new IllegalArgumentException("Unknown rubric criterion: " + fieldName);
        };
    }

    private static boolean isScoreField(String name) {
        return name.equals("taskScore") || name.equals("coherenceScore")
                || name.equals("lexicalScore") || name.equals("grammarScore");
    }

    public record RubricField(String fieldName, RubricCriterion annotation) {
    }

    public record CriterionGuidance(String displayName, String openingIndicator,
                                    String bodyIndicator, String conclusionIndicator,
                                    String descriptorAnchors) {
    }
}
