package org.example.flashsale.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.flashsale.model.Role;
import org.example.flashsale.service.JwtService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length()).trim();

            Optional<Long> userId = jwtService.extractUserId(token);
            Optional<Role> role = jwtService.extractRole(token);
            Optional<String> email = jwtService.extractEmail(token);

            if (userId.isPresent() && role.isPresent() &&
                    SecurityContextHolder.getContext().getAuthentication() == null) {

                UserPrincipal principal = new UserPrincipal(
                        userId.get(),
                        email.orElse(null),
                        role.get()
                );

                var authority = new SimpleGrantedAuthority("ROLE_" + role.get().name());

                var auth = new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of(authority)
                );
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        filterChain.doFilter(request, response);
    }
}