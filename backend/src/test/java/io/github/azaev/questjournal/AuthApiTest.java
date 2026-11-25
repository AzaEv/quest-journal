package io.github.azaev.questjournal;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthApiTest extends ApiTestSupport {

    @Test
    void registerLoginAndMe() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username":"Geralt_1","displayName":"Neo","password":"secret-pass","inviteCode":"%s"}
                        """.formatted(INVITE)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.user.username").value("Geralt_1"));

        // login is case-insensitive on the username
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username":"geralt_1","password":"secret-pass"}
                        """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = com.jayway.jsonpath.JsonPath.read(body, "$.token");

        getAs(token, "/api/auth/me")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Neo"));
    }

    @Test
    void duplicateUsernameIsRejectedIgnoringCase() throws Exception {
        String payload = """
                {"username":"%s","displayName":"Dup","password":"secret-pass","inviteCode":"%s"}
                """;
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(payload.formatted("Twin", INVITE)))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(payload.formatted("twin", INVITE)))
                .andExpect(status().isConflict());
    }

    @Test
    void wrongInviteCodeIsForbidden() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username":"intruder","displayName":"X","password":"secret-pass","inviteCode":"guess"}
                        """))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username":"intruder","displayName":"X","password":"secret-pass"}
                        """))
                .andExpect(status().isForbidden());
    }

    @Test
    void wrongPasswordIsUnauthorized() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username":"nobody","password":"whatever-pass"}
                        """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidRegistrationIsBadRequest() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username":"a b","displayName":"","password":"short","inviteCode":"%s"}
                        """.formatted(INVITE)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void protectedEndpointsNeedToken() throws Exception {
        mvc.perform(get("/api/quests")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/quests").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void healthIsPublic() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }
}
