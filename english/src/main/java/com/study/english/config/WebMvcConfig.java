package com.study.english.config;

import com.study.english.security.AuthRequiredInterceptor;
import com.study.english.security.QuotaInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthRequiredInterceptor authRequiredInterceptor;
    private final QuotaInterceptor quotaInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authRequiredInterceptor)
                .addPathPatterns("/api/**", "/admin/**", "/tenant/**")
                .order(1);
        registry.addInterceptor(quotaInterceptor)
                .addPathPatterns("/api/**", "/admin/**", "/tenant/**")
                .order(2);
    }
}
