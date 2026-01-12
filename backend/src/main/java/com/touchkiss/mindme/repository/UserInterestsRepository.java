package com.touchkiss.mindme.repository;

import com.touchkiss.mindme.domain.UserInterest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.List;

public interface UserInterestsRepository extends JpaRepository<UserInterest, UUID> {
    List<UserInterest> findAllByOrderByWeightDesc();
}
