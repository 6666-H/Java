package com.study.english.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.study.english.context.TenantContext;
import com.study.english.dto.StudySubmitResult;
import com.study.english.entity.Word;
import com.study.english.exception.GlobalExceptionHandler;
import com.study.english.service.ErrorLogService;
import com.study.english.service.ErrorReviewCompleteService;
import com.study.english.service.ProductStudyService;
import com.study.english.service.StudentUnitModeService;
import com.study.english.service.StudentWordModeProgressService;
import com.study.english.service.StudentWordProgressService;
import com.study.english.service.StudyLogService;
import com.study.english.service.WordService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class StudyControllerTest {

    @Mock private StudentWordProgressService progressService;
    @Mock private ErrorLogService errorLogService;
    @Mock private ErrorReviewCompleteService errorReviewCompleteService;
    @Mock private WordService wordService;
    @Mock private StudentUnitModeService unitModeService;
    @Mock private StudyLogService studyLogService;
    @Mock private StudentWordModeProgressService modeProgressService;
    @Mock private ProductStudyService productStudyService;

    @InjectMocks
    private StudyController studyController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(studyController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        TenantContext.set("tenant-001", 1L, "student01", "STUDENT");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void testGetUnitWords_ShouldReturnWordList() throws Exception {
        Word word1 = createWord(1L, "apple", "苹果");
        Word word2 = createWord(2L, "banana", "香蕉");
        when(wordService.list(any(Wrapper.class))).thenReturn(List.of(word1, word2));

        mockMvc.perform(get("/api/study/unit_words").param("unitId", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].word").value("apple"));
    }

    @Test
    void testSubmitStudyFeedback_ShouldReturnOk() throws Exception {
        StudySubmitResult result = new StudySubmitResult();
        result.setWordCompleted(true);
        when(progressService.submitStudyFeedback(anyString(), anyLong(), anyLong(), anyString(), any(), anyString()))
                .thenReturn(result);

        Map<String, Object> request = Map.of(
                "wordId", 1L,
                "feedbackType", "KNOW",
                "mode", "FLASHCARD"
        );

        mockMvc.perform(post("/api/study/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));
    }

    @Test
    void testSubmitStudyFeedback_WhenMissingWordId_ShouldReturnError() throws Exception {
        Map<String, Object> request = Map.of("feedbackType", "KNOW");

                mockMvc.perform(post("/api/study/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("BUSINESS_ERROR"));
    }

    @Test
    void testGetRecentErrors_ShouldReturnErrorWords() throws Exception {
        when(progressService.getRecentErrorWords(anyString(), anyLong(), eq(7), eq(50)))
                .thenReturn(List.of(createWord(1L, "difficult", "困难的")));

        mockMvc.perform(get("/api/study/recent_errors").param("days", "7").param("limit", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"))
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    private Word createWord(Long id, String word, String meaning) {
        Word item = new Word();
        item.setId(id);
        item.setWord(word);
        item.setMeaning(meaning);
        return item;
    }
}
