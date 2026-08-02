package com.sccothe.fridgeclear.ai.repository;

import com.sccothe.fridgeclear.ai.domain.AiProviderProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AiProviderProfileRepository extends JpaRepository<AiProviderProfile, Long> {
    List<AiProviderProfile> findByUserIdOrderByActiveDescNameAsc(Long userId);
    Optional<AiProviderProfile> findByIdAndUserId(Long id, Long userId);
    Optional<AiProviderProfile> findByUserIdAndActiveTrue(Long userId);
}
