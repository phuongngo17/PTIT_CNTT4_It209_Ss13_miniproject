package vn.edu.ptit.campushub.post;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Post p where p.id = :id")
    Optional<Post> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            select p from Post p
            where p.status <> vn.edu.ptit.campushub.post.PostStatus.HIDDEN
              and (:categoryId is null or p.category.id = :categoryId)
              and (:keyword = '' or lower(p.title) like lower(concat('%', :keyword, '%'))
                   or lower(p.content) like lower(concat('%', :keyword, '%')))
            """)
    Page<Post> searchPublished(@Param("keyword") String keyword,
                               @Param("categoryId") Long categoryId,
                               Pageable pageable);
}
