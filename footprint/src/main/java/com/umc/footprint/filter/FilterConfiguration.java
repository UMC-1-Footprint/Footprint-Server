package com.umc.footprint.filter;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class FilterConfiguration implements WebMvcConfigurer {

    @Bean
    public FilterRegistrationBean<DecodingFilter> decodingFilterRegistrationBean(){
        FilterRegistrationBean<DecodingFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new DecodingFilter());
        registrationBean.addUrlPatterns("/users/infos");
        registrationBean.addUrlPatterns("/users/infos/after");
        registrationBean.addUrlPatterns("/users/auth/login");
        registrationBean.addUrlPatterns("/users/goals");
        registrationBean.addUrlPatterns("/walks");
        registrationBean.addUrlPatterns("/weather");
        registrationBean.addUrlPatterns("/notices/key");
        registrationBean.addUrlPatterns("/courses/list");
        registrationBean.addUrlPatterns("/courses/recommend");

        System.out.println("URL =" + registrationBean.getUrlPatterns());

        return registrationBean;
    }

    @Bean
    public FilterRegistrationBean<EncodingFilter> encodingFilterRegistrationBean(){
        FilterRegistrationBean<EncodingFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new EncodingFilter());
        registrationBean.addUrlPatterns("/users/*");
        registrationBean.addUrlPatterns("/footprints/*");
        registrationBean.addUrlPatterns("/walks/*");
        registrationBean.addUrlPatterns("/weather");
        registrationBean.addUrlPatterns("/notices/*");
        registrationBean.addUrlPatterns("/courses/*");


        System.out.println("URL =" + registrationBean.getUrlPatterns());

        return registrationBean;
    }




}
