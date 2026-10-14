package vn.edu.ptit.campushub;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
class LostItemClaimIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired CategoryRepository categories;
    @Autowired UserRepository users;
    @Autowired PostRepository posts;
    @Autowired PasswordEncoder encoder;
    private String ownerName;
    private String ownerPassword;
    private String finderName;
    private String finderPassword;
    private String outsiderName;
    private String outsiderPassword;
    private Long lostPostId;
    private Long eventPostId;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        ownerName = "owner" + suffix;
        finderName = "finder" + suffix;
        outsiderName = "outsider" + suffix;
        ownerPassword = UUID.randomUUID().toString();
        finderPassword = UUID.randomUUID().toString();
        outsiderPassword = UUID.randomUUID().toString();
        User owner = users.save(new User(ownerName, "Chủ bài", encoder.encode(ownerPassword), Role.USER));
        users.save(new User(finderName, "Người nhặt", encoder.encode(finderPassword), Role.USER));
        User outsider = users.save(new User(outsiderName, "Người khác", encoder.encode(outsiderPassword), Role.USER));
        Category lost = categories.findBySlug("do-that-lac")
                .orElseGet(() -> categories.save(new Category("Đồ thất lạc", "do-that-lac")));
        Category events = categories.findBySlug("su-kien")
                .orElseGet(() -> categories.save(new Category("Sự kiện", "su-kien")));
        lostPostId = posts.save(new Post("Tìm ví gần thư viện", "Tôi làm rơi ví.", lost, owner)).getId();
        eventPostId = posts.save(new Post("Sự kiện trường", "Ngày hội câu lạc bộ.", events, outsider)).getId();
    }

    @Test
    void claimantAndOwnerCompleteFlow() throws Exception {
        String response = mockMvc.perform(post("/api/posts/" + lostPostId + "/claims")
                .with(httpBasic(finderName, finderPassword))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"proof\":\"Ví có thẻ sinh viên màu xanh\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();
        long claimId = new com.fasterxml.jackson.databind.ObjectMapper().readTree(response).get("id").asLong();

        mockMvc.perform(post("/api/posts/" + lostPostId + "/claims")
                .with(httpBasic(finderName, finderPassword))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"proof\":\"Lặp lại\"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/api/posts/" + lostPostId + "/claims")
                .with(httpBasic(outsiderName, outsiderPassword)))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(patch("/api/posts/" + lostPostId + "/claims/" + claimId)
                .with(httpBasic(finderName, finderPassword))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/posts/" + lostPostId + "/claims/" + claimId)
                .with(httpBasic(ownerName, ownerPassword))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED"));
        mockMvc.perform(get("/api/posts/" + lostPostId))
                .andExpect(jsonPath("$.status").value("RESOLVED"));
    }

    @Test
    void invalidOrUnauthorizedClaimsAreRejected() throws Exception {
        mockMvc.perform(post("/api/posts/" + eventPostId + "/claims")
                .with(httpBasic(finderName, finderPassword))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"proof\":\"Sự kiện\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/posts/" + lostPostId + "/claims")
                .with(httpBasic(ownerName, ownerPassword))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"proof\":\"Tự nhận\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/posts/" + lostPostId + "/claims")
                .with(httpBasic(finderName, finderPassword))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"proof\":\"  \"}"))
                .andExpect(status().isBadRequest());
    }
}
