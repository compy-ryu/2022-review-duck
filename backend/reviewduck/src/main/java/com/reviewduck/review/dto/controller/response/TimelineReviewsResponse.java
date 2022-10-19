package com.reviewduck.review.dto.controller.response;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;

import com.reviewduck.member.domain.Member;
import com.reviewduck.member.dto.response.MemberDto;
import com.reviewduck.review.domain.Review;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class TimelineReviewsResponse {

    private long numberOfReviews;
    private boolean isLastPage;
    private List<ReviewResponse> reviews;

    public static TimelineReviewsResponse of(Page<Review> reviews, MemberDto member) {
        List<ReviewResponse> reviewResponses = reviews.getContent().stream()
            .map(review -> ReviewResponse.of(member, review))
            .collect(Collectors.toUnmodifiableList());

        return new TimelineReviewsResponse(reviews.getTotalElements(), reviews.isLast(), reviewResponses);
    }

    public boolean getIsLastPage() {
        return isLastPage;
    }

}
