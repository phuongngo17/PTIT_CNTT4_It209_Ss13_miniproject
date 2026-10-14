package vn.edu.ptit.campushub.claim;

import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ptit.campushub.claim.LostItemClaimController.ClaimRequest;
import vn.edu.ptit.campushub.claim.LostItemClaimController.ClaimView;
import vn.edu.ptit.campushub.claim.LostItemClaimController.ReviewRequest;
import vn.edu.ptit.campushub.common.ApiException;
import vn.edu.ptit.campushub.post.Post;
import vn.edu.ptit.campushub.post.PostRepository;
import vn.edu.ptit.campushub.post.PostStatus;
import vn.edu.ptit.campushub.user.User;
import vn.edu.ptit.campushub.user.UserRepository;

@Service
public class LostItemClaimService {
    private final LostItemClaimRepository claims;
    private final PostRepository posts;
    private final UserRepository users;

    public LostItemClaimService(LostItemClaimRepository claims, PostRepository posts, UserRepository users) {
        this.claims = claims;
        this.posts = posts;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<ClaimView> list(Long postId, Principal principal) {
        Post post = findLostPost(postId);
        User user = currentUser(principal);
        return claims.findByPostIdOrderByCreatedAtAsc(postId).stream()
                .filter(claim -> post.getAuthor().getId().equals(user.getId())
                        || claim.getClaimant().getId().equals(user.getId()))
                .map(ClaimView::from).toList();
    }

    @Transactional
    public ClaimView create(Long postId, ClaimRequest request, Principal principal) {
        Post post = lockedLostPost(postId);
        User user = currentUser(principal);
        if (post.getStatus() != PostStatus.PUBLISHED) {
            throw new ApiException(HttpStatus.CONFLICT, "Bài đăng không còn nhận yêu cầu");
        }
        if (post.getAuthor().getId().equals(user.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Bạn không thể xác nhận bài của mình");
        }
        if (claims.findByPostIdAndClaimantId(postId, user.getId()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "Bạn đã gửi yêu cầu cho bài này");
        }
        return ClaimView.from(claims.save(new LostItemClaim(post, user, request.proof().trim())));
    }

    @Transactional
    public ClaimView review(Long postId, Long claimId, ReviewRequest request, Principal principal) {
        Post post = lockedLostPost(postId);
        User user = currentUser(principal);
        if (!post.getAuthor().getId().equals(user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Chỉ chủ bài được duyệt yêu cầu");
        }
        LostItemClaim claim = claims.findById(claimId)
                .filter(item -> item.getPost().getId().equals(postId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy yêu cầu"));
        if (post.getStatus() != PostStatus.PUBLISHED || claim.getStatus() != ClaimStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "Yêu cầu này không còn chờ duyệt");
        }
        if (request.status() != ClaimStatus.APPROVED && request.status() != ClaimStatus.REJECTED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Quyết định không hợp lệ");
        }
        if (request.status() == ClaimStatus.APPROVED
                && claims.existsByPostIdAndStatus(postId, ClaimStatus.APPROVED)) {
            throw new ApiException(HttpStatus.CONFLICT, "Bài đăng đã có yêu cầu được duyệt");
        }
        claim.review(request.status());
        if (request.status() == ClaimStatus.APPROVED) post.changeStatus(PostStatus.RESOLVED);
        return ClaimView.from(claim);
    }

    private Post findLostPost(Long id) {
        Post post = posts.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy bài đăng"));
        if (!"do-that-lac".equals(post.getCategory().getSlug()) || post.getStatus() == PostStatus.HIDDEN) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy bài đồ thất lạc");
        }
        return post;
    }

    private Post lockedLostPost(Long id) {
        Post post = posts.findByIdForUpdate(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy bài đăng"));
        if (!"do-that-lac".equals(post.getCategory().getSlug()) || post.getStatus() == PostStatus.HIDDEN) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy bài đồ thất lạc");
        }
        return post;
    }

    private User currentUser(Principal principal) {
        if (principal == null) throw new ApiException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập");
        return users.findByUsername(principal.getName())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập"));
    }
}
