package com.reviewduck.review.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;

import com.reviewduck.auth.exception.AuthorizationException;
import com.reviewduck.common.exception.NotFoundException;
import com.reviewduck.member.domain.Member;
import com.reviewduck.member.service.MemberService;
import com.reviewduck.review.domain.ReviewForm;
import com.reviewduck.review.domain.ReviewFormQuestion;
import com.reviewduck.review.dto.request.ReviewFormCreateRequest;
import com.reviewduck.review.dto.request.ReviewFormQuestionCreateRequest;
import com.reviewduck.review.dto.request.ReviewFormQuestionUpdateRequest;
import com.reviewduck.review.dto.request.ReviewFormUpdateRequest;
import com.reviewduck.template.domain.Template;
import com.reviewduck.template.dto.request.TemplateCreateRequest;
import com.reviewduck.template.dto.request.TemplateQuestionCreateRequest;
import com.reviewduck.template.service.TemplateService;

@SpringBootTest
@Sql("classpath:truncate.sql")
@Transactional
public class ReviewFormServiceTest {

    private final String invalidCode = "aaaaaaaa";

    @Autowired
    private ReviewFormService reviewFormService;

    @Autowired
    private TemplateService templateService;

    @Autowired
    private MemberService memberService;

    private Member member1;
    private Member member2;

    @BeforeEach
    void setUp() {
        Member tempMember1 = new Member("1", "jason", "제이슨", "testUrl1");
        member1 = memberService.save(tempMember1);

        Member tempMember2 = new Member("2", "woni", "워니", "testUrl2");
        member2 = memberService.save(tempMember2);
    }

    @Nested
    @DisplayName("회고 폼 생성")
    class saveReviewForm {

        @Test
        @DisplayName("회고 폼을 생성한다.")
        void saveReviewForm() {
            // given
            String reviewFormTitle = "title";
            List<ReviewFormQuestionCreateRequest> questions = List.of(
                new ReviewFormQuestionCreateRequest("question1", "description1"),
                new ReviewFormQuestionCreateRequest("question2", "description2"));

            ReviewFormCreateRequest createRequest = new ReviewFormCreateRequest(reviewFormTitle, questions);

            List<ReviewFormQuestion> expected = questions.stream()
                .map(questionRequest -> new ReviewFormQuestion(
                    questionRequest.getValue(),
                    questionRequest.getDescription()))
                .collect(Collectors.toUnmodifiableList());

            int index = 0;
            for (ReviewFormQuestion reviewFormQuestion : expected) {
                reviewFormQuestion.setPosition(index++);
            }

            // when
            ReviewForm reviewForm = reviewFormService.save(member1, createRequest);

            // then
            assertAll(
                () -> assertThat(reviewForm).isNotNull(),
                () -> assertThat(reviewForm.getId()).isNotNull(),
                () -> assertThat(reviewForm.getMember().getNickname()).isEqualTo("제이슨"),
                () -> assertThat(reviewForm.getCode().length()).isEqualTo(8),
                () -> assertThat(reviewForm.getTitle()).isEqualTo(reviewFormTitle),
                () -> assertThat(reviewForm.getReviewFormQuestions())
                    .usingRecursiveComparison()
                    .ignoringFields("id")
                    .isEqualTo(expected)
            );
        }

    }

    @Nested
    @DisplayName("템플릿 기반 회고폼 생성")
    class saveReviewFormByTemplate {

