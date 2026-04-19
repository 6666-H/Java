package com.study.english.config;

import com.study.english.security.JwtAuthFilter;
import com.study.english.security.JwtUtils;
import com.study.english.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
public class JwtAuthFilterConfig {

    @Bean
    public JwtAuthFilter jwtAuthFilter(JwtUtils jwtUtils, @Lazy UserService userService) {
        return new JwtAuthFilter(jwtUtils, userService);
    }
}
