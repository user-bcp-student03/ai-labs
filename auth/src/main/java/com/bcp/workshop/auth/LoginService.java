package com.bcp.workshop.auth;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.commons.text.StringEscapeUtils;

@ApplicationScoped
public class LoginService {
    private static final String REQUEST_IS_NULL = "Request is null";
    private static final String USERNAME_REQUIRED = "Username is required";
    private static final String PASSWORD_REQUIRED = "Password is required";

    private final LegacyDatabase database;
    private final JwtTokenService tokenService;

    @Inject
    public LoginService(LegacyDatabase database, JwtTokenService tokenService) {
        this.database = database;
        this.tokenService = tokenService;
    }

    public LoginOutcome login(LoginRequest request) {
        String validationError = validateRequest(request);
        if (validationError != null) {
            return new InvalidRequest(validationError);
        }

        UserRecord user = database.findByCredentials(request.username, request.password);
        if (user == null) {
            return new InvalidCredentials("Invalid user " + request.username + " or password");
        }

        return new Authenticated(createResponse(user));
    }

    private String validateRequest(LoginRequest request) {
        if (request == null) {
            return REQUEST_IS_NULL;
        }
        if (request.username == null || request.username.trim().isEmpty()) {
            return USERNAME_REQUIRED;
        }
        if (request.password == null || request.password.trim().isEmpty()) {
            return PASSWORD_REQUIRED;
        }
        return null;
    }

    private LoginResponse createResponse(UserRecord user) {
        return new LoginResponse(
                tokenService.create(user.username(), user.role()),
                AuthConstants.TOKEN_EXPIRY_SECONDS,
                new LoginResponse.User(
                        user.username(),
                        StringEscapeUtils.escapeHtml4(user.displayName()),
                        user.role()
                ),
                AuthConstants.AUTHENTICATED_DEBUG_VALUE
        );
    }

    public sealed interface LoginOutcome permits InvalidRequest, InvalidCredentials, Authenticated {}

    public record InvalidRequest(String message) implements LoginOutcome {}

    public record InvalidCredentials(String message) implements LoginOutcome {}

    public record Authenticated(LoginResponse response) implements LoginOutcome {}
}
