package com.reviewduck.review.domain;

import static lombok.AccessLevel.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.reviewduck.common.domain.BaseDate;
import com.reviewduck.member.domain.Member;
import com.reviewduck.review.dto.service.QuestionAnswerCreateDto;
import com.reviewduck.review.dto.service.QuestionAnswerUpdateDto;
import com.reviewduck.review.exception.ReviewException;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = PROTECTED)
@Getter
public class Review extends BaseDate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    private ReviewForm reviewForm;

    private boolean isPrivate;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "review", fetch = FetchType.LAZY)
    @OrderBy("position asc")
    @JsonIgnore
    private final List<QuestionAnswer> questionAnswers = new ArrayList<>();

    @Column(nullable = false)
    private int likes;

    public Review(String title, Member member, ReviewForm reviewForm, List<QuestionAnswerCreateDto> createDtos,
        boolean isPrivate) {
        validate(title);
        this.title = title;
        this.member = member;
        this.reviewForm = reviewForm;
        this.isPrivate = isPrivate;
        this.questionAnswers.addAll(createQuestionAnswers(createDtos));
        sortQuestionAnswers();
    }

    public void update(boolean isPrivate, List<QuestionAnswerUpdateDto> updateDtos) {
        int oldSize = this.questionAnswers.size();
        this.questionAnswers.addAll(updateQuestionAnswers(updateDtos));
        this.questionAnswers.subList(0, oldSize).clear();

        this.isPrivate = isPrivate;
        sortQuestionAnswers();
    }

    public boolean isMine(Member member) {
        return this.member.equals(member);
    }

    public int like(int likeCount) {
        likes += likeCount;
        return likes;
    }

    private void validate(String title) {
        if (Objects.isNull(title) || title.isBlank()) {
            throw new ReviewException("회고의 제목은 비어있을 수 없습니다.");
        }
    }

    private List<QuestionAnswer> createQuestionAnswers(List<QuestionAnswerCreateDto> createDtos) {
        return createDtos.stream()
            .map(dto -> new QuestionAnswer(dto.getReviewFormQuestion(), dto.getAnswer(), this))
            .collect(Collectors.toUnmodifiableList());
    }

    private List<QuestionAnswer> updateQuestionAnswers(List<QuestionAnswerUpdateDto> updateDtos) {
        return updateDtos.stream()
            .map(this::updateQuestionAnswer)
            .collect(Collectors.toUnmodifiableList());
    }

    private QuestionAnswer updateQuestionAnswer(QuestionAnswerUpdateDto updateDto) {
        QuestionAnswer questionAnswer = questionAnswers.stream()
            .filter(it -> it.getReviewFormQuestion().equals(updateDto.getReviewFormQuestion()))
            .findFirst()
            .orElseGet(() ->
                new QuestionAnswer(updateDto.getReviewFormQuestion(), new Answer(updateDto.getAnswerValue()), this));

        questionAnswer.setAnswerValue(updateDto.getAnswerValue());
        return questionAnswer;
    }

    private void sortQuestionAnswers() {
        int index = 0;
        for (QuestionAnswer questionAnswer : questionAnswers) {
            questionAnswer.setPosition(index++);
        }
    }
}
