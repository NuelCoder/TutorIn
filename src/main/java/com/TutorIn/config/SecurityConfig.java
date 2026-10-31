package com.TutorIn.config;

import com.TutorIn.security.CustomUserDetailsService;
import com.TutorIn.security.JwtFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
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
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final CustomUserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 1. ALL Authentication & Recovery Endpoints
                        .requestMatchers("/api/v1/auth/**").permitAll()

                        // 2. PUBLIC User Registration Endpoints
                        .requestMatchers(
                                "/api/v1/users/register-user",
                                "/api/v1/users/verify-user",
                                "/api/v1/users/resend-registration-otp"
                        ).permitAll()

                        // 3. PUBLIC Tutor Registration Endpoints
                        .requestMatchers(
                                "/api/v1/tutors/register-tutor",
                                "/api/v1/tutors/verify-tutor",
                                "/api/v1/tutors/resend-registration-otp"
                        ).permitAll()

                        // 4. PUBLIC Admin Registration Endpoints
                        .requestMatchers(
                                "/api/v1/admins/register-admin",
                                "/api/v1/admins/verify-admin",
                                "/api/v1/admins/resend-registration-otp"
                        ).permitAll()
                        //5 Documentation
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-ui.html"
                        ).permitAll()

                        // 6. EVERY other request requires a valid JWT
                        // (Roles are strictly enforced by @PreAuthorize in the Services/Controllers)
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}