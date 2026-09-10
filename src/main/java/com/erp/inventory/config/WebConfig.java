package com.erp.inventory.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

@Configuration
public class WebConfig {

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
