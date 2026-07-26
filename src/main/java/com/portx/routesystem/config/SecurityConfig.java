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
 * SecurityConfig - Configures Spring Security for PortX application.
 *
 * Key Configurations:
 * 1. Dual authentication: Session-based form login for Thymeleaf pages, JWT for REST API (/api/**)
 * 2. Public access to landing page (/), services, about, contact, and auth pages (/login, /register)
 * 3. Role-based access control: ADMIN, DISPATCHER, DRIVER
 * 4. Custom logout redirect: Redirects directly to the home/landing page (/) upon logout.
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
            // Disable CSRF only for REST API calls; Thymeleaf form requests retain protection
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

                // Admin-only REST operations
                .requestMatchers(HttpMethod.POST, "/api/drivers/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/drivers/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/drivers/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/vehicles/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/vehicles/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/vehicles/**").hasRole("ADMIN")

                // Admin & Dispatcher operations
                .requestMatchers(HttpMethod.POST, "/api/deliveries/**").hasAnyRole("ADMIN", "DISPATCHER")
                .requestMatchers("/admin/**").hasAnyRole("ADMIN", "DISPATCHER")
                .requestMatchers("/dispatcher/**").hasAnyRole("ADMIN", "DISPATCHER")

                // Driver portal pages
                .requestMatchers("/driver/**").hasAnyRole("ADMIN", "DRIVER")

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
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout")) // Allows GET and POST /logout
                .logoutSuccessUrl("/") // Redirects to Home/Landing page
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
