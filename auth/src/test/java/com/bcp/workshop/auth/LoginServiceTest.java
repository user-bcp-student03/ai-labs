package com.bcp.workshop.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LoginServiceTest {
    @Test
    void rejectsNullRequestBeforeAccessingDatabase() {
        LoginService loginService = new LoginService(new LegacyDatabase(), new LegacyTokenUtil());

        LoginService.LoginOutcome outcome = loginService.login(null);

        LoginService.InvalidRequest invalidRequest =
                assertInstanceOf(LoginService.InvalidRequest.class, outcome);
        assertEquals("Request is null", invalidRequest.message());
    }

    @Test
    void rejectsBlankUsernameBeforeAccessingDatabase() {
        LoginRequest request = new LoginRequest();
        request.username = " ";
        request.password = "Workshop123!";

        LoginService.LoginOutcome outcome = serviceReturning(null).login(request);

        LoginService.InvalidRequest invalidRequest =
                assertInstanceOf(LoginService.InvalidRequest.class, outcome);
        assertEquals("Username is required", invalidRequest.message());
    }

    @Test
    void rejectsBlankPasswordBeforeAccessingDatabase() {
        LoginRequest request = new LoginRequest();
        request.username = "student";
        request.password = " ";

        LoginService.LoginOutcome outcome = serviceReturning(null).login(request);

        LoginService.InvalidRequest invalidRequest =
                assertInstanceOf(LoginService.InvalidRequest.class, outcome);
        assertEquals("Password is required", invalidRequest.message());
    }

    @Test
    void reportsInvalidCredentialsWhenUserIsNotFound() {
        LoginService.LoginOutcome outcome = serviceReturning(null).login(requestWithCredentials());

        LoginService.InvalidCredentials invalidCredentials =
                assertInstanceOf(LoginService.InvalidCredentials.class, outcome);
        assertEquals("Invalid user student or password", invalidCredentials.message());
    }

    @Test
    void mapsAuthenticatedUserToResponseDto() {
        UserRecord user = new UserRecord(1, "student", "<b>Workshop</b>", "STUDENT");
        LoginService.LoginOutcome outcome = serviceReturning(user).login(requestWithCredentials());

        LoginService.Authenticated authenticated =
                assertInstanceOf(LoginService.Authenticated.class, outcome);
        assertNotNull(authenticated.response().token());
        assertEquals(AuthConstants.TOKEN_EXPIRY_SECONDS, authenticated.response().expiresIn());
        assertEquals("student", authenticated.response().user().username());
        assertEquals("&lt;b&gt;Workshop&lt;/b&gt;", authenticated.response().user().displayName());
        assertEquals("STUDENT", authenticated.response().user().role());
        assertEquals(AuthConstants.AUTHENTICATED_DEBUG_VALUE, authenticated.response().debug());
    }

    private LoginService serviceReturning(UserRecord user) {
        LegacyDatabase database = new LegacyDatabase() {
            @Override
            public UserRecord findByCredentials(String username, String password) {
                return user;
            }
        };
        LegacyTokenUtil tokenUtil = new LegacyTokenUtil();
        tokenUtil.secret = "unit-test-secret";
        return new LoginService(database, tokenUtil);
    }

    private LoginRequest requestWithCredentials() {
        LoginRequest request = new LoginRequest();
        request.username = "student";
        request.password = "Workshop123!";
        return request;
    }
}
