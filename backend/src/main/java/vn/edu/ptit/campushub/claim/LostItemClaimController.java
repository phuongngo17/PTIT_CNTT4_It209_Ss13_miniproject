package vn.edu.ptit.campushub.claim;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts/{postId}/claims")
public class LostItemClaimController {
    private final LostItemClaimService service;

    public LostItemClaimController(LostItemClaimService service) { this.service = service; }

    @GetMapping
    List<ClaimView> list(@PathVariable Long postId, Principal principal) {
        return service.list(postId, principal);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ClaimView create(@PathVariable Long postId, @Valid @RequestBody ClaimRequest request, Principal principal) {
        return service.create(postId, request, principal);
    }

    @PatchMapping("/{claimId}")
    ClaimView review(@PathVariable Long postId, @PathVariable Long claimId,
                     @Valid @RequestBody ReviewRequest request, Principal principal) {
        return service.review(postId, claimId, request, principal);
    }

    public record ClaimRequest(@NotBlank(message = "Vui lòng mô tả đặc điểm nhận dạng")
                               @Size(max = 500, message = "Mô tả tối đa 500 ký tự") String proof) {}
    public record ReviewRequest(@NotNull(message = "Vui lòng chọn quyết định") ClaimStatus status) {}
    public record ClaimView(Long id, Long claimantId, String claimantName, String proof,
                            ClaimStatus status, Instant createdAt, Instant reviewedAt) {
        static ClaimView from(LostItemClaim claim) {
            return new ClaimView(claim.getId(), claim.getClaimant().getId(),
                    claim.getClaimant().getDisplayName(), claim.getProof(), claim.getStatus(),
                    claim.getCreatedAt(), claim.getReviewedAt());
        }
    }
}
