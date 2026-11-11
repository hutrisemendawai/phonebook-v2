package com.pb.phonebook.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.savedrequest.SavedRequest;

import jakarta.servlet.DispatcherType;

@Configuration
public class SecurityConfig {

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .authorizeHttpRequests(auth -> auth
                                                .dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR).permitAll()
                                                // 1. Static resources & public pages
                                                .requestMatchers("/", "/home", "/login", "/panduan", "/user-analog-ancol",
                                                                "/css/**", "/js/**", "/images/**", "/pdf/**", "/register/**",
                                                                "/favicon.ico", "/error")
                                                .permitAll()

                                                // 2. Admin actions for Users (harus dievaluasi sebelum rule publik /users)
                                                .requestMatchers("/users/new", "/users/edit/**", "/users/delete/**",
                                                                "/users/import/**")
                                                .hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.POST, "/users")
                                                .hasRole("ADMIN")

                                                // 3. Admin sections (Companies & Locations)
                                                .requestMatchers("/companies/**", "/locations/**")
                                                .hasRole("ADMIN")

                                                // 4. Public access untuk melihat list users (mendukung /users, /users/, /users/** via GET)
                                                .requestMatchers(HttpMethod.GET, "/users", "/users/", "/users/**")
                                                .permitAll()

                                                // 5. Endpoint lain wajib autentikasi
                                                .anyRequest().authenticated())
                                .formLogin(form -> form
                                                .loginPage("/login")
                                                .successHandler((request, response, authentication) -> {
                                                        var savedRequest = (SavedRequest) request
                                                                        .getSession()
                                                                        .getAttribute("SPRING_SECURITY_SAVED_REQUEST");

                                                        if (savedRequest != null) {
                                                                response.sendRedirect(savedRequest.getRedirectUrl());
                                                        } else {
                                                                response.sendRedirect("/");
                                                        }
                                                })
                                                .permitAll())
                                .logout(logout -> logout
                                                .logoutSuccessUrl("/")
                                                .permitAll())
                                .exceptionHandling(exception -> exception
                                                .authenticationEntryPoint(
                                                                (request, response, authException) -> response
                                                                                .sendRedirect("/login?unauthorized")))
                                .headers(headers -> headers
                                                .frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }
}
