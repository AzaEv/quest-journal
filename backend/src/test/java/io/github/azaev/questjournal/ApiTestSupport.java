package io.github.azaev.questjournal;

import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
@TestPropertySource(properties = "app.registration.invite-code=" + ApiTestSupport.INVITE)
public abstract class ApiTestSupport {

    protected static final String INVITE = "let-me-in";

    @Autowired
    protected MockMvc mvc;

    /** Registers a fresh user and returns its bearer token. */
    protected String registerUser() throws Exception {
        String username = "hero_" + UUID.randomUUID().toString().substring(0, 8);
        String body = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","displayName":"Hero","password":"secret-pass","inviteCode":"%s"}
                                """.formatted(username, INVITE)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    protected ResultActions getAs(String token, String url, Object... vars) throws Exception {
        return mvc.perform(get(url, vars).header("Authorization", "Bearer " + token));
    }

    protected ResultActions postAs(String token, String json, String url, Object... vars) throws Exception {
        return mvc.perform(post(url, vars).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    protected ResultActions putAs(String token, String json, String url, Object... vars) throws Exception {
        return mvc.perform(put(url, vars).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    protected ResultActions deleteAs(String token, String url, Object... vars) throws Exception {
        return mvc.perform(delete(url, vars).header("Authorization", "Bearer " + token));
    }

    protected static String json(ResultActions actions) throws Exception {
        return actions.andReturn().getResponse().getContentAsString();
    }
}
