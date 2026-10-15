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
