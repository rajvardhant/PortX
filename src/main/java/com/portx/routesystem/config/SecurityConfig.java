package com.portx.routesystem.config;

import com.portx.routesystem.security.CustomUserDetailsService;
import com.portx.routesystem.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * SecurityConfig — Modular Role-Based Security Configuration.
 *
 * ROLE ACCESS MATRIX:
 * 1. ADMIN (ROLE_ADMIN): System manager (Users, Fleet, Vehicles, Customers, Orders, System Dashboard, Reports).
 * 2. DISPATCHER (ROLE_DISPATCHER): Operations manager (Daily dispatch, Driver assignments, Active tracking, Routes).
 * 3. DRIVER (ROLE_DRIVER): Delivery executive (Assigned orders, Pickups, In-transit updates, Delivery completion, History).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final CustomUserDetailsService userDetailsService;

    // Password encoder using BCrypt hashing
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Authentication manager used for manual authentication
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // DAO-based authentication provider
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF for REST API calls; Thymeleaf form requests retain protection
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/**")
            )
            .authorizeHttpRequests(auth -> auth
                // Publicly accessible pages & static assets
                .requestMatchers(
                    "/", "/services", "/about", "/contact",
                    "/login", "/register",
                    "/api/auth/**",
                    "/css/**", "/js/**", "/images/**",
                    "/webjars/**", "/favicon.ico"
                ).permitAll()

                // ── 1. ADMIN MODULE (ROLE_ADMIN Only) ──────────────────────
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/admin/drivers/**").hasRole("ADMIN")
                .requestMatchers("/admin/vehicles/**").hasRole("ADMIN")

                // ── 2. DISPATCHER MODULE (ROLE_ADMIN & ROLE_DISPATCHER) ────
                .requestMatchers("/api/dispatcher/**").hasAnyRole("ADMIN", "DISPATCHER")
                .requestMatchers("/admin/dashboard").hasAnyRole("ADMIN", "DISPATCHER")
                .requestMatchers("/admin/routes/**").hasAnyRole("ADMIN", "DISPATCHER")
                .requestMatchers("/dispatcher/**").hasAnyRole("ADMIN", "DISPATCHER")

                // ── 3. DRIVER MODULE (ROLE_ADMIN & ROLE_DRIVER) ────────────
                .requestMatchers("/api/driver/**").hasAnyRole("ADMIN", "DRIVER")
                .requestMatchers("/driver/**").hasAnyRole("ADMIN", "DRIVER")

                // ── 4. INVOICES (ROLE_ADMIN & ROLE_DISPATCHER) ─────────────
                .requestMatchers("/invoices/**").hasAnyRole("ADMIN", "DISPATCHER")

                // All other requests require authentication
                .anyRequest().authenticated()
            )
            // Allow sessions for Thymeleaf navigation
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            // Form Login configuration
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/login?error")
                .permitAll()
            )
            // Logout configuration - Redirects to Landing Page (/)
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            // 403 Access Denied handling
            .exceptionHandling(ex -> ex
                .accessDeniedPage("/403")
            );

        return http.build();
    }
}
