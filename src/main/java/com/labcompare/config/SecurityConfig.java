package com.labcompare.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/labs/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/tests/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/prices/**").permitAll()
                // User endpoints
                .requestMatchers(HttpMethod.POST, "/api/bookings").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/bookings/*/qr").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/bookings/*").authenticated()
                .requestMatchers(HttpMethod.PATCH, "/api/bookings/*/cancel").authenticated()
                // All bookings list: must be authenticated (controller enforces USER vs ADMIN scope)
                .requestMatchers(HttpMethod.GET, "/api/bookings").authenticated()
                // Admin-only write operations — ADMIN or SUPERADMIN
                .requestMatchers(HttpMethod.POST, "/api/labs/**").hasAnyRole("ADMIN", "SUPERADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/labs/**").hasAnyRole("ADMIN", "SUPERADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/labs/**").hasAnyRole("ADMIN", "SUPERADMIN")
                .requestMatchers(HttpMethod.POST, "/api/tests/**").hasAnyRole("ADMIN", "SUPERADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/tests/**").hasAnyRole("ADMIN", "SUPERADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/tests/**").hasAnyRole("ADMIN", "SUPERADMIN")
                .requestMatchers(HttpMethod.POST, "/api/prices/**").hasAnyRole("ADMIN", "SUPERADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/prices/**").hasAnyRole("ADMIN", "SUPERADMIN")
                // User management — SUPERADMIN only
                .requestMatchers("/api/users/**").hasRole("SUPERADMIN")
                // Frontend static
                .requestMatchers("/", "/index.html", "/*.js", "/*.css", "/*.ico",
                                 "/h2-console/**", "/favicon.ico").permitAll()
                .anyRequest().permitAll()
            )
            .headers(h -> h.frameOptions(f -> f.sameOrigin()))
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
