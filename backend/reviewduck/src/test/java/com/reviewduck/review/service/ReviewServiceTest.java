package com.reviewduck.review.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

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
import com.reviewduck.review.domain.Review;
import com.reviewduck.review.domain.ReviewForm;
import com.reviewduck.review.dto.request.AnswerCreateRequest;
import com.reviewduck.review.dto.request.AnswerUpdateRequest;
import com.reviewduck.review.dto.request.ReviewContentCreateRequest;
import com.reviewduck.review.dto.request.ReviewContentUpdateRequest;
import com.reviewduck.review.dto.request.ReviewCreateRequest;
import com.reviewduck.review.dto.request.ReviewFormCreateRequest;
import com.reviewduck.review.dto.request.ReviewFormQuestionCreateRequest;
import com.reviewduck.review.dto.request.ReviewUpdateRequest;

@SpringBootTest
@Sql("classpath:truncate.sql")
@Transactional
public class ReviewServiceTest {

    private static final String invalidCode = "aaaaaaaa";

    @Autowired
    private ReviewFormService reviewFormService;
    @Autowired
    private ReviewService reviewService;
    @Autowired
    private MemberService memberService;

    private ReviewForm reviewForm;
    private Member member1;
    private Member member2;

    @BeforeEach
    void setUp() {
        Member tempMember1 = new Member("1", "jason", "제이슨", "testUrl1");
        member1 = memberService.save(tempMember1);

        Member tempMember2 = new Member("2", "woni", "워니", "testUrl2");
        member2 = memberService.save(tempMember2);

        String reviewTitle = "title";
        List<ReviewFormQuestionCreateRequest> questions = List.of(
            new ReviewFormQuestionCreateRequest("question1", "description1"),
            new ReviewFormQuestionCreateRequest("question2", "description2"));
        ReviewFormCreateRequest createRequest = new ReviewFormCreateRequest(reviewTitle, questions);

        this.reviewForm = reviewFormService.save(member1, createRequest);
    }

    @Nested
    @DisplayName("회고 저장")
    class saveReview {

        @Test
        @DisplayName("회고를 저장한다.")
        void saveReview() {
            // given
            long questionId1 = reviewForm.getReviewFormQuestions().get(0).getId();
            long questionId2 = reviewForm.getReviewFormQuestions().get(1).getId();

            ReviewCreateRequest reviewCreateRequest = new ReviewCreateRequest(false, List.of(
                new ReviewContentCreateRequest(questionId1, new AnswerCreateRequest("answer1")),
                new ReviewContentCreateRequest(questionId2, new AnswerCreateRequest("answer2"))
            ));

            // when
            Review savedReview = reviewService.save(member1, reviewForm.getCode(), reviewCreateRequest);

            // then
            assertAll(
                () -> assertThat(savedReview.getId()).isNotNull(),
                () -> assertThat(savedReview.getMember().getNickname()).isEqualTo("제이슨"),
                () -> assertThat(savedReview.getQuestionAnswers().get(0).getAnswer().getValue())
                    .isEqualTo("answer1"),
                () -> assertThat(savedReview.getQuestionAnswers().get(0).getPosition())
                    .isEqualTo(0)
            );
        }

        @Test
        @DisplayName("유효하지 않은 입장 코드로 저장할 수 없다.")
        void withInvalidCode() {
            // given
            ReviewCreateRequest reviewCreateRequest = new ReviewCreateRequest(false, List.of(
                new ReviewContentCreateRequest(1L, new AnswerCreateRequest("answer1")),
                new ReviewContentCreateRequest(2L, new AnswerCreateRequest("answer2"))
            ));

            // when, then
            assertThatThrownBy(() -> reviewService.save(member1, invalidCode, reviewCreateRequest))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("존재하지 않는 회고 폼입니다.");
        }

        @Test
        @DisplayName("유효하지 않은 질문 번호로 저장할 수 없다.")
        void withInvalidQuestionId() {
            //given
            ReviewCreateRequest reviewCreateRequest = new ReviewCreateRequest(false, List.of(
                new ReviewContentCreateRequest(9999L, new AnswerCreateRequest("answer1")),
                new ReviewContentCreateRequest(2L, new AnswerCreateRequest("answer2"))
            ));

            // when, then
            assertThatThrownBy(() -> reviewService.save(member1, reviewForm.getCode(), reviewCreateRequest))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("존재하지 않는 질문입니다.");
        }


    }
    @Nested
    @DisplayName("id로 회고 조회")
    class findById {

        @Test
        @DisplayName("id로 특정 회고를 조회한다.")
        void findById() throws InterruptedException {
            // given
            Review saved = saveReview(member1, false);

            // when
            Review actual = reviewService.findById(saved.getId());

            // then
            assertThat(actual).isEqualTo(saved);
        }

