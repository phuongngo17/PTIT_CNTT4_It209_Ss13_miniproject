package vn.edu.ptit.campushub.claim;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LostItemClaimRepository extends JpaRepository<LostItemClaim, Long> {
    List<LostItemClaim> findByPostIdOrderByCreatedAtAsc(Long postId);
    Optional<LostItemClaim> findByPostIdAndClaimantId(Long postId, Long claimantId);
    boolean existsByPostIdAndStatus(Long postId, ClaimStatus status);
    void deleteByPostId(Long postId);
}
