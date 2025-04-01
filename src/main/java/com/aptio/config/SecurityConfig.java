package com.aptio.config;

import com.aptio.service.UserDetailsServiceImpl;
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

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsServiceImpl userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/api-docs/**").permitAll()
                        .requestMatchers("/swagger-ui/**").permitAll()
                        .requestMatchers("/user/appointments").permitAll()
                        .requestMatchers("/user/appointments/").permitAll()
                        .requestMatchers("/user/appointments/**").permitAll()
                        .requestMatchers("/user/appointments/available-slots").permitAll()
                        .requestMatchers("/appointments").permitAll()
                        .requestMatchers("/appointments/").permitAll()
                        .requestMatchers("/appointments/user/appointments").permitAll()
                        .requestMatchers("/appointments/user/appointments/").permitAll()
                        .requestMatchers("/appointments/**").permitAll()
                        .requestMatchers("/service-categories").permitAll()
                        .requestMatchers("/service-categories/**").permitAll()
                        .requestMatchers("/api/v1/service-categories/").permitAll()
                        .requestMatchers("/services").permitAll()
                        .requestMatchers("/services/**").permitAll()
                        .requestMatchers("/api/v1/services/").permitAll()
                        .requestMatchers("/users").permitAll()
                        .requestMatchers("/users/**").permitAll()
                        .requestMatchers("/api/v1/users/").permitAll()
                        .requestMatchers("/customers").permitAll()
                        .requestMatchers("/customers/").permitAll()
                        .requestMatchers("/customers/**").permitAll()
                        .requestMatchers("/settings").permitAll()
                        .requestMatchers("/settings/business").permitAll()
                        .requestMatchers("/settings/**").permitAll()
                        .requestMatchers("/dashboard/stats").permitAll()
                        .requestMatchers("/schedule/staff/").permitAll()
                        .requestMatchers("/schedule/staff/**").permitAll()
                        .requestMatchers("/schedule/range").permitAll()
                        .requestMatchers("/staff/**").permitAll()
                        .requestMatchers("/staff").permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        http.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.disable()));

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}