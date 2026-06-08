package akakash.backend.monitor;

import akakash.backend.check.MonitorCheckRepository;
import akakash.backend.common.dto.CreateMonitorRequest;
import akakash.backend.common.dto.MonitorResponse;
import akakash.backend.common.exception.GlobalExceptionHandler;
import akakash.backend.common.exception.PlanLimitException;
import akakash.backend.incident.IncidentRepository;
import akakash.backend.user.Plan;
import akakash.backend.user.User;
import akakash.backend.user.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MonitorControllerIntegrationTest {

    @Mock
    private MonitorService monitorService;

    @Mock
    private UserService userService;

    @Mock
    private MonitorCheckRepository monitorCheckRepository;

    @Mock
    private IncidentRepository incidentRepository;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        MonitorController controller = new MonitorController(
                monitorService,
                userService,
                monitorCheckRepository,
                incidentRepository
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(new SpringValidatorAdapter(Validation.buildDefaultValidatorFactory().getValidator()))
                .setCustomArgumentResolvers(new JwtArgumentResolver())
                .build();
    }

    @Test
    void createMonitorMapsJwtUserValidatesJsonAndReturnsCreatedResponse() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID monitorId = UUID.randomUUID();
        User user = user(userId);
        CreateMonitorRequest request = validCreateRequest();
        MonitorResponse response = new MonitorResponse(
                monitorId,
                "API",
                "http",
                "https://example.com",
                null,
                2,
                "GET",
                200,
                10,
                "unknown",
                true,
                OffsetDateTime.now(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                1,
                null,
                null,
                false,
                false
        );

        when(userService.getOrCreateUser("clerk_123", "user@example.com", "Akash"))
                .thenReturn(user);
        when(monitorService.createMonitor(eq(userId), any(CreateMonitorRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/monitors")
                        .requestAttr("jwt", jwt("clerk_123", "user@example.com", "Akash"))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(monitorId.toString()))
                .andExpect(jsonPath("$.name").value("API"))
                .andExpect(jsonPath("$.type").value("http"));

        verify(monitorService).createMonitor(eq(userId), any(CreateMonitorRequest.class));
    }

    @Test
    void createMonitorReturnsValidationErrorForMissingRequiredFields() throws Exception {
        mockMvc.perform(post("/api/monitors")
                        .requestAttr("jwt", jwt("clerk_123", null, null))
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_error"));
    }

    @Test
    void createMonitorMapsPlanLimitExceptionToForbidden() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = user(userId);

        when(userService.getOrCreateUser("clerk_123", "clerk_123", ""))
                .thenReturn(user);
        when(monitorService.createMonitor(eq(userId), any(CreateMonitorRequest.class)))
                .thenThrow(new PlanLimitException("Plan limit reached: 3"));

        mockMvc.perform(post("/api/monitors")
                        .requestAttr("jwt", jwt("clerk_123", null, null))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(validCreateRequest())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("plan_limit"))
                .andExpect(jsonPath("$.message").value("Plan limit reached: 3"));
    }

    @Test
    void deleteMonitorUsesAuthenticatedUserIdAndReturnsNoContent() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID monitorId = UUID.randomUUID();
        User user = user(userId);
        when(userService.getOrCreateUser("clerk_123", "clerk_123", ""))
                .thenReturn(user);

        mockMvc.perform(delete("/api/monitors/{monitorId}", monitorId)
                        .requestAttr("jwt", jwt("clerk_123", null, null)))
                .andExpect(status().isNoContent());

        verify(monitorService).deleteMonitor(monitorId, userId);
    }

    private CreateMonitorRequest validCreateRequest() {
        return new CreateMonitorRequest(
                "API",
                "http",
                "https://example.com",
                2,
                "GET",
                200,
                10,
                null,
                null,
                1,
                null,
                null,
                null,
                null
        );
    }

    private User user(UUID userId) {
        return User.builder()
                .id(userId)
                .clerkUserId("clerk_123")
                .email("user@example.com")
                .plan(Plan.FREE)
                .build();
    }

    private Jwt jwt(String subject, String email, String name) {
        Jwt.Builder builder = Jwt.withTokenValue("token-" + subject)
                .header("alg", "none")
                .subject(subject);
        if (email != null) {
            builder.claim("email", email);
        }
        if (name != null) {
            builder.claim("name", name);
        }
        return builder.build();
    }

    private static class JwtArgumentResolver implements HandlerMethodArgumentResolver {

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
                    && Jwt.class.isAssignableFrom(parameter.getParameterType());
        }

        @Override
        public Object resolveArgument(MethodParameter parameter,
                                      ModelAndViewContainer mavContainer,
                                      NativeWebRequest webRequest,
                                      org.springframework.web.bind.support.WebDataBinderFactory binderFactory) {
            return webRequest.getAttribute("jwt", NativeWebRequest.SCOPE_REQUEST);
        }
    }
}
