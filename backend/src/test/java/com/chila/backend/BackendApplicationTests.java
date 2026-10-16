package com.chila.backend;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyString;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chila.backend.user.UserEntity;
import com.chila.backend.user.UserRepository;
import com.chila.backend.workspace.WorkspaceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
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
	private WorkspaceRepository workspaceRepository;

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
		workspaceRepository.deleteAllInBatch();
		userRepository.deleteAllInBatch();
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

	@Test
	void workspaceCreationCreatesOwnerAndReturnsOnlyTheCallersWorkspaces() throws Exception {
		registerUser();
		Cookie owner = loginAndGetAccessCookie();
		MvcResult created = mockMvc.perform(authenticatedCsrfRequest(post("/api/v1/workspaces")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"name\":\"  Chila Team  \",\"description\":\" Planning workspace \"}"), owner))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("Chila Team"))
				.andExpect(jsonPath("$.description").value("Planning workspace"))
				.andExpect(jsonPath("$.role").value("OWNER"))
				.andReturn();
		String workspaceId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

		mockMvc.perform(get("/api/v1/workspaces").cookie(owner))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(workspaceId))
				.andExpect(jsonPath("$[0].role").value("OWNER"));
		mockMvc.perform(get("/api/v1/workspaces/{id}/members", workspaceId).cookie(owner))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].email").value("loc@example.com"))
				.andExpect(jsonPath("$[0].role").value("OWNER"))
				.andExpect(jsonPath("$[0].passwordHash").doesNotExist());
	}

	@Test
	void nonMembersCannotDiscoverOrReadAWorkspace() throws Exception {
		registerUser();
		Cookie owner = loginAndGetAccessCookie();
		MvcResult created = mockMvc.perform(authenticatedCsrfRequest(post("/api/v1/workspaces")
					.contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Private\"}"), owner))
				.andExpect(status().isCreated()).andReturn();
		String workspaceId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

		mockMvc.perform(registrationRequest("{\"email\":\"other@example.com\","
				+ "\"password\":\"long-enough-password\",\"displayName\":\"Other User\"}"))
				.andExpect(status().isCreated());
		Cookie otherUser = loginAndGetAccessCookie("other@example.com");
		mockMvc.perform(get("/api/v1/workspaces/{id}", workspaceId).cookie(otherUser))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("WORKSPACE_NOT_FOUND"));
		mockMvc.perform(get("/api/v1/workspaces").cookie(otherUser))
				.andExpect(status().isOk()).andExpect(content().json("[]"));
	}

	@Test
	void ownerCanAddAndPromoteAMemberButMemberCannotManageWorkspace() throws Exception {
		registerUser();
		Cookie owner = loginAndGetAccessCookie();
		MvcResult created = mockMvc.perform(authenticatedCsrfRequest(post("/api/v1/workspaces")
					.contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Team\"}"), owner))
				.andExpect(status().isCreated()).andReturn();
		String workspaceId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();
		mockMvc.perform(registrationRequest("{\"email\":\"member@example.com\","
				+ "\"password\":\"long-enough-password\",\"displayName\":\"Member User\"}"))
				.andExpect(status().isCreated());

		mockMvc.perform(authenticatedCsrfRequest(post("/api/v1/workspaces/{id}/members", workspaceId)
					.contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"MEMBER@EXAMPLE.COM\"}"), owner))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.role").value("MEMBER"));
		mockMvc.perform(authenticatedCsrfRequest(post("/api/v1/workspaces/{id}/members", workspaceId)
					.contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"member@example.com\"}"), owner))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("MEMBERSHIP_ALREADY_EXISTS"));

		Cookie member = loginAndGetAccessCookie("member@example.com");
		mockMvc.perform(get("/api/v1/workspaces/{id}", workspaceId).cookie(member))
				.andExpect(status().isOk()).andExpect(jsonPath("$.role").value("MEMBER"));
		mockMvc.perform(authenticatedCsrfRequest(patch("/api/v1/workspaces/{id}", workspaceId)
					.contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Changed\"}"), member))
				.andExpect(status().isForbidden());
		mockMvc.perform(authenticatedCsrfRequest(patch("/api/v1/workspaces/{workspaceId}/members/{userId}",
					workspaceId, userRepository.findByEmail("member@example.com").orElseThrow().getId())
					.contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"ADMIN\"}"), owner))
				.andExpect(status().isOk()).andExpect(jsonPath("$.role").value("ADMIN"));
	}

	@Test
	void ownerCannotBeRemovedAndOnlyOwnerCanDeleteWorkspace() throws Exception {
		registerUser();
		Cookie owner = loginAndGetAccessCookie();
		MvcResult created = mockMvc.perform(authenticatedCsrfRequest(post("/api/v1/workspaces")
					.contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Team\"}"), owner))
				.andExpect(status().isCreated()).andReturn();
		String workspaceId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();
		String ownerId = userRepository.findByEmail("loc@example.com").orElseThrow().getId().toString();

		mockMvc.perform(authenticatedCsrfRequest(delete("/api/v1/workspaces/{workspaceId}/members/{userId}",
					workspaceId, ownerId), owner))
				.andExpect(status().isBadRequest());
		mockMvc.perform(authenticatedCsrfRequest(delete("/api/v1/workspaces/{id}", workspaceId), owner))
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/v1/workspaces/{id}", workspaceId).cookie(owner))
				.andExpect(status().isNotFound());
	}

	@Test
	void ownerCanCreateListUpdateAndArchiveProjectsWithWorkspaceScopedUniqueKeys() throws Exception {
		registerUser();
		Cookie owner = loginAndGetAccessCookie();
		MvcResult workspace = mockMvc.perform(authenticatedCsrfRequest(post("/api/v1/workspaces")
					.contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Team\"}"), owner))
				.andExpect(status().isCreated()).andReturn();
		String workspaceId = objectMapper.readTree(workspace.getResponse().getContentAsString()).get("id").asText();

		MvcResult created = mockMvc.perform(authenticatedCsrfRequest(post("/api/v1/workspaces/{id}/projects", workspaceId)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"name\":\"Ticket Manager\",\"key\":\"chila\",\"description\":\"First project\"}"), owner))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("Ticket Manager"))
				.andExpect(jsonPath("$.key").value("CHILA"))
				.andExpect(jsonPath("$.status").value("ACTIVE"))
				.andReturn();
		String projectId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

		mockMvc.perform(get("/api/v1/workspaces/{id}/projects", workspaceId).cookie(owner))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(projectId));
		mockMvc.perform(get("/api/v1/projects/{id}", projectId).cookie(owner))
				.andExpect(status().isOk()).andExpect(jsonPath("$.createdBy").exists());
		mockMvc.perform(authenticatedCsrfRequest(post("/api/v1/workspaces/{id}/projects", workspaceId)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"name\":\"Duplicate\",\"key\":\"CHILA\"}"), owner))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("PROJECT_KEY_ALREADY_EXISTS"));
		mockMvc.perform(authenticatedCsrfRequest(post("/api/v1/workspaces/{id}/projects", workspaceId)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"name\":\"Invalid key\",\"key\":\"bad-key\"}"), owner))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.key").exists());

		mockMvc.perform(authenticatedCsrfRequest(patch("/api/v1/projects/{id}", projectId)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"name\":\"Updated Project\",\"description\":\"Updated\"}"), owner))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Updated Project"))
				.andExpect(jsonPath("$.key").value("CHILA"));
		mockMvc.perform(authenticatedCsrfRequest(patch("/api/v1/projects/{id}/status", projectId)
					.contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ARCHIVED\"}"), owner))
				.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ARCHIVED"));
		mockMvc.perform(authenticatedCsrfRequest(patch("/api/v1/projects/{id}/status", projectId)
					.contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"REMOVED\"}"), owner))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
	}

	@Test
	void projectReadsRequireWorkspaceMembershipAndMembersCannotManageProjects() throws Exception {
		registerUser();
		Cookie owner = loginAndGetAccessCookie();
		MvcResult workspace = mockMvc.perform(authenticatedCsrfRequest(post("/api/v1/workspaces")
					.contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Team\"}"), owner))
				.andExpect(status().isCreated()).andReturn();
		String workspaceId = objectMapper.readTree(workspace.getResponse().getContentAsString()).get("id").asText();
		String projectJson = "{\"name\":\"Private project\",\"key\":\"PRIVATE\"}";
		MvcResult created = mockMvc.perform(authenticatedCsrfRequest(post("/api/v1/workspaces/{id}/projects", workspaceId)
					.contentType(MediaType.APPLICATION_JSON).content(projectJson), owner))
				.andExpect(status().isCreated()).andReturn();
		String projectId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

		mockMvc.perform(registrationRequest("{\"email\":\"member@example.com\","
				+ "\"password\":\"long-enough-password\",\"displayName\":\"Member User\"}"))
				.andExpect(status().isCreated());
		mockMvc.perform(authenticatedCsrfRequest(post("/api/v1/workspaces/{id}/members", workspaceId)
					.contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"member@example.com\"}"), owner))
				.andExpect(status().isCreated());
		Cookie member = loginAndGetAccessCookie("member@example.com");
		mockMvc.perform(get("/api/v1/projects/{id}", projectId).cookie(member))
				.andExpect(status().isOk());
		mockMvc.perform(authenticatedCsrfRequest(post("/api/v1/workspaces/{id}/projects", workspaceId)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"name\":\"Member Project\",\"key\":\"MEMBER\"}"), member))
				.andExpect(status().isCreated());
		mockMvc.perform(authenticatedCsrfRequest(patch("/api/v1/projects/{id}", projectId)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"name\":\"Unauthorized edit\"}"), member))
				.andExpect(status().isForbidden());

		mockMvc.perform(registrationRequest("{\"email\":\"outsider@example.com\","
				+ "\"password\":\"long-enough-password\",\"displayName\":\"Outsider\"}"))
				.andExpect(status().isCreated());
		Cookie outsider = loginAndGetAccessCookie("outsider@example.com");
		mockMvc.perform(get("/api/v1/projects/{id}", projectId).cookie(outsider))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("WORKSPACE_NOT_FOUND"));
		mockMvc.perform(get("/api/v1/workspaces/{id}/projects", workspaceId).cookie(outsider))
				.andExpect(status().isNotFound());
	}

	@Test
	void issueLifecycleSupportsAssignmentFilteringPagingAndArchiveRestore() throws Exception {
		registerUser();
		Cookie owner = loginAndGetAccessCookie();
		String workspaceId = createWorkspaceFor(owner);
		mockMvc.perform(registrationRequest("{\"email\":\"assignee@example.com\","
				+ "\"password\":\"long-enough-password\",\"displayName\":\"Assigned Person\"}"))
				.andExpect(status().isCreated());
		mockMvc.perform(authenticatedCsrfRequest(post("/api/v1/workspaces/{id}/members", workspaceId)
					.contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"assignee@example.com\"}"), owner))
				.andExpect(status().isCreated());
		UUID assigneeId = userRepository.findByEmail("assignee@example.com").orElseThrow().getId();
		String projectId = createProjectFor(owner, workspaceId);

		MvcResult created = mockMvc.perform(authenticatedCsrfRequest(post("/api/v1/projects/{id}/issues", projectId)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"title\":\"Broken login\",\"description\":\"Users cannot sign in\","
						+ "\"type\":\"BUG\",\"status\":\"TODO\",\"priority\":\"HIGH\","
						+ "\"assigneeId\":\"" + assigneeId + "\",\"dueDate\":\"2027-01-10\"}"), owner))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.reporterName").value("Loc Nguyen"))
				.andExpect(jsonPath("$.assigneeName").value("Assigned Person"))
				.andExpect(jsonPath("$.status").value("TODO"))
				.andReturn();
		String issueId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

		mockMvc.perform(get("/api/v1/projects/{id}/issues", projectId)
					.param("status", "TODO").param("priority", "HIGH").param("type", "BUG")
					.param("assigneeId", assigneeId.toString()).param("search", "login")
					.param("page", "0").param("size", "1").param("sort", "title").param("direction", "asc")
					.cookie(owner))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalItems").value(1))
				.andExpect(jsonPath("$.items[0].id").value(issueId));

		Cookie assignee = loginAndGetAccessCookie("assignee@example.com");
		mockMvc.perform(authenticatedCsrfRequest(patch("/api/v1/issues/{id}", issueId)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"title\":\"Login issue fixed\",\"description\":null,\"type\":\"TASK\","
						+ "\"status\":\"IN_PROGRESS\",\"priority\":\"MEDIUM\",\"assigneeId\":null,\"dueDate\":null}"), assignee))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Login issue fixed"))
				.andExpect(jsonPath("$.assigneeId").doesNotExist());
		mockMvc.perform(authenticatedCsrfRequest(delete("/api/v1/issues/{id}", issueId), assignee))
				.andExpect(status().isForbidden());
		mockMvc.perform(authenticatedCsrfRequest(delete("/api/v1/issues/{id}", issueId), owner))
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/v1/projects/{id}/issues", projectId).cookie(owner))
				.andExpect(status().isOk()).andExpect(jsonPath("$.totalItems").value(0));
		mockMvc.perform(get("/api/v1/projects/{id}/issues", projectId)
					.param("includeArchived", "true").cookie(owner))
				.andExpect(status().isOk()).andExpect(jsonPath("$.totalItems").value(1));
		mockMvc.perform(authenticatedCsrfRequest(post("/api/v1/issues/{id}/restore", issueId), owner))
				.andExpect(status().isOk()).andExpect(jsonPath("$.archivedAt").doesNotExist());
	}

	@Test
	void issueKeysCanBeLookedUpAndBoardOrderCanBeChanged() throws Exception {
		registerUser();
		Cookie owner = loginAndGetAccessCookie();
		String workspaceId = createWorkspaceFor(owner);
		String projectId = createProjectFor(owner, workspaceId);

		String firstId = createIssue(owner, projectId, "First issue");
		String secondId = createIssue(owner, projectId, "Second issue");
		String thirdId = createIssue(owner, projectId, "Third issue");
		mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/issues/ISS-2", workspaceId).cookie(owner))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(secondId))
				.andExpect(jsonPath("$.issueKey").value("ISS-2"))
				.andExpect(jsonPath("$.issueNumber").value(2));

		mockMvc.perform(get("/api/v1/projects/{projectId}/issues", projectId)
				.param("sort", "rank").param("direction", "asc").cookie(owner))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[0].id").value(firstId))
				.andExpect(jsonPath("$.items[1].id").value(secondId))
				.andExpect(jsonPath("$.items[2].id").value(thirdId));

		mockMvc.perform(authenticatedCsrfRequest(patch("/api/v1/issues/{id}/position", thirdId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"TODO\",\"beforeIssueId\":\"" + firstId + "\"}"), owner))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.issueKey").value("ISS-3"));
		mockMvc.perform(get("/api/v1/projects/{projectId}/issues", projectId)
				.param("sort", "rank").param("direction", "asc").cookie(owner))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[0].id").value(thirdId))
				.andExpect(jsonPath("$.items[1].id").value(firstId))
				.andExpect(jsonPath("$.items[2].id").value(secondId));

		mockMvc.perform(authenticatedCsrfRequest(patch("/api/v1/issues/{id}/position", firstId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"IN_PROGRESS\"}"), owner))
				.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("IN_PROGRESS"));
		mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/issues/ISS-1", workspaceId).cookie(owner))
				.andExpect(status().isOk()).andExpect(jsonPath("$.id").value(firstId));

		mockMvc.perform(registrationRequest("{\"email\":\"board-outsider@example.com\","
				+ "\"password\":\"long-enough-password\",\"displayName\":\"Outsider\"}"))
				.andExpect(status().isCreated());
		Cookie outsider = loginAndGetAccessCookie("board-outsider@example.com");
		mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/issues/ISS-1", workspaceId).cookie(outsider))
				.andExpect(status().isNotFound());
		mockMvc.perform(authenticatedCsrfRequest(patch("/api/v1/issues/{id}/position", secondId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"TODO\"}"), outsider))
				.andExpect(status().isNotFound());
	}

	@Test
	void statusAutomationNotifiesAndRetainsExecutionHistory() throws Exception {
		registerUser();
		Cookie owner = loginAndGetAccessCookie();
		String workspaceId = createWorkspaceFor(owner);
		String projectId = createProjectFor(owner, workspaceId);
		String issueId = createIssue(owner, projectId, "Automation target");
		String ruleJson = "{\"name\":\"Notify reporter on progress\",\"enabled\":true,"
				+ "\"targetStatus\":\"IN_PROGRESS\",\"action\":\"NOTIFY_REPORTER\"}";
		MvcResult created = mockMvc.perform(authenticatedCsrfRequest(post("/api/v1/projects/{projectId}/automation/rules", projectId)
				.contentType(MediaType.APPLICATION_JSON).content(ruleJson), owner))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.enabled").value(true)).andReturn();
		String ruleId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();
		mockMvc.perform(registrationRequest("{\"email\":\"automation-member@example.com\","
				+ "\"password\":\"long-enough-password\",\"displayName\":\"Automation Member\"}"))
				.andExpect(status().isCreated());
		mockMvc.perform(authenticatedCsrfRequest(post("/api/v1/workspaces/{id}/members", workspaceId)
				.contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"automation-member@example.com\",\"role\":\"MEMBER\"}"), owner))
				.andExpect(status().isCreated());
		Cookie member = loginAndGetAccessCookie("automation-member@example.com");
		mockMvc.perform(authenticatedCsrfRequest(post("/api/v1/projects/{projectId}/automation/rules", projectId)
				.contentType(MediaType.APPLICATION_JSON).content(ruleJson), member)).andExpect(status().isForbidden());
		mockMvc.perform(authenticatedCsrfRequest(patch("/api/v1/issues/{id}/position", issueId)
				.contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"IN_PROGRESS\",\"beforeIssueId\":null}"), owner))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/v1/users/me/notifications").param("size", "50").cookie(owner))
				.andExpect(status().isOk()).andExpect(jsonPath("$.items[0].eventType").value("AUTOMATION"));
		mockMvc.perform(get("/api/v1/projects/{projectId}/automation/executions", projectId).cookie(owner))
				.andExpect(status().isOk()).andExpect(jsonPath("$[0].outcome").value("SUCCEEDED"))
				.andExpect(jsonPath("$[0].issueKey").isNotEmpty());
		mockMvc.perform(get("/api/v1/projects/{projectId}/automation/executions", projectId).cookie(member))
				.andExpect(status().isOk()).andExpect(jsonPath("$[0].ruleName").value("Notify reporter on progress"));
		mockMvc.perform(authenticatedCsrfRequest(delete("/api/v1/projects/{projectId}/automation/rules/{ruleId}", projectId, ruleId), owner))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/v1/projects/{projectId}/automation/executions", projectId).cookie(owner))
				.andExpect(status().isOk()).andExpect(jsonPath("$[0].ruleId").doesNotExist())
				.andExpect(jsonPath("$[0].ruleName").value("Notify reporter on progress"));
	}

	@Test
	void sprintLifecyclePlansIssuesEnforcesRolesAndPreservesClosedHistory() throws Exception {
		registerUser();
		Cookie owner = loginAndGetAccessCookie();
		String workspaceId = createWorkspaceFor(owner);
		String projectId = createProjectFor(owner, workspaceId);
		String issueId = createIssue(owner, projectId, "Sprint candidate");
		String secondIssueId = createIssue(owner, projectId, "Second backlog issue");
		MvcResult completedIssue = mockMvc.perform(authenticatedCsrfRequest(post("/api/v1/projects/{id}/issues", projectId)
				.contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Already complete\",\"type\":\"TASK\",\"status\":\"DONE\"}"), owner))
				.andExpect(status().isCreated()).andReturn();
		String completedIssueId = objectMapper.readTree(completedIssue.getResponse().getContentAsString()).get("id").asText();
		String sprintJson = "{\"name\":\"Sprint One\",\"goal\":\"Ship core work\","
				+ "\"startDate\":\"2026-01-01\",\"endDate\":\"2026-12-31\"}";
		MvcResult sprintCreated = mockMvc.perform(authenticatedCsrfRequest(post("/api/v1/projects/{id}/sprints", projectId)
				.contentType(MediaType.APPLICATION_JSON).content(sprintJson), owner))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.state").value("FUTURE")).andReturn();
		String sprintId = objectMapper.readTree(sprintCreated.getResponse().getContentAsString()).get("id").asText();
		MvcResult otherWorkspace = mockMvc.perform(authenticatedCsrfRequest(post("/api/v1/workspaces")
				.contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Other sprint workspace\"}"), owner))
				.andExpect(status().isCreated()).andReturn();
		String otherWorkspaceId = objectMapper.readTree(otherWorkspace.getResponse().getContentAsString()).get("id").asText();
		String otherProjectId = createProjectFor(owner, otherWorkspaceId);
		MvcResult otherSprint = mockMvc.perform(authenticatedCsrfRequest(post("/api/v1/projects/{id}/sprints", otherProjectId)
				.contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Foreign sprint\"}"), owner))
				.andExpect(status().isCreated()).andReturn();
		String otherSprintId = objectMapper.readTree(otherSprint.getResponse().getContentAsString()).get("id").asText();
		mockMvc.perform(authenticatedCsrfRequest(patch("/api/v1/issues/{id}/sprint", issueId)
				.contentType(MediaType.APPLICATION_JSON).content("{\"sprintId\":\"" + otherSprintId + "\"}"), owner))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("SPRINT_NOT_FOUND"));
		mockMvc.perform(get("/api/v1/projects/{id}/backlog", projectId).param("sort", "rank").cookie(owner))
				.andExpect(status().isOk()).andExpect(jsonPath("$.items[0].id").value(issueId))
				.andExpect(jsonPath("$.items[1].id").value(secondIssueId));
		mockMvc.perform(authenticatedCsrfRequest(patch("/api/v1/issues/{id}/sprint", secondIssueId)
				.contentType(MediaType.APPLICATION_JSON).content("{\"beforeIssueId\":\"" + issueId + "\"}"), owner))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/v1/projects/{id}/backlog", projectId).cookie(owner))
				.andExpect(status().isOk()).andExpect(jsonPath("$.items[0].id").value(secondIssueId));
		mockMvc.perform(authenticatedCsrfRequest(post("/api/v1/projects/{projectId}/sprints/{sprintId}/issues", projectId, sprintId)
				.contentType(MediaType.APPLICATION_JSON).content("{\"issueIds\":[\"" + issueId + "\",\"" + completedIssueId + "\"]}"), owner))
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/v1/projects/{id}/backlog", projectId).cookie(owner))
				.andExpect(status().isOk()).andExpect(jsonPath("$.totalItems").value(1));
		mockMvc.perform(authenticatedCsrfRequest(post("/api/v1/projects/{projectId}/sprints/{sprintId}/start", projectId, sprintId), owner))
				.andExpect(status().isOk()).andExpect(jsonPath("$.state").value("ACTIVE"));
		mockMvc.perform(get("/api/v1/projects/{projectId}/sprints/{sprintId}/issues", projectId, sprintId)
				.param("status", "TODO").cookie(owner))
				.andExpect(status().isOk()).andExpect(jsonPath("$.totalItems").value(1))
				.andExpect(jsonPath("$.items[0].sprintId").value(sprintId));

		MvcResult secondCreated = mockMvc.perform(authenticatedCsrfRequest(post("/api/v1/projects/{id}/sprints", projectId)
				.contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Sprint Two\",\"startDate\":\"2026-01-01\",\"endDate\":\"2026-12-31\"}"), owner))
				.andExpect(status().isCreated()).andReturn();
		String secondSprintId = objectMapper.readTree(secondCreated.getResponse().getContentAsString()).get("id").asText();
		mockMvc.perform(authenticatedCsrfRequest(post("/api/v1/projects/{projectId}/sprints/{sprintId}/start", projectId, secondSprintId), owner))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("SPRINT_CONFLICT"));

		mockMvc.perform(registrationRequest("{\"email\":\"sprint-member@example.com\","
				+ "\"password\":\"long-enough-password\",\"displayName\":\"Sprint Member\"}"))
				.andExpect(status().isCreated());
		mockMvc.perform(authenticatedCsrfRequest(post("/api/v1/workspaces/{id}/members", workspaceId)
				.contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"sprint-member@example.com\",\"role\":\"MEMBER\"}"), owner))
				.andExpect(status().isCreated());
		Cookie member = loginAndGetAccessCookie("sprint-member@example.com");
		mockMvc.perform(authenticatedCsrfRequest(post("/api/v1/projects/{id}/sprints", projectId)
				.contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Unauthorized sprint\"}"), member))
				.andExpect(status().isForbidden());

		mockMvc.perform(authenticatedCsrfRequest(post("/api/v1/projects/{projectId}/sprints/{sprintId}/complete", projectId, sprintId)
				.contentType(MediaType.APPLICATION_JSON).content("{\"unfinishedIssueAction\":\"BACKLOG\"}"), owner))
				.andExpect(status().isOk()).andExpect(jsonPath("$.state").value("CLOSED"));
		mockMvc.perform(get("/api/v1/projects/{projectId}/reports/sprints/{sprintId}", projectId, sprintId).cookie(owner))
				.andExpect(status().isOk()).andExpect(jsonPath("$.historyAvailable").value(true))
				.andExpect(jsonPath("$.committedIssues").value(2)).andExpect(jsonPath("$.completedIssues").value(1))
				.andExpect(jsonPath("$.unfinishedIssues").value(1)).andExpect(jsonPath("$.burndown").isArray())
				.andExpect(jsonPath("$.completionIssues.length()").value(2));
		mockMvc.perform(get("/api/v1/projects/{projectId}/reports", projectId).cookie(owner))
				.andExpect(status().isOk()).andExpect(jsonPath("$.totalIssues").value(3))
				.andExpect(jsonPath("$.issuesByStatus.DONE").value(1))
				.andExpect(jsonPath("$.sprintVelocity[0].historyAvailable").value(true));
		mockMvc.perform(get("/api/v1/projects/{id}/backlog", projectId).cookie(owner))
				.andExpect(status().isOk()).andExpect(jsonPath("$.totalItems").value(2));
		mockMvc.perform(get("/api/v1/projects/{id}/sprints", projectId).cookie(owner))
				.andExpect(status().isOk()).andExpect(jsonPath("$[?(@.id == '" + sprintId + "')].state").value(org.hamcrest.Matchers.contains("CLOSED")));
		mockMvc.perform(get("/api/v1/projects/{projectId}/sprints/{sprintId}/issues", projectId, sprintId).cookie(owner))
				.andExpect(status().isOk()).andExpect(jsonPath("$.totalItems").value(1))
				.andExpect(jsonPath("$.items[0].id").value(completedIssueId));
		mockMvc.perform(authenticatedCsrfRequest(patch("/api/v1/issues/{id}/sprint", completedIssueId)
				.contentType(MediaType.APPLICATION_JSON).content("{\"sprintId\":null}"), owner))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("SPRINT_CONFLICT"));

		MvcResult thirdCreated = mockMvc.perform(authenticatedCsrfRequest(post("/api/v1/projects/{id}/sprints", projectId)
				.contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Sprint Three\"}"), owner))
				.andExpect(status().isCreated()).andReturn();
		String thirdSprintId = objectMapper.readTree(thirdCreated.getResponse().getContentAsString()).get("id").asText();
		mockMvc.perform(authenticatedCsrfRequest(post("/api/v1/projects/{projectId}/sprints/{sprintId}/issues", projectId, secondSprintId)
				.contentType(MediaType.APPLICATION_JSON).content("{\"issueIds\":[\"" + secondIssueId + "\"]}"), owner))
				.andExpect(status().isNoContent());
		mockMvc.perform(authenticatedCsrfRequest(post("/api/v1/projects/{projectId}/sprints/{sprintId}/start", projectId, secondSprintId), owner))
				.andExpect(status().isOk()).andExpect(jsonPath("$.state").value("ACTIVE"));
		mockMvc.perform(authenticatedCsrfRequest(post("/api/v1/projects/{projectId}/sprints/{sprintId}/complete", projectId, secondSprintId)
				.contentType(MediaType.APPLICATION_JSON).content("{\"unfinishedIssueAction\":\"FUTURE_SPRINT\",\"targetSprintId\":\"" + thirdSprintId + "\"}"), owner))
				.andExpect(status().isOk()).andExpect(jsonPath("$.state").value("CLOSED"));
		mockMvc.perform(get("/api/v1/projects/{projectId}/sprints/{sprintId}/issues", projectId, thirdSprintId).cookie(owner))
				.andExpect(status().isOk()).andExpect(jsonPath("$.totalItems").value(1))
				.andExpect(jsonPath("$.items[0].id").value(secondIssueId));
	}

	@Test
	void epicsParentsLinksAndReleasesRespectProjectBoundaries() throws Exception {
		registerUser();
		Cookie owner = loginAndGetAccessCookie();
		String workspaceId = createWorkspaceFor(owner);
		String projectId = createProjectFor(owner, workspaceId);
		String firstId = createIssue(owner, projectId, "Epic parent");
		String secondId = createIssue(owner, projectId, "Child task");
		String epicJson = "{\"name\":\"Checkout\",\"summary\":\"Checkout improvements\",\"color\":\"#6554C0\"}";
		MvcResult epicResult = mockMvc.perform(authenticatedCsrfRequest(post("/api/v1/projects/{id}/epics", projectId)
				.contentType(MediaType.APPLICATION_JSON).content(epicJson), owner))
				.andExpect(status().isCreated()).andReturn();
		String epicId = objectMapper.readTree(epicResult.getResponse().getContentAsString()).get("id").asText();
		String releaseJson = "{\"name\":\"1.0\",\"description\":\"First release\",\"startDate\":\"2027-01-01\",\"releaseDate\":\"2027-01-31\",\"state\":\"UNRELEASED\"}";
		MvcResult releaseResult = mockMvc.perform(authenticatedCsrfRequest(post("/api/v1/projects/{id}/releases", projectId)
				.contentType(MediaType.APPLICATION_JSON).content(releaseJson), owner))
				.andExpect(status().isCreated()).andReturn();
		String releaseId = objectMapper.readTree(releaseResult.getResponse().getContentAsString()).get("id").asText();

		mockMvc.perform(authenticatedCsrfRequest(put("/api/v1/issues/{id}/epic", firstId)
				.contentType(MediaType.APPLICATION_JSON).content("{\"id\":\"" + epicId + "\"}"), owner))
				.andExpect(status().isOk()).andExpect(jsonPath("$.epicId").value(epicId));
		mockMvc.perform(authenticatedCsrfRequest(put("/api/v1/issues/{id}/release", firstId)
				.contentType(MediaType.APPLICATION_JSON).content("{\"id\":\"" + releaseId + "\"}"), owner))
				.andExpect(status().isOk()).andExpect(jsonPath("$.releaseId").value(releaseId));
		mockMvc.perform(authenticatedCsrfRequest(patch("/api/v1/issues/{id}", firstId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"Epic parent\",\"description\":null,\"type\":\"TASK\","
						+ "\"status\":\"TODO\",\"priority\":\"MEDIUM\",\"assigneeId\":null,\"dueDate\":\"2027-01-12\"}"), owner))
				.andExpect(status().isOk()).andExpect(jsonPath("$.dueDate").value("2027-01-12"));
		mockMvc.perform(get("/api/v1/projects/{id}/timeline/due-issues", projectId).cookie(owner))
				.andExpect(status().isOk()).andExpect(jsonPath("$.items[0].id").value(firstId));
		mockMvc.perform(authenticatedCsrfRequest(put("/api/v1/issues/{id}/parent", secondId)
				.contentType(MediaType.APPLICATION_JSON).content("{\"id\":\"" + firstId + "\"}"), owner))
				.andExpect(status().isOk()).andExpect(jsonPath("$.parentIssueId").value(firstId));
		mockMvc.perform(authenticatedCsrfRequest(put("/api/v1/issues/{id}/parent", firstId)
				.contentType(MediaType.APPLICATION_JSON).content("{\"id\":\"" + secondId + "\"}"), owner))
				.andExpect(status().isBadRequest());
		mockMvc.perform(authenticatedCsrfRequest(post("/api/v1/issues/{id}/links", firstId)
				.contentType(MediaType.APPLICATION_JSON).content("{\"targetIssueId\":\"" + secondId + "\",\"type\":\"BLOCKS\"}"), owner))
				.andExpect(status().isCreated());
		mockMvc.perform(get("/api/v1/issues/{id}/relationships", firstId).cookie(owner))
				.andExpect(status().isOk()).andExpect(jsonPath("$.children[0].id").value(secondId))
				.andExpect(jsonPath("$.links[0].type").value("BLOCKS"));
		mockMvc.perform(get("/api/v1/projects/{id}/epics/{epicId}/issues", projectId, epicId).cookie(owner))
				.andExpect(status().isOk()).andExpect(jsonPath("$.items[0].id").value(firstId));
		mockMvc.perform(get("/api/v1/projects/{id}/releases", projectId).cookie(owner))
				.andExpect(status().isOk()).andExpect(jsonPath("$[0].issueCount").value(1));

		MvcResult otherProject = mockMvc.perform(authenticatedCsrfRequest(post("/api/v1/workspaces/{id}/projects", workspaceId)
				.contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"External project\",\"key\":\"EXT\"}"), owner))
				.andExpect(status().isCreated()).andReturn();
		String otherProjectId = objectMapper.readTree(otherProject.getResponse().getContentAsString()).get("id").asText();
		String foreignIssueId = createIssue(owner, otherProjectId, "Foreign issue");
		mockMvc.perform(authenticatedCsrfRequest(put("/api/v1/issues/{id}/epic", foreignIssueId)
				.contentType(MediaType.APPLICATION_JSON).content("{\"id\":\"" + epicId + "\"}"), owner))
				.andExpect(status().isNotFound());
	}

	@Test
	void workflowTransitionsAndSavedFiltersAreProjectScoped() throws Exception {
		registerUser();
		Cookie owner = loginAndGetAccessCookie();
		String workspaceId = createWorkspaceFor(owner);
		String projectId = createProjectFor(owner, workspaceId);
		String issueId = createIssue(owner, projectId, "Workflow task");

		mockMvc.perform(get("/api/v1/projects/{id}/workflow", projectId).cookie(owner))
				.andExpect(status().isOk()).andExpect(jsonPath("$.statuses.length()").value(4))
				.andExpect(jsonPath("$.transitions.length()").value(6));
		mockMvc.perform(authenticatedCsrfRequest(put("/api/v1/projects/{id}/workflow/transitions", projectId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("[{\"from\":\"TODO\",\"to\":\"DONE\"}]"), owner))
				.andExpect(status().isOk()).andExpect(jsonPath("$.transitions.length()").value(1));
		mockMvc.perform(authenticatedCsrfRequest(patch("/api/v1/issues/{id}", issueId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"Workflow task\",\"description\":null,\"type\":\"TASK\","
						+ "\"status\":\"IN_PROGRESS\",\"priority\":\"MEDIUM\",\"assigneeId\":null,\"dueDate\":null}"), owner))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
		mockMvc.perform(authenticatedCsrfRequest(patch("/api/v1/issues/{id}", issueId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"Workflow task\",\"description\":null,\"type\":\"TASK\","
						+ "\"status\":\"DONE\",\"priority\":\"MEDIUM\",\"assigneeId\":null,\"dueDate\":\"2027-02-01\"}"), owner))
				.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DONE"));

		MvcResult saved = mockMvc.perform(authenticatedCsrfRequest(post("/api/v1/projects/{id}/filters", projectId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Due done\",\"visibility\":\"WORKSPACE\",\"status\":\"DONE\","
						+ "\"priority\":null,\"type\":null,\"assigneeId\":null,\"unassigned\":false,"
						+ "\"sprintId\":null,\"epicId\":null,\"dueFrom\":\"2027-01-01\",\"dueTo\":\"2027-03-01\",\"search\":\"workflow\"}"), owner))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.visibility").value("WORKSPACE"))
				.andExpect(jsonPath("$.dueFrom").value("2027-01-01")).andReturn();
		String filterId = objectMapper.readTree(saved.getResponse().getContentAsString()).get("id").asText();
		mockMvc.perform(get("/api/v1/projects/{id}/filters", projectId).cookie(owner))
				.andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(filterId));
		mockMvc.perform(get("/api/v1/projects/{id}/issues", projectId).param("status", "DONE")
				.param("dueFrom", "2027-01-01").param("dueTo", "2027-03-01").param("search", "workflow").cookie(owner))
				.andExpect(status().isOk()).andExpect(jsonPath("$.totalItems").value(1));
	}

	private String createIssue(Cookie owner, String projectId, String title) throws Exception {
		MvcResult created = mockMvc.perform(authenticatedCsrfRequest(post("/api/v1/projects/{id}/issues", projectId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"" + title + "\",\"type\":\"TASK\"}"), owner))
				.andExpect(status().isCreated()).andReturn();
		return objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();
	}

	@Test
	void issueAssigneeAndReadsAreRestrictedToWorkspaceMembers() throws Exception {
		registerUser();
		Cookie owner = loginAndGetAccessCookie();
		String workspaceId = createWorkspaceFor(owner);
		String projectId = createProjectFor(owner, workspaceId);
		mockMvc.perform(registrationRequest("{\"email\":\"outsider@example.com\","
				+ "\"password\":\"long-enough-password\",\"displayName\":\"Outsider\"}"))
				.andExpect(status().isCreated());
		UUID outsiderId = userRepository.findByEmail("outsider@example.com").orElseThrow().getId();
		Cookie outsider = loginAndGetAccessCookie("outsider@example.com");

		mockMvc.perform(authenticatedCsrfRequest(post("/api/v1/projects/{id}/issues", projectId)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"title\":\"Not allowed assignee\",\"type\":\"TASK\","
						+ "\"assigneeId\":\"" + outsiderId + "\"}"), owner))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.assigneeId").exists());
		MvcResult created = mockMvc.perform(authenticatedCsrfRequest(post("/api/v1/projects/{id}/issues", projectId)
					.contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Private issue\",\"type\":\"TASK\"}"), owner))
				.andExpect(status().isCreated()).andReturn();
		String issueId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();
		mockMvc.perform(get("/api/v1/issues/{id}", issueId).cookie(outsider))
				.andExpect(status().isNotFound());
		mockMvc.perform(get("/api/v1/projects/{id}/issues", projectId).param("status", "UNKNOWN").cookie(owner))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
		mockMvc.perform(get("/api/v1/projects/{id}/issues", projectId).param("size", "101").cookie(owner))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.size").exists());
	}

	@Test
	void commentsActivityNotificationsAndStatusTransitionsRespectIssueAccess() throws Exception {
		registerUser();
		Cookie owner = loginAndGetAccessCookie();
		String workspaceId = createWorkspaceFor(owner);
		String projectId = createProjectFor(owner, workspaceId);
		mockMvc.perform(registrationRequest("{\"email\":\"collaborator@example.com\","
				+ "\"password\":\"long-enough-password\",\"displayName\":\"Collaborator\"}"))
				.andExpect(status().isCreated());
		mockMvc.perform(authenticatedCsrfRequest(post("/api/v1/workspaces/{id}/members", workspaceId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"collaborator@example.com\",\"role\":\"MEMBER\"}"), owner))
				.andExpect(status().isCreated());
		UUID collaboratorId = userRepository.findByEmail("collaborator@example.com").orElseThrow().getId();
		Cookie collaborator = loginAndGetAccessCookie("collaborator@example.com");
		MvcResult created = mockMvc.perform(authenticatedCsrfRequest(post("/api/v1/projects/{id}/issues", projectId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"Collaborative issue\",\"type\":\"TASK\",\"assigneeId\":\"" + collaboratorId + "\"}"), owner))
				.andExpect(status().isCreated()).andReturn();
		String issueId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();
		mockMvc.perform(registrationRequest("{\"email\":\"phasef-outsider@example.com\","
				+ "\"password\":\"long-enough-password\",\"displayName\":\"Outsider\"}"))
				.andExpect(status().isCreated());
		Cookie outsider = loginAndGetAccessCookie("phasef-outsider@example.com");
		mockMvc.perform(get("/api/v1/issues/{id}/comments", issueId).cookie(outsider))
				.andExpect(status().isNotFound());
		mockMvc.perform(get("/api/v1/issues/{id}/activities", issueId).cookie(outsider))
				.andExpect(status().isNotFound());
		mockMvc.perform(get("/api/v1/users/me/notifications").param("unreadOnly", "true").cookie(collaborator))
				.andExpect(status().isOk()).andExpect(jsonPath("$.totalItems").value(1))
				.andExpect(jsonPath("$.items[0].eventType").value("ISSUE_ASSIGNED"));
		mockMvc.perform(authenticatedCsrfRequest(post("/api/v1/issues/{id}/comments", issueId)
				.contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"I will take a look\"}"), collaborator))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.authorName").value("Collaborator"));
		mockMvc.perform(get("/api/v1/issues/{id}/comments", issueId).cookie(owner))
				.andExpect(status().isOk()).andExpect(jsonPath("$.totalItems").value(1));
		mockMvc.perform(authenticatedCsrfRequest(patch("/api/v1/issues/{id}", issueId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"Collaborative issue\",\"type\":\"TASK\",\"status\":\"DONE\","
						+ "\"priority\":\"MEDIUM\",\"assigneeId\":\"" + collaboratorId + "\"}"), collaborator))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
		mockMvc.perform(authenticatedCsrfRequest(patch("/api/v1/issues/{id}", issueId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"Collaborative issue\",\"type\":\"TASK\",\"status\":\"IN_PROGRESS\","
						+ "\"priority\":\"MEDIUM\",\"assigneeId\":\"" + collaboratorId + "\"}"), collaborator))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/v1/issues/{id}/activities", issueId).cookie(owner))
				.andExpect(status().isOk()).andExpect(jsonPath("$.totalItems").value(3));
		mockMvc.perform(get("/api/v1/users/me/notifications").param("unreadOnly", "true").cookie(owner))
				.andExpect(status().isOk()).andExpect(jsonPath("$.totalItems").value(1))
				.andExpect(jsonPath("$.items[0].eventType").value("ISSUE_COMMENTED"));
		String notificationId = objectMapper.readTree(mockMvc.perform(get("/api/v1/users/me/notifications").cookie(owner))
				.andReturn().getResponse().getContentAsString()).get("items").get(0).get("id").asText();
		mockMvc.perform(authenticatedCsrfRequest(patch("/api/v1/notifications/{id}/read", notificationId), collaborator))
				.andExpect(status().isNotFound());
		mockMvc.perform(authenticatedCsrfRequest(patch("/api/v1/notifications/{id}/read", notificationId), owner))
				.andExpect(status().isOk()).andExpect(jsonPath("$.readAt").exists());
		mockMvc.perform(authenticatedCsrfRequest(delete("/api/v1/workspaces/{workspaceId}/members/{userId}", workspaceId, collaboratorId), owner))
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/v1/users/me/notifications").param("unreadOnly", "true").cookie(collaborator))
				.andExpect(status().isOk()).andExpect(jsonPath("$.totalItems").value(0));
	}

	private String validRegistrationJson() {
		return "{\"email\":\"Loc@Example.com\","
				+ "\"password\":\"long-enough-password\",\"displayName\":\" Loc Nguyen \"}";
	}

	private String createWorkspaceFor(Cookie owner) throws Exception {
		MvcResult created = mockMvc.perform(authenticatedCsrfRequest(post("/api/v1/workspaces")
				.contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Issue workspace\"}"), owner))
				.andExpect(status().isCreated()).andReturn();
		return objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();
	}

	private String createProjectFor(Cookie owner, String workspaceId) throws Exception {
		MvcResult created = mockMvc.perform(authenticatedCsrfRequest(post("/api/v1/workspaces/{id}/projects", workspaceId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Issue project\",\"key\":\"ISS\"}"), owner))
				.andExpect(status().isCreated()).andReturn();
		return objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();
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
		return loginAndGetAccessCookie("loc@example.com");
	}

	private Cookie loginAndGetAccessCookie(String email) throws Exception {
		MvcResult result = mockMvc.perform(csrfRequest(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"" + email + "\",\"password\":\"long-enough-password\"}")))
				.andExpect(status().isOk()).andReturn();
		Cookie cookie = result.getResponse().getCookie("CHILA_ACCESS");
		assertNotNull(cookie);
		return cookie;
	}

	private MockHttpServletRequestBuilder authenticatedCsrfRequest(
			MockHttpServletRequestBuilder request, Cookie accessCookie) throws Exception {
		MvcResult bootstrap = mockMvc.perform(get("/api/v1/auth/csrf"))
				.andExpect(status().isNoContent()).andReturn();
		Cookie csrfCookie = bootstrap.getResponse().getCookie("XSRF-TOKEN");
		assertNotNull(csrfCookie);
		return request.cookie(accessCookie, csrfCookie).header("X-XSRF-TOKEN", csrfCookie.getValue());
	}

	private record RegistrationPayload(String email, String password, String displayName) {
	}
}