        @Test
        @DisplayName("템플릿과 동일한 모양의 회고 폼을 생성한다.")
        void saveFromTemplate_Same() {
            // when
            // 템플릿 생성
            String templateTitle = "title";
            String templateDescription = "description";
            List<TemplateQuestionCreateRequest> questions = List.of(
                new TemplateQuestionCreateRequest("question1", "description1"),
                new TemplateQuestionCreateRequest("question2", "description2"));

            TemplateCreateRequest templateRequest = new TemplateCreateRequest(templateTitle, templateDescription,
                questions);
            Template savedTemplate = templateService.save(member1, templateRequest);

            // 템플릿 기반 회고 폼 생성
            ReviewForm savedReviewForm = reviewFormService.saveFromTemplate(member1, savedTemplate.getId());

            List<ReviewFormQuestion> expected = questions.stream()
                .map(question -> new ReviewFormQuestion(question.getValue(), question.getDescription()))
                .collect(Collectors.toUnmodifiableList());

            int index = 0;
            for (ReviewFormQuestion reviewFormQuestion : expected) {
                reviewFormQuestion.setPosition(index++);
            }

            // then
            assertAll(
                // save Review Form
                () -> assertThat(savedReviewForm).isNotNull(),
                () -> assertThat(savedReviewForm.getId()).isNotNull(),
                () -> assertThat(savedReviewForm.getMember().getNickname()).isEqualTo("제이슨"),
                () -> assertThat(savedReviewForm.getCode().length()).isEqualTo(8),
                () -> assertThat(savedReviewForm.getTitle()).isEqualTo(templateTitle),
                () -> assertThat(savedReviewForm.getReviewFormQuestions())
                    .usingRecursiveComparison()
                    .ignoringFields("id")
                    .isEqualTo(expected),
                // template usedCount ++
                () -> assertThat(savedTemplate.getUsedCount()).isEqualTo(1)
            );

        }

        @Test
        @DisplayName("템플릿과 동일하지 않은 모양의 회고 폼을 생성한다.")
        void saveFromTemplate_Different() {
            // given
            // 템플릿 생성
            String templateTitle = "title";
            String templateDescription = "description";
            List<TemplateQuestionCreateRequest> templateQuestions = List.of(
                new TemplateQuestionCreateRequest("question1", "description1"),
                new TemplateQuestionCreateRequest("question2", "description2"));

            TemplateCreateRequest templateRequest = new TemplateCreateRequest(templateTitle, templateDescription,
                templateQuestions);
            Template savedTemplate = templateService.save(member1, templateRequest);

            String reviewFormTitle = "title";
            List<ReviewFormQuestionCreateRequest> reviewFromQuestions = List.of(
                new ReviewFormQuestionCreateRequest("question3", "description3"),
                new ReviewFormQuestionCreateRequest("question4", "description4"));

            List<ReviewFormQuestion> expected = reviewFromQuestions.stream()
                .map(request -> new ReviewFormQuestion(
                    request.getValue(),
                    request.getDescription()))
                .collect(Collectors.toUnmodifiableList());

            int index = 0;
            for (ReviewFormQuestion reviewFormQuestion : expected) {
                reviewFormQuestion.setPosition(index++);
            }

            // when
            ReviewFormCreateRequest createRequest = new ReviewFormCreateRequest(reviewFormTitle, reviewFromQuestions);
            ReviewForm createdReviewForm = reviewFormService.saveFromTemplate(member1, savedTemplate.getId(),
                createRequest);

            // then
            assertAll(
                // save reviewForm
                () -> assertThat(createdReviewForm).isNotNull(),
                () -> assertThat(createdReviewForm.getId()).isNotNull(),
                () -> assertThat(createdReviewForm.getMember().getNickname()).isEqualTo("제이슨"),
                () -> assertThat(createdReviewForm.getCode().length()).isEqualTo(8),
                () -> assertThat(createdReviewForm.getTitle()).isEqualTo(reviewFormTitle),
                () -> assertThat(createdReviewForm.getReviewFormQuestions())
                    .usingRecursiveComparison()
                    .ignoringFields("id")
                    .isEqualTo(expected),
                // usedCount ++
                () -> assertThat(savedTemplate.getUsedCount()).isEqualTo(1)
            );
        }

    }

    @Nested
    @DisplayName("회고 폼 코드로 회고 폼 조회")
    class findByCode {

        @Test
        @DisplayName("회고 폼 코드로 회고 폼을 조회한다.")
        void findReviewForm() {
            // given
            ReviewForm expected = saveReviewForm(member1);

            // when
            ReviewForm actual = reviewFormService.findByCode(expected.getCode());

            // then
            assertThat(expected).isSameAs(actual);
        }

        @Test
        @DisplayName("존재하지 않는 코드로 조회할 수 없다.")
        void findReviewFormByInvalidCode() {
            // when, then
            assertThatThrownBy(() -> reviewFormService.findByCode(invalidCode))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("존재하지 않는 회고 폼입니다.");
        }

    }

    @Nested
    @DisplayName("자신이 작성한 회고 조회")
    class findByMember {

