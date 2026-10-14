package vn.edu.ptit.campushub;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import vn.edu.ptit.campushub.category.Category;
import vn.edu.ptit.campushub.category.CategoryRepository;
import vn.edu.ptit.campushub.post.Post;
import vn.edu.ptit.campushub.post.PostRepository;
import vn.edu.ptit.campushub.user.Role;
import vn.edu.ptit.campushub.user.User;
import vn.edu.ptit.campushub.user.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
class PostApiIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired CategoryRepository categories;
    @Autowired UserRepository users;
    @Autowired PostRepository posts;
    @Autowired PasswordEncoder encoder;
    private String username;
    private String password;
    private Long categoryId;

    @BeforeEach
    void setUp() {
        username = "student" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        password = UUID.randomUUID().toString();
        User author = users.save(new User(username, "Sinh viên kiểm thử", encoder.encode(password), Role.USER));
        Category category = categories.save(new Category("Tìm nhóm " + username, "tim-nhom-" + username));
        categoryId = category.getId();
        posts.save(new Post("Tìm nhóm học DevOps", "Cần thêm thành viên thực hành.", category, author));
    }

    @Test
    void publicUserCanReadPosts() throws Exception {
        mockMvc.perform(get("/api/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(1)));
    }

    @Test
    void anonymousUserCannotCreatePost() throws Exception {
        mockMvc.perform(post("/api/posts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Bài mới\",\"content\":\"Nội dung hợp lệ\",\"categoryId\":" + categoryId + "}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUserCanCreatePost() throws Exception {
        mockMvc.perform(post("/api/posts").with(httpBasic(username, password))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Bài mới\",\"content\":\"Nội dung hợp lệ\",\"categoryId\":" + categoryId + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Bài mới"))
                .andExpect(jsonPath("$.author.username").value(username));
    }

    @Test
    void invalidPostReturnsReadableMessage() throws Exception {
        mockMvc.perform(post("/api/posts").with(httpBasic(username, password))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"\",\"content\":\"Nội dung\",\"categoryId\":" + categoryId + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }
}
