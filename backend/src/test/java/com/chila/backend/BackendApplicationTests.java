package com.chila.backend;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyString;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chila.backend.user.UserEntity;
import com.chila.backend.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class BackendApplicationTests {

	@Container
	static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private ObjectMapper objectMapper;

	@DynamicPropertySource
	static void configureDatabase(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
	}

	@BeforeEach
	void clearUsers() {
		userRepository.deleteAll();
	}

	@Test
	void contextLoads() {
	}

	@Test
	void healthEndpointReturnsApplicationStatus() throws Exception {
		mockMvc.perform(get("/api/v1/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"))
				.andExpect(jsonPath("$.application").value("Chila Backend"));
	}

	@Test
	void healthEndpointAllowsTheConfiguredLocalFrontendOrigin() throws Exception {
		mockMvc.perform(get("/api/v1/health").header("Origin", "http://localhost:5173"))
				.andExpect(status().isOk())
				.andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
	}

	@Test
	void csrfBootstrapSetsReadableTokenCookie() throws Exception {
		mockMvc.perform(get("/api/v1/auth/csrf"))
				.andExpect(status().isNoContent())
				.andExpect(cookie().exists("XSRF-TOKEN"))
				.andExpect(cookie().httpOnly("XSRF-TOKEN", false));
	}

	@Test
	void registrationRequiresCsrfToken() throws Exception {
		mockMvc.perform(post("/api/v1/auth/register")
					.contentType(MediaType.APPLICATION_JSON)
					.content(validRegistrationJson()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("CSRF_REJECTED"));
	}

	@Test
	void registrationNormalizesEmailAndStoresOnlyBcryptHash() throws Exception {
		mockMvc.perform(registrationRequest(validRegistrationJson()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(not(emptyString())))
				.andExpect(jsonPath("$.email").value("loc@example.com"))
				.andExpect(jsonPath("$.displayName").value("Loc Nguyen"))
				.andExpect(jsonPath("$.password").doesNotExist())
				.andExpect(jsonPath("$.passwordHash").doesNotExist());

		UserEntity savedUser = userRepository.findByEmail("loc@example.com").orElseThrow();
		assertNotEquals("long-enough-password", savedUser.getPasswordHash());
		assertTrue(passwordEncoder.matches("long-enough-password", savedUser.getPasswordHash()));
	}

	@Test
	void duplicateEmailReturnsConflictEvenWhenCaseDiffers() throws Exception {
		String json = validRegistrationJson();
		mockMvc.perform(registrationRequest(json))
				.andExpect(status().isCreated());

		mockMvc.perform(registrationRequest(json.replace("Loc@Example.com", "LOC@EXAMPLE.COM")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));
	}

	@Test
	void invalidRegistrationReturnsFieldErrors() throws Exception {
		mockMvc.perform(registrationRequest(
					"{\"email\":\"bad-email\",\"password\":\"short\",\"displayName\":\" \"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.fieldErrors.email").exists())
				.andExpect(jsonPath("$.fieldErrors.password").exists())
				.andExpect(jsonPath("$.fieldErrors.displayName").exists());
	}

	@Test
	void passwordCannotExceedBcryptByteLimit() throws Exception {
		String tooLongInUtf8 = "\u00e9".repeat(37);
		String json = objectMapper.writeValueAsString(new RegistrationPayload(
				"loc@example.com", tooLongInUtf8, "Loc Nguyen"));

		mockMvc.perform(registrationRequest(json))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.password").exists());
	}

	@Test
	void loginIssuesShortLivedHttpOnlyJwtCookieAndMeReadsItsUser() throws Exception {
		registerUser();
		MockHttpServletRequestBuilder login = csrfRequest(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"LOC@EXAMPLE.COM\",\"password\":\"long-enough-password\"}"));

		MvcResult loginResult = mockMvc.perform(login)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("loc@example.com"))
				.andExpect(jsonPath("$.accessToken").doesNotExist())
				.andReturn();
		Cookie accessCookie = loginResult.getResponse().getCookie("CHILA_ACCESS");
		assertNotNull(accessCookie);
		assertTrue(accessCookie.isHttpOnly());
		assertEquals(900, accessCookie.getMaxAge());
		assertEquals("/api", accessCookie.getPath());

		mockMvc.perform(get("/api/v1/users/me").cookie(accessCookie))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("loc@example.com"));
	}

	@Test
	void invalidLoginUsesGenericUnauthorizedError() throws Exception {
		registerUser();
		mockMvc.perform(csrfRequest(post("/api/v1/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"email\":\"unknown@example.com\",\"password\":\"long-enough-password\"}")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
	}

	@Test
	void currentUserRejectsMissingAndInvalidJwtCookies() throws Exception {
		mockMvc.perform(get("/api/v1/users/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

		mockMvc.perform(get("/api/v1/users/me").cookie(new Cookie("CHILA_ACCESS", "not-a-jwt")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
	}

	@Test
	void profileUpdateRequiresAuthAndCsrfAndLogoutClearsBrowserCookie() throws Exception {
		registerUser();
		Cookie accessCookie = loginAndGetAccessCookie();
		MvcResult csrfBootstrap = mockMvc.perform(get("/api/v1/auth/csrf"))
				.andExpect(status().isNoContent()).andReturn();
		Cookie csrfCookie = csrfBootstrap.getResponse().getCookie("XSRF-TOKEN");
		assertNotNull(csrfCookie);

		mockMvc.perform(patch("/api/v1/users/me")
					.cookie(accessCookie, csrfCookie)
					.header("X-XSRF-TOKEN", csrfCookie.getValue())
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"displayName\":\"New Name\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.displayName").value("New Name"));

		mockMvc.perform(post("/api/v1/auth/logout")
					.cookie(accessCookie, csrfCookie)
					.header("X-XSRF-TOKEN", csrfCookie.getValue()))
				.andExpect(status().isNoContent())
				.andExpect(cookie().maxAge("CHILA_ACCESS", 0));

		// Logout removes the browser cookie; an already-copied JWT remains valid until its 15-minute expiry.
		mockMvc.perform(get("/api/v1/users/me").cookie(accessCookie))
				.andExpect(status().isOk());
	}

	@Test
	void logoutCanClearAnExpiredOrInvalidCookie() throws Exception {
		MvcResult bootstrap = mockMvc.perform(get("/api/v1/auth/csrf"))
				.andExpect(status().isNoContent()).andReturn();
		Cookie csrfCookie = bootstrap.getResponse().getCookie("XSRF-TOKEN");
		assertNotNull(csrfCookie);

		mockMvc.perform(post("/api/v1/auth/logout")
					.cookie(csrfCookie, new Cookie("CHILA_ACCESS", "expired-or-invalid"))
					.header("X-XSRF-TOKEN", csrfCookie.getValue()))
				.andExpect(status().isNoContent())
				.andExpect(cookie().maxAge("CHILA_ACCESS", 0));
	}

	private String validRegistrationJson() {
		return "{\"email\":\"Loc@Example.com\","
				+ "\"password\":\"long-enough-password\",\"displayName\":\" Loc Nguyen \"}";
	}

	private MockHttpServletRequestBuilder registrationRequest(String json) throws Exception {
		return csrfRequest(post("/api/v1/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json));
	}

	private MockHttpServletRequestBuilder csrfRequest(MockHttpServletRequestBuilder request) throws Exception {
		MvcResult bootstrap = mockMvc.perform(get("/api/v1/auth/csrf"))
				.andExpect(status().isNoContent()).andReturn();
		Cookie csrfCookie = bootstrap.getResponse().getCookie("XSRF-TOKEN");
		assertNotNull(csrfCookie);
		return request.cookie(csrfCookie)
				.header("X-XSRF-TOKEN", csrfCookie.getValue())
				;
	}

	private void registerUser() throws Exception {
		mockMvc.perform(registrationRequest(validRegistrationJson()))
				.andExpect(status().isCreated());
	}

	private Cookie loginAndGetAccessCookie() throws Exception {
		MvcResult result = mockMvc.perform(csrfRequest(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"loc@example.com\",\"password\":\"long-enough-password\"}")))
				.andExpect(status().isOk()).andReturn();
		Cookie cookie = result.getResponse().getCookie("CHILA_ACCESS");
		assertNotNull(cookie);
		return cookie;
	}

	private record RegistrationPayload(String email, String password, String displayName) {
	}
}