        @Test
        @DisplayName("자신이 작성한 회고를 조회한다.")
        void findMyReviewForms() {
            String reviewFormTitle1 = "title1";
            List<ReviewFormQuestionCreateRequest> questions1 = List.of(
                new ReviewFormQuestionCreateRequest("question1", "description1"),
                new ReviewFormQuestionCreateRequest("question2", "description2"));

            ReviewFormCreateRequest createRequest1 = new ReviewFormCreateRequest(reviewFormTitle1, questions1);

            String reviewFormTitle2 = "title2";
            List<ReviewFormQuestionCreateRequest> questions2 = List.of(
                new ReviewFormQuestionCreateRequest("question3", "description3"),
                new ReviewFormQuestionCreateRequest("question4", "description4"));

            ReviewFormCreateRequest createRequest2 = new ReviewFormCreateRequest(reviewFormTitle2, questions2);

            List<ReviewFormQuestion> expected = questions1.stream()
                .map(questionRequest -> new ReviewFormQuestion(questionRequest.getValue(),
                    questionRequest.getDescription()))
                .collect(Collectors.toUnmodifiableList());

            int index = 0;
            for (ReviewFormQuestion reviewFormQuestion : expected) {
                reviewFormQuestion.setPosition(index++);
            }

            // when
            reviewFormService.save(member1, createRequest1);
            reviewFormService.save(member2, createRequest2);

            List<ReviewForm> myReviewForms = reviewFormService.findByMember(member1);

            // then
            assertAll(
                () -> assertThat(myReviewForms).hasSize(1),
                () -> assertThat(myReviewForms.get(0)).isNotNull(),
                () -> assertThat(myReviewForms.get(0).getMember().getNickname()).isEqualTo("제이슨"),
                () -> assertThat(myReviewForms.get(0).getId()).isNotNull(),
                () -> assertThat(myReviewForms.get(0).getCode().length()).isEqualTo(8),
                () -> assertThat(myReviewForms.get(0).getTitle()).isEqualTo(reviewFormTitle1),
                () -> assertThat(myReviewForms.get(0).getReviewFormQuestions())
                    .usingRecursiveComparison()
                    .ignoringFields("id")
                    .isEqualTo(expected)
            );
        }

    }

    @Nested
    @DisplayName("회고 폼 수정")
    class updateReviewForm {

        @Test
        @DisplayName("회고 폼을 수정한다.")
        void updateReviewForm() {
            // given
            ReviewForm savedReviewForm = saveReviewForm(member1);
            String code = savedReviewForm.getCode();
            Long questionId = savedReviewForm.getReviewFormQuestions().get(0).getId();

            // when
            String reviewFormTitle = "new title";
            List<ReviewFormQuestionUpdateRequest> updateRequests = List.of(
                new ReviewFormQuestionUpdateRequest(questionId, "new question1", "new description1"),
                new ReviewFormQuestionUpdateRequest(null, "new question3", "new description3"));

            ReviewFormUpdateRequest updateRequest = new ReviewFormUpdateRequest(reviewFormTitle, updateRequests);

            List<ReviewFormQuestion> expected = updateRequests.stream()
                .map(questionRequest -> new ReviewFormQuestion(questionRequest.getValue(),
                    questionRequest.getDescription()))
                .collect(Collectors.toUnmodifiableList());

            int index = 0;
            for (ReviewFormQuestion reviewFormQuestion : expected) {
                reviewFormQuestion.setPosition(index++);
            }

            reviewFormService.update(member1, code, updateRequest);
            ReviewForm foundReviewForm = reviewFormService.findByCode(code);

            assertAll(
                () -> assertThat(foundReviewForm).isNotNull(),
                () -> assertThat(foundReviewForm.getId()).isNotNull(),
                () -> assertThat(foundReviewForm.getMember().getNickname()).isEqualTo("제이슨"),
                () -> assertThat(foundReviewForm.getCode().length()).isEqualTo(8),
                () -> assertThat(foundReviewForm.getTitle()).isEqualTo(reviewFormTitle),
                () -> assertThat(foundReviewForm.getReviewFormQuestions())
                    .usingRecursiveComparison()
                    .ignoringFields("id")
                    .isEqualTo(expected)
            );
        }

