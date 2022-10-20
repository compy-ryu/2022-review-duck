package com.reviewduck.review.dto.controller.response;

import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Collectors;

import com.reviewduck.member.dto.response.MemberResponse;
import com.reviewduck.review.domain.ReviewForm;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ReviewFormResponse {

    private String reviewFormTitle;
    private long updatedAt;
    private CreatorResponse creator;
    private boolean isCreator;
    private List<ReviewFormQuestionResponse> questions;
    private List<CreatorResponse> participants;

    public static ReviewFormResponse of(ReviewForm reviewForm, boolean isMine, List<MemberResponse> participants) {
        List<ReviewFormQuestionResponse> reviewFormQuestionResponse = reviewForm.getQuestions().stream()
            .map(ReviewFormQuestionResponse::from)
            .collect(Collectors.toUnmodifiableList());

        List<CreatorResponse> participantsResponse = participants.stream()
            .map(CreatorResponse::from)
            .collect(Collectors.toUnmodifiableList());

        return new ReviewFormResponse(
            reviewForm.getTitle(),
            Timestamp.valueOf(reviewForm.getUpdatedAt()).getTime(),
            CreatorResponse.from(MemberResponse.from(reviewForm.getMember())),
            isMine,
            reviewFormQuestionResponse,
            participantsResponse
        );
    }

    public boolean getIsCreator() {
        return isCreator;
    }
}
