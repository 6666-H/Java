package com.study.english.service;

import com.study.english.entity.StudentWordProgress;
import com.study.english.mapper.StudentWordProgressMapper;
import com.study.english.service.impl.StudentWordProgressServiceImpl;
import org.springframework.test.util.ReflectionTestUtils;
import com.study.english.service.ErrorLogService;
import com.study.english.service.StudentUnitModeService;
import com.study.english.service.StudentWordModeProgressService;
import com.study.english.service.UnitService;
import com.study.english.service.UserService;
import com.study.english.service.WordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.*;

/**
 * 学生学习进度服务测试
 */
@ExtendWith(MockitoExtension.class)
class StudentWordProgressServiceTest {

    @Mock
    private StudentWordProgressMapper progressMapper;

    @Mock
    private WordService wordService;

    @Mock
    private UnitService unitService;

    @Mock
    private ErrorLogService errorLogService;

    @Mock
    private UserService userService;

    @Mock
    private StudentUnitModeService unitModeService;

    @Mock
    private StudentWordModeProgressService modeProgressService;

    @InjectMocks
    private StudentWordProgressServiceImpl progressService;

    private String tenantId;
    private Long userId;
    private Long wordId;

    @BeforeEach
    void setUp() {
        tenantId = "tenant-001";
        userId = 1L;
        wordId = 100L;
        ReflectionTestUtils.setField(progressService, "baseMapper", progressMapper);
    }

    @Test
    void testSubmitStudyFeedback_WhenCorrect_ShouldIncreaseMastery() {
        // Given
        StudentWordProgress existing = new StudentWordProgress();
        existing.setId(1L);
        existing.setTenantId(tenantId);
        existing.setUserId(userId);
        existing.setWordId(wordId);
        existing.setMasteryLevel(2);
        existing.setCorrectCount(1);

        doReturn(existing).when(progressMapper).selectOne(any(), eq(true));
        when(progressMapper.updateById(any())).thenReturn(1);

        // When
        progressService.submitStudyFeedback(tenantId, userId, wordId, "CORRECT", null, "FLASHCARD");

        // Then
        verify(progressMapper, times(1)).updateById(any(StudentWordProgress.class));
    }

    @Test
    void testSubmitStudyFeedback_WhenWrong_ShouldRecordError() {
        // Given
        StudentWordProgress existing = new StudentWordProgress();
        existing.setId(1L);
        existing.setTenantId(tenantId);
        existing.setUserId(userId);
        existing.setWordId(wordId);
        existing.setMasteryLevel(2);
        existing.setWrongCount(0);

        doReturn(existing).when(progressMapper).selectOne(any(), eq(true));
        when(progressMapper.updateById(any())).thenReturn(1);

        // When
        progressService.submitStudyFeedback(tenantId, userId, wordId, "WRONG", "DONT_KNOW", "FLASHCARD");

        // Then
        verify(progressMapper, times(1)).updateById(any(StudentWordProgress.class));
    }

    @Test
    void testSubmitStudyFeedback_WhenNewWord_ShouldCreateProgress() {
        // Given
        doReturn(null).when(progressMapper).selectOne(any(), eq(true));
        when(progressMapper.insert(any())).thenReturn(1);

        // When
        progressService.submitStudyFeedback(tenantId, userId, wordId, "CORRECT", null, "FLASHCARD");

        // Then
        verify(progressMapper, times(1)).insert(any(StudentWordProgress.class));
    }

    @Test
    void testSubmitStudyFeedback_WhenInvalidTenantId_ShouldThrowException() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            progressService.submitStudyFeedback(null, userId, wordId, "CORRECT", null, "FLASHCARD");
        });
    }

    @Test
    void testSubmitStudyFeedback_WhenInvalidUserId_ShouldThrowException() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            progressService.submitStudyFeedback(tenantId, null, wordId, "CORRECT", null, "FLASHCARD");
        });
    }

    @Test
    void testSubmitStudyFeedback_WhenInvalidWordId_ShouldThrowException() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            progressService.submitStudyFeedback(tenantId, userId, null, "CORRECT", null, "FLASHCARD");
        });
    }
}