        @Test
        @DisplayName("본인이 생성한 회고 폼이 아니면 수정할 수 없다.")
        void updateNotMyReviewForm() {
            // given
            ReviewForm savedReviewForm = saveReviewForm(member1);
            String code = savedReviewForm.getCode();
            Long questionId = savedReviewForm.getReviewFormQuestions().get(0).getId();

            // when
            String reviewFormTitle = "new title";
            List<ReviewFormQuestionUpdateRequest> updateRequests = List.of(
                new ReviewFormQuestionUpdateRequest(questionId, "new question1", "new description1"),
                new ReviewFormQuestionUpdateRequest(null, "new question3", "new description3"));

            ReviewFormUpdateRequest updateRequest = new ReviewFormUpdateRequest(reviewFormTitle, updateRequests);

            List<ReviewFormQuestion> expected = updateRequests.stream()
                .map(questionRequest -> new ReviewFormQuestion(questionRequest.getValue(), ""))
                .collect(Collectors.toUnmodifiableList());

            int index = 0;
            for (ReviewFormQuestion reviewFormQuestion : expected) {
                reviewFormQuestion.setPosition(index++);
            }

            assertThatThrownBy(() -> reviewFormService.update(member2, code, updateRequest))
                .isInstanceOf(AuthorizationException.class)
                .hasMessageContaining("본인이 생성한 회고 폼이 아니면 수정할 수 없습니다.");
        }

        @Test
        @DisplayName("존재하지 않는 회고 폼을 수정할 수 없다.")
        void updateReviewFormByInvalidCode() {
            // when
            List<ReviewFormQuestionUpdateRequest> updateRequests = List.of(
                new ReviewFormQuestionUpdateRequest(1L, "new question1", "new description1"),
                new ReviewFormQuestionUpdateRequest(null, "new question3", "new description3"),
                new ReviewFormQuestionUpdateRequest(2L, "new question2", "new description2"));

            ReviewFormUpdateRequest reviewFormUpdateRequest = new ReviewFormUpdateRequest("new title", updateRequests);

            assertThatThrownBy(() -> reviewFormService.update(member1, invalidCode, reviewFormUpdateRequest))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("존재하지 않는 회고 폼입니다.");
        }

        @Test
        @DisplayName("존재하지 않는 질문을 수정할 수 없다.")
        void updateReviewFormByInvalidQuestionId() {
            // given
            String code = saveReviewForm(member1).getCode();

            // when, then
            List<ReviewFormQuestionUpdateRequest> updateRequests = List.of(
                new ReviewFormQuestionUpdateRequest(9999999L, "new question", "new description"));

            assertThatThrownBy(
                () -> reviewFormService.update(member1, code, new ReviewFormUpdateRequest("new title", updateRequests)))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("존재하지 않는 질문입니다.");

        }

    }

    @Nested
    @DisplayName("회고 폼 코드로 회고 폼 삭제")
    class deleteByCode {

        @Test
        @DisplayName("회고 폼을 삭제한다.")
        void deleteReviewForm() {
            // given
            ReviewForm savedReviewForm = saveReviewForm(member1);
            String code = savedReviewForm.getCode();

            // when
            reviewFormService.deleteByCode(member1, code);

            // then
            assertThatThrownBy(() -> reviewFormService.findByCode(code))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("존재하지 않는 회고 폼입니다.");
        }

        @Test
        @DisplayName("본인이 생성한 회고 폼이 아니면 삭제할 수 없다.")
        void deleteNotMyReviewForm() {
            // given
            ReviewForm savedReviewForm = saveReviewForm(member1);
            String code = savedReviewForm.getCode();

            // when, then
            assertThatThrownBy(() -> reviewFormService.deleteByCode(member2, code))
                .isInstanceOf(AuthorizationException.class)
                .hasMessageContaining("본인이 생성한 회고 폼이 아니면 삭제할 수 없습니다.");
        }

        @Test
        @DisplayName("존재하지 않는 회고 폼을 삭제할 수 없다.")
        void deleteReviewFormByInvalidCode() {
            // when, then
            assertThatThrownBy(() -> reviewFormService.deleteByCode(member1, invalidCode))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("존재하지 않는 회고 폼입니다.");
        }

    }

    private ReviewForm saveReviewForm(Member member) {
        List<ReviewFormQuestionCreateRequest> createRequests = List.of(
            new ReviewFormQuestionCreateRequest("question1", "description1"),
            new ReviewFormQuestionCreateRequest("question2", "description2"));

        ReviewFormCreateRequest createRequest = new ReviewFormCreateRequest("title", createRequests);

        return reviewFormService.save(member, createRequest);
    }
}
