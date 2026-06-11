package com.bank.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Intercepts every HTTP request and validates the JWT Bearer token.
 *
 * <p>DESIGN DECISIONS:
 *
 * <p>1. Extends OncePerRequestFilter — guarantees this filter runs exactly
 *    once per request. Without this, in some servlet configurations a filter
 *    could run multiple times (e.g. on forwards/includes). Spring provides
 *    this base class precisely to prevent that issue.
 *
 * <p>2. Filter vs HandlerInterceptor.
 *    We use a servlet Filter (not a Spring HandlerInterceptor) because
 *    authentication must happen BEFORE Spring MVC dispatches the request.
 *    A HandlerInterceptor runs after DispatcherServlet, which is too late —
 *    security decisions must be made at the filter chain level.
 *
 * <p>3. We call shouldNotFilter() to skip public endpoints.
 *    ALTERNATIVE: Let the filter run on all requests and rely on
 *    SecurityConfig to permit them. Both work, but skipping the filter
 *    entirely on public routes is slightly more efficient and explicit.
 *
 * <p>4. We do NOT throw exceptions from this filter.
 *    If the token is invalid, we simply don't set the authentication.
 *    Spring Security then sees an unauthenticated request and returns 401.
 *    Throwing exceptions from filters bypasses the global exception handler
 *    and produces inconsistent error responses.
 *
 * <p>FLOW:
 * Request arrives
 *   → Extract "Authorization: Bearer <token>" header
 *   → Validate token signature and expiry via JwtService
 *   → Load UserDetails from DB via UserDetailsServiceImpl
 *   → Set Authentication in SecurityContextHolder
 *   → Continue filter chain
 * If any step fails → continue chain without setting auth → Spring returns 401
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTH_HEADER = "Authorization";

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;

    public JwtAuthFilter(JwtService jwtService, UserDetailsServiceImpl userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader(AUTH_HEADER);

        // No Authorization header or doesn't start with "Bearer " — skip
        // This handles public endpoints and non-JWT requests gracefully
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Strip "Bearer " prefix to get the raw token
        final String token = authHeader.substring(BEARER_PREFIX.length());

        // Extract the user ID from the token
        // Returns null if token is malformed, expired, or signature invalid
        final UUID userId = jwtService.extractUserId(token);

        if (userId == null) {
            log.debug("Could not extract user ID from token");
            filterChain.doFilter(request, response);
            return;
        }

        // Only process if not already authenticated
        // DECISION: Check SecurityContextHolder first.
        // WHY: In theory, earlier filters could have already authenticated
        // the request. We respect that and don't overwrite it.
        if (SecurityContextHolder.getContext().getAuthentication() == null) {

            // Load UserDetails by UUID string
            // UserDetailsService.loadUserByUsername() — "username" = UUID in our system
            UserDetails userDetails = userDetailsService.loadUserByUsername(
                    userId.toString()
            );

            // Validate the token against the loaded user
            if (jwtService.isTokenValid(token, userDetails)) {

                // Create an authentication token and set it in the security context
                // This is the standard Spring Security way to mark a request as authenticated
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,               // credentials — null after authentication
                                userDetails.getAuthorities()
                        );

                // Attach request details (IP address, session ID) to the auth token
                // Useful for audit logging and Spring Security's built-in events
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // Set the authentication in the SecurityContext
                // From this point forward, the request is treated as authenticated
                SecurityContextHolder.getContext().setAuthentication(authToken);

                log.debug("Authenticated user: {}", userId);
            }
        }

        // Always continue the filter chain — even if auth failed
        // Spring Security's authorization checks will reject unauthenticated requests
        filterChain.doFilter(request, response);
    }

    /**
     * Skip this filter entirely for public endpoints.
     *
     * DECISION: Explicit skip list rather than pattern matching.
     * WHY: Prevents accidental exposure. If you use pattern matching
     * and get the pattern wrong, you might skip auth on protected routes.
     * An explicit list is harder to get wrong.
     *
     * Note: context-path (/api/v1) is already stripped by the time
     * the filter sees the URI, so we match against /auth/**
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/v1/auth/") ||
                (path.equals("/v1/users") && request.getMethod().equals("POST"));
    }
}
