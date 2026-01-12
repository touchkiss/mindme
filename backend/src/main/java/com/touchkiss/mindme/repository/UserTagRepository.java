package com.touchkiss.mindme.repository;

import com.touchkiss.mindme.domain.UserTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserTagRepository extends JpaRepository<UserTag, UUID> {

    Optional<UserTag> findByTag(String tag);

    List<UserTag> findTop20ByOrderByFrequencyDesc();

    List<UserTag> findAllByOrderByFrequencyDesc();
}
