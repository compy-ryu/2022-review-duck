package com.reviewduck.admin.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.reviewduck.common.exception.NotFoundException;
import com.reviewduck.review.domain.ReviewForm;
import com.reviewduck.review.repository.ReviewFormRepository;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Transactional(readOnly = true)
public class AdminReviewFormService {

    private ReviewFormRepository reviewFormRepository;

    public List<ReviewForm> findAllReviewForms() {
        return reviewFormRepository.findAll();
    }

    @Transactional
    public void deleteReviewFormById(Long reviewFormId) {
        reviewFormRepository.findById(reviewFormId)
            .orElseThrow(() -> new NotFoundException("존재하지 않는 회고 폼입니다."));

        reviewFormRepository.delete(reviewFormId);
    }
}