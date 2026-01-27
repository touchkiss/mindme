package com.touchkiss.mindme.repository;

import com.touchkiss.mindme.domain.ActivityRecord;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

public class ActivitySpecifications {

    public static Specification<ActivityRecord> withFilters(
            String query,
            String hostname,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Boolean analyzed,
            Integer maxScore,
            Integer maxDuration,
            String tag) {
        return (root, criteriaQuery, builder) -> {
            Specification<ActivityRecord> spec = Specification.where(null);

            if (StringUtils.hasText(query)) {
                String likeQuery = "%" + query.toLowerCase() + "%";
                spec = spec.and((r, q, b) -> b.or(
                        b.like(b.lower(r.get("title")), likeQuery),
                        b.like(b.lower(r.get("contentSummary")), likeQuery)));
            }

            if (StringUtils.hasText(hostname)) {
                spec = spec.and((r, q, b) -> b.like(b.lower(r.get("url")), "%" + hostname.toLowerCase() + "%"));
            }

            if (startDate != null) {
                spec = spec.and((r, q, b) -> b.greaterThanOrEqualTo(r.get("visitTime"), startDate));
            }

            if (endDate != null) {
                spec = spec.and((r, q, b) -> b.lessThanOrEqualTo(r.get("visitTime"), endDate));
            }

            if (analyzed != null) {
                spec = spec.and((r, q, b) -> b.equal(r.get("analyzed"), analyzed));
            }

            if (maxScore != null) {
                spec = spec.and((r, q, b) -> b.lessThanOrEqualTo(r.get("interestScore"), maxScore));
            }

            if (maxDuration != null) {
                spec = spec.and((r, q, b) -> b.lessThanOrEqualTo(r.get("durationSeconds"), maxDuration));
            }

            if (StringUtils.hasText(tag)) {
                spec = spec.and((r, q, b) -> b.like(b.lower(r.get("tags")), "%" + tag.toLowerCase() + "%"));
            }

            return spec.toPredicate(root, criteriaQuery, builder);
        };
    }
}
