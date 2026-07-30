package com.dbtraining.reconx.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter) throws Exception {

        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(
                        s -> s.sessionCreationPolicy(
                                org.springframework.security.config.http.SessionCreationPolicy.STATELESS))

                .headers(h -> h.frameOptions(f -> f.disable())) // allow /h2 in dev
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .headers(h -> h.frameOptions(f -> f.disable())) // allow /h2 in dev
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/auth/login",
                                "/v1/trades/stream",
                                "/actuator/health/**",
                                "/actuator/info",
                                "/actuator/prometheus",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/h2/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/v1/trades/**")
                        .hasAnyRole("VIEWER", "TRADER", "RECON_ANALYST", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/v1/trades").hasAnyRole("TRADER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/v1/trades/**").hasAnyRole("TRADER", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/v1/trades/**").hasAnyRole("TRADER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/v1/trades/**").hasRole("ADMIN")
                        .requestMatchers("/v1/recon/**").hasAnyRole("RECON_ANALYST", "ADMIN")
                        .requestMatchers("/v1/audit/**").hasAnyRole("RECON_ANALYST", "ADMIN")
                        .requestMatchers("/v1/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
