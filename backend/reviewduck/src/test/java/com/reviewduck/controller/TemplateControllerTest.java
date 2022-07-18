package com.reviewduck.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reviewduck.dto.request.QuestionRequest;
import com.reviewduck.dto.request.TemplateCreateRequest;
import com.reviewduck.service.TemplateService;

@WebMvcTest(TemplateController.class)
public class TemplateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TemplateService TemplateService;

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("템플릿 생성시 회고 제목에 빈 값이 들어갈 경우 예외가 발생한다.")
    void createWithEmptyReviewTitle(String title) throws Exception {
        // given
        TemplateCreateRequest request = new TemplateCreateRequest(title, "description", List.of());

        // when, then
        assertBadRequestFromPost("/api/templates", request, "템플릿의 제목은 비어있을 수 없습니다.");
    }

    @ParameterizedTest
    @NullSource
    @DisplayName("템플릿 생성시 회고 질문 목록에 null 값이 들어갈 경우 예외가 발생한다.")
    void createWithNullQuestionList(List<QuestionRequest> questions) throws Exception {
        // given
        TemplateCreateRequest request = new TemplateCreateRequest("title", "description", questions);

        // when, then
        assertBadRequestFromPost("/api/templates", request, "템플릿의 질문 목록 생성 중 오류가 발생했습니다.");
    }

    private void assertBadRequestFromPost(String uri, Object request, String errorMessage) throws Exception {
        mockMvc.perform(post(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
            ).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", containsString(errorMessage)));
    }
}
