package com.bcp.workshop.auth;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.util.Map;

@Path("/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {
    private static final Logger LOG = Logger.getLogger(AuthResource.class);
    private static final String INTERNAL_ERROR_MESSAGE = "Authentication failed";

    @Inject
    LoginService loginService;

    @POST
    @Path("/login")
    public Response login(LoginRequest request) {
        try {
            return toHttpResponse(loginService.login(request));
        } catch (Exception error) {
            LOG.error("Authentication request failed", error);
            return Response.status(500).entity(Map.of("message", INTERNAL_ERROR_MESSAGE)).build();
        }
    }

    private Response toHttpResponse(LoginService.LoginOutcome outcome) {
        if (outcome instanceof LoginService.InvalidRequest invalidRequest) {
            return Response.status(400).entity(Map.of("message", invalidRequest.message())).build();
        }
        if (outcome instanceof LoginService.InvalidCredentials invalidCredentials) {
            return Response.status(401).entity(Map.of("message", invalidCredentials.message())).build();
        }
        if (outcome instanceof LoginService.Authenticated authenticated) {
            return Response.ok(authenticated.response()).build();
        }
        throw new IllegalStateException("Unsupported login outcome");
    }
}
