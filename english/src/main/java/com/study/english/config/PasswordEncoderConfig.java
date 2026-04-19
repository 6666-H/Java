package com.study.english.config;

import com.study.english.security.Md5KeyPasswordEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/** 独立配置，避免 SecurityConfig -> JwtAuthFilter -> UserService -> PasswordEncoder 的循环依赖 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new Md5KeyPasswordEncoder();
    }
}
