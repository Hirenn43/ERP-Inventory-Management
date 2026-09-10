package com.erp.inventory.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ShallowEtagHeaderFilter;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.method.HandlerTypePredicate;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Every {@code @RestController} in {@code com.erp.inventory.controller} is served under
     * {@code /api/v1/...}. Controllers declare only their resource path ({@code "/brokers"});
     * the version prefix is applied here so it can be bumped in one place.
     */
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix("/api/v1",
                HandlerTypePredicate.forBasePackage("com.erp.inventory.controller"));
    }

    /**
     * Emits an {@code ETag} for GET responses and answers {@code 304 Not Modified} to a matching
     * {@code If-None-Match}, so master-data lists/detail reads that haven't changed cost no payload.
     */
    @Bean
    public FilterRegistrationBean<ShallowEtagHeaderFilter> shallowEtagHeaderFilter() {
        FilterRegistrationBean<ShallowEtagHeaderFilter> registration =
                new FilterRegistrationBean<>(new ShallowEtagHeaderFilter());
        registration.addUrlPatterns("/api/*");
        registration.setName("shallowEtagHeaderFilter");
        return registration;
    }
}