        @Test
        @DisplayName("존재하지 않는 id로 조회할 수 없다.")
        void invalidId() {
            // when, then
            assertThatThrownBy(() -> reviewService.findById(99999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("존재하지 않는 회고입니다.");
        }


    }
    @Nested
    @DisplayName("사용자가 생성한 회고 조회")
    class findMemberReview {

        @Test
        @DisplayName("자신이 작성한 회고 중 최신순으로 첫 페이지를 조회한다.")
        void findPageOfMyReviewsOrderByLatest() throws InterruptedException {
            // given
            saveReview(member1, false);
            saveReview(member1, true);
            Review review = saveReview(member1, false);

            // when
            Integer page = 0;
            Integer size = 3;
            List<Review> myReviews = reviewService.findBySocialId(member1.getSocialId(), member1, page, size)
                .getContent();

            // then
            assertAll(
                () -> assertThat(myReviews).hasSize(3),
                () -> assertThat(myReviews.get(0)).isNotNull(),
                () -> assertThat(myReviews.get(0).getMember().getNickname()).isEqualTo(member1.getNickname()),
                () -> assertThat(myReviews.get(0).getUpdatedAt()).isEqualTo(review.getUpdatedAt())
            );
        }

        @Test
        @DisplayName("타인이 작성한 회고 중 최신순으로 첫 페이지를 조회한다.")
        void findPageOfOtherReviewsOrderByUpdatedAtDesc() throws InterruptedException {
            // given
            saveReview(member1, false);
            saveReview(member1, true);
            Review review = saveReview(member1, false);

            // when
            Integer page = 0;
            Integer size = 3;
            List<Review> myReviews = reviewService.findBySocialId(member1.getSocialId(), member2, page, size)
                .getContent();

            // then
            assertAll(
                () -> assertThat(myReviews).hasSize(2),
                () -> assertThat(myReviews.get(0)).isNotNull(),
                () -> assertThat(myReviews.get(0).getMember().getNickname()).isEqualTo(member1.getNickname()),
                () -> assertThat(myReviews.get(0).getUpdatedAt()).isEqualTo(review.getUpdatedAt())
            );
        }

        @Test
        @DisplayName("특정 회고 폼을 삭제해도 자신이 생성한 회고를 조회할 수 있다.")
        void findPageOfReviewsByDeletedSpecificReviewForm() throws InterruptedException {
            // given
            Review savedReview = saveReview(member1, false);

            // when
            reviewFormService.deleteByCode(member1, reviewForm.getCode());
            Integer page = 0;
            Integer size = 3;
            List<Review> reviews = reviewService.findBySocialId(member1.getSocialId(), member1, page, size)
                .getContent();

            // then
            assertAll(
                () -> assertThat(reviews).hasSize(1),
                () -> assertThat(reviews.get(0).getMember().getNickname()).isEqualTo(
                    savedReview.getMember().getNickname())
            );
        }

    }
    @Nested
    @DisplayName("회고 폼 code로 회고 조회")
    class findByCode {

        @Test
        @DisplayName("특정 회고 폼을 기반으로 작성된 회고를 모두 조회한다.")
        void findByReviewFormCode() throws InterruptedException {
            // given
            Review savedReview = saveReview(member1, false);

            // when
            List<Review> reviews = reviewService.findAllByCode(reviewForm.getCode());

            // then
            assertAll(
                () -> assertThat(reviews).hasSize(1),
                () -> assertThat(reviews.get(0).getMember().getNickname()).isEqualTo(
                    savedReview.getMember().getNickname())
            );
        }

        @Test
        @DisplayName("존재하지 않는 회고 폼으로 조회할 수 없다.")
        void invalidCode() {
            // when, then
            assertThatThrownBy(() -> reviewService.findAllByCode("aaaaaa"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("존재하지 않는 회고 폼입니다.");
        }

    }
    @Nested
    @DisplayName("최신 순 회고 조회")
    class findTimelineReview {

        @Test
        @DisplayName("공개된 모든 회고 답변을 조회한다.")
        void findAllReviews() throws InterruptedException {
            // given
            saveReview(member1, false);
            saveReview(member1, true);
            saveReview(member2, false);

            // when
            List<Review> reviews = reviewService.findAllPublic();

            // then
            assertAll(
                () -> assertThat(reviews).hasSize(2),
                () -> assertThat(reviews.get(0).getId()).isEqualTo(3)
            );
        }

    }
    @Nested
    @DisplayName("회고 수정")
    class updateReview {

        @Test
        @DisplayName("회고를 수정한다.")
        void updateReview() throws InterruptedException {
            // given
            Review savedReview = saveReview(member1, false);

            // when
            ReviewUpdateRequest updateRequest = new ReviewUpdateRequest(false, List.of(
                new ReviewContentUpdateRequest(1L, new AnswerUpdateRequest(1L, "editedAnswer1")),
                new ReviewContentUpdateRequest(2L, new AnswerUpdateRequest(2L, "editedAnswer2"))
            ));

            Review updatedReview = reviewService.update(member1, savedReview.getId(), updateRequest);

            // then
            assertAll(
                () -> assertThat(updatedReview.getId()).isNotNull(),
                () -> assertThat(updatedReview.getMember().getNickname()).isEqualTo("제이슨"),
                () -> assertThat(updatedReview.getQuestionAnswers().get(0).getAnswer().getValue())
                    .isEqualTo("editedAnswer1")
            );
        }

        @Test
        @DisplayName("본인이 생성한 회고가 아니면 수정할 수 없다.")
        void notMine() throws InterruptedException {
            // given
            Review savedReview = saveReview(member1, false);

            // when
            ReviewUpdateRequest updateRequest = new ReviewUpdateRequest(false, List.of(
                new ReviewContentUpdateRequest(1L, new AnswerUpdateRequest(1L, "editedAnswer1")),
                new ReviewContentUpdateRequest(2L, new AnswerUpdateRequest(2L, "editedAnswer2"))
            ));

            // then
            assertThatThrownBy(() -> reviewService.update(member2, savedReview.getId(), updateRequest))
                .isInstanceOf(AuthorizationException.class)
                .hasMessageContaining("본인이 생성한 회고가 아니면 수정할 수 없습니다.");
        }

        @Test
        @DisplayName("존재하지 않는 회고는 수정할 수 없다.")
        void invalidId() {
            // given
            ReviewUpdateRequest updateRequest = new ReviewUpdateRequest(false, List.of(
                new ReviewContentUpdateRequest(1L, new AnswerUpdateRequest(1L, "editedAnswer1")),
                new ReviewContentUpdateRequest(2L, new AnswerUpdateRequest(2L, "editedAnswer2"))
            ));

            // when, then
            assertThatThrownBy(() -> reviewService.update(member1, 99999L, updateRequest))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("존재하지 않는 회고입니다.");
        }

        @Test
        @DisplayName("유효하지 않은 질문 번호로 수정할 수 없다.")
        void withInvalidQuestionId() throws InterruptedException {
            // given
            Review savedReview = saveReview(member1, false);

            ReviewUpdateRequest updateRequest = new ReviewUpdateRequest(false, List.of(
                new ReviewContentUpdateRequest(1L, new AnswerUpdateRequest(1L, "editedAnswer1")),
                new ReviewContentUpdateRequest(999L, new AnswerUpdateRequest(2L, "editedAnswer2"))
            ));

            // when, then
            assertThatThrownBy(() -> reviewService.update(member1, savedReview.getId(), updateRequest))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("존재하지 않는 질문입니다.");
        }

        @Test
        @DisplayName("유효하지 않은 답변 번호로 수정할 수 없다.")
        void withInvalidAnswerId() throws InterruptedException {
            // given
            Review savedReview = saveReview(member1, false);

            ReviewUpdateRequest updateRequest = new ReviewUpdateRequest(false, List.of(
                new ReviewContentUpdateRequest(1L, new AnswerUpdateRequest(1L, "editedAnswer1")),
                new ReviewContentUpdateRequest(2L, new AnswerUpdateRequest(9999L, "editedAnswer2"))
            ));

            // when, then
            assertThatThrownBy(() -> reviewService.update(member1, savedReview.getId(), updateRequest))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("존재하지 않는 답변 번호입니다.");
        }


    }
    @Nested
    @DisplayName("회고 삭제")
    class deleteReview {

        @Test
        @DisplayName("회고를 삭제한다.")
        void deleteReview() throws InterruptedException {
            // given
            Review savedReview = saveReview(member1, false);

            // when
            reviewService.delete(member1, savedReview.getId());

            // then
            assertThat(reviewService.findAllByCode(reviewForm.getCode())).hasSize(0);
        }

        @Test
        @DisplayName("본인이 생성한 회고가 아니면 삭제할 수 없다.")
        void notMine() throws InterruptedException {
            // given
            Review savedReview = saveReview(member1, false);

            // when, then
            assertThatThrownBy(() -> reviewService.delete(member2, savedReview.getId()))
                .isInstanceOf(AuthorizationException.class)
                .hasMessageContaining("본인이 생성한 회고가 아니면 삭제할 수 없습니다.");
        }

        @Test
        @DisplayName("존재하지 않는 회고는 삭제할 수 없다.")
        void invalidId() {
            // when, then
            assertThatThrownBy(() -> reviewService.delete(member1, 99999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("존재하지 않는 회고입니다.");
        }


    }
    private Review saveReview(Member member, boolean isPrivate) throws InterruptedException {
        Thread.sleep(1);

        ReviewCreateRequest reviewCreateRequest = new ReviewCreateRequest(isPrivate, List.of(
            new ReviewContentCreateRequest(
                reviewForm.getReviewFormQuestions().get(0).getId(),
                new AnswerCreateRequest("answer1")
            ),
            new ReviewContentCreateRequest(
                reviewForm.getReviewFormQuestions().get(1).getId(),
                new AnswerCreateRequest("answer2")
            )
        ));

        return reviewService.save(member, reviewForm.getCode(), reviewCreateRequest);
    }
}
