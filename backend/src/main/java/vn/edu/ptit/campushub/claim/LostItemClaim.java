package vn.edu.ptit.campushub.claim;

import jakarta.persistence.*;
import java.time.Instant;
import vn.edu.ptit.campushub.post.Post;
import vn.edu.ptit.campushub.user.User;

@Entity
@Table(name = "lost_item_claims", uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "claimant_id"}))
public class LostItemClaim {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id")
    private Post post;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "claimant_id")
    private User claimant;
    @Column(nullable = false, length = 500)
    private String proof;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClaimStatus status = ClaimStatus.PENDING;
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    private Instant reviewedAt;

    protected LostItemClaim() {}

    public LostItemClaim(Post post, User claimant, String proof) {
        this.post = post;
        this.claimant = claimant;
        this.proof = proof;
    }

    public void review(ClaimStatus decision) {
        this.status = decision;
        this.reviewedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Post getPost() { return post; }
    public User getClaimant() { return claimant; }
    public String getProof() { return proof; }
    public ClaimStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getReviewedAt() { return reviewedAt; }
}
