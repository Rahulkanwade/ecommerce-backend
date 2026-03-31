package com.ecommerce.backend.config;

import com.ecommerce.backend.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    @Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http)
        throws Exception {

    http
        .csrf(AbstractHttpConfigurer::disable)

        .authorizeHttpRequests(auth -> auth

            // ✅ Public endpoints
            .requestMatchers("/api/auth/**").permitAll()
            .requestMatchers("/error").permitAll()
            .requestMatchers("/api/payments/success").permitAll()
            .requestMatchers("/api/payments/cancel").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()

            // ✅ Admin only
            .requestMatchers(HttpMethod.POST, "/api/products/**")
                .hasAuthority("ROLE_ADMIN")
            .requestMatchers(HttpMethod.PUT, "/api/products/**")
                .hasAuthority("ROLE_ADMIN")
            .requestMatchers(HttpMethod.DELETE, "/api/products/**")
                .hasAuthority("ROLE_ADMIN")
            .requestMatchers("/api/orders/admin/**")
                .hasAuthority("ROLE_ADMIN")

            // ✅ Any authenticated user
            .requestMatchers("/api/cart/**").authenticated()
            .requestMatchers("/api/orders/**").authenticated()
            .requestMatchers("/api/payments/**").authenticated()

            .anyRequest().authenticated()
        )

        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        )

        .authenticationProvider(authenticationProvider)

        .addFilterBefore(
            jwtAuthFilter,
            UsernamePasswordAuthenticationFilter.class
        );

    return http.build();
}}