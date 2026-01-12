package com.touchkiss.mindme.repository;

import com.touchkiss.mindme.domain.UserInterest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserInterestRepository extends JpaRepository<UserInterest, UUID> {

    Optional<UserInterest> findByCategory(String category);

    List<UserInterest> findTop10ByOrderByWeightDesc();

    List<UserInterest> findAllByOrderByWeightDesc();
}
