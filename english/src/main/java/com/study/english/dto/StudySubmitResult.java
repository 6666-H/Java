package com.study.english.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudySubmitResult {
    private boolean wordCompleted;
    private boolean stillWeak;
    private int modeStreak;
    private int reviewIntervalHours;
}
