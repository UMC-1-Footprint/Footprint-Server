package com.umc.footprint.config.Secret;

import com.umc.footprint.config.properties.AppProperties;
import com.umc.footprint.config.properties.CorsProperties;
import com.umc.footprint.oauth.entity.RoleType;
import com.umc.footprint.oauth.exception.RestAuthenticationEntryPoint;
import com.umc.footprint.oauth.filter.TokenAuthenticationFilter;
import com.umc.footprint.oauth.handler.OAuth2AuthenticationFailureHandler;
import com.umc.footprint.oauth.handler.OAuth2AuthenticationSuccessHandler;
import com.umc.footprint.oauth.handler.TokenAccessDeniedHandler;
import com.umc.footprint.oauth.repository.OAuth2AuthorizationRequestBasedOnCookieRepository;
import com.umc.footprint.oauth.service.CustomOAuth2UserService;
import com.umc.footprint.oauth.service.CustomUserDetailsService;
import com.umc.footprint.oauth.token.AuthTokenProvider;
import com.umc.footprint.src.users.jpaRepository.UserRefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.BeanIds;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.servlet.Filter;
import java.util.Arrays;

@Configuration
@RequiredArgsConstructor
@EnableWebSecurity // spring security 활성화
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    private final CorsProperties corsProperties;
    private final AppProperties appProperties;
    private final AuthTokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final CustomOAuth2UserService oAuth2UserService;
    private final TokenAccessDeniedHandler tokenAccessDeniedHandler;
    private final UserRefreshTokenRepository userRefreshTokenRepository;

    // UserDetailsService 설정

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService)
                .passwordEncoder(passwordEncorder());
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                // CORS 허용
                .cors()
                .and()
                    // 토큰을 사용하기 위해 sessionCreationPolicy 를 STATELESS 로 설정 (Session 비활성화)
                    .sessionManagement()
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                    // CSRF 비활성화
                    .csrf().disable()
                    // 로그인폼 비활성화
                    .formLogin().disable()
                    // 기본 로그인 창 비활성화
                    .httpBasic().disable()
                    .exceptionHandling()
                    .authenticationEntryPoint(new RestAuthenticationEntryPoint())
                    .accessDeniedHandler(tokenAccessDeniedHandler)
                .and()
                    .authorizeRequests()
                    .requestMatchers(CorsUtils::isPreFlightRequest).permitAll()
                        .antMatchers("/api/**").hasAnyAuthority(RoleType.USER.getCode())
//                        .antMatchers("/users/**").hasAnyAuthority(RoleType.USER.getCode())
//                        .antMatchers("/walks/**").hasAnyAuthority(RoleType.USER.getCode())
                        .antMatchers("/api/**/admin/**").hasAnyAuthority(RoleType.ADMIN.getCode())
                        .anyRequest().authenticated()
                .and()
                    .oauth2Login()
                        .authorizationEndpoint()
                        // 클라이언트의 최초 로그인 uri 
                        .baseUri("/oauth2/authorization")
                    .authorizationRequestRepository(oAuth2AuthorizationRequestBasedOnCookieRepository())
                .and()
                    .redirectionEndpoint()
                        // redirect uri
                        .baseUri("/*/oauth2/code/*")
                .and()
                    .userInfoEndpoint()
                        .userService(oAuth2UserService)
                .and()
                    .successHandler(oAuth2AuthenticationSuccessHandler())
                    .failureHandler(oAuth2AuthenticationFailureHandler());

        // UsernamePasswordAuthenticationFilter 앞에 custom 필터 추가
        http.addFilterBefore(tokenAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
    }

    // auth 매니저 설정
    @Override
    @Bean(BeanIds.AUTHENTICATION_MANAGER)
    protected AuthenticationManager authenticationManager() throws Exception {
        return super.authenticationManager();
    }

    // security 설정 시 사용할 인코더 설정
    @Bean
    public PasswordEncoder passwordEncorder() {
        return new BCryptPasswordEncoder();
    }

    // 토큰 필터 설정
    @Bean
    public TokenAuthenticationFilter tokenAuthenticationFilter() {
        return new TokenAuthenticationFilter(tokenProvider);
    }

    /**
     * 인증 응답을 저장하고 검증
     */
    @Bean
    public OAuth2AuthorizationRequestBasedOnCookieRepository oAuth2AuthorizationRequestBasedOnCookieRepository() {
        return new OAuth2AuthorizationRequestBasedOnCookieRepository();
    }

    // Oauth 인증 성공 핸들러
    @Bean
    public AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler() {
        return new OAuth2AuthenticationSuccessHandler(
                tokenProvider,
                appProperties,
                userRefreshTokenRepository,
                oAuth2AuthorizationRequestBasedOnCookieRepository()
        );
    }

    // Oauth 인증 실패 핸들러
    @Bean
    public AuthenticationFailureHandler oAuth2AuthenticationFailureHandler() {
        return new OAuth2AuthenticationFailureHandler(oAuth2AuthorizationRequestBasedOnCookieRepository());
    }

    // Cors 설정
    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource corsConfigurationSource = new UrlBasedCorsConfigurationSource();

        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowedHeaders(Arrays.asList(corsProperties.getAllowedHeaders().split(",")));
        corsConfig.setAllowedMethods(Arrays.asList(corsProperties.getAllowedMethods().split(",")));
        corsConfig.setAllowedOrigins(Arrays.asList(corsProperties.getAllowedOrigins().split(",")));
        corsConfig.setAllowCredentials(true);
        corsConfig.setMaxAge(corsConfig.getMaxAge());

        corsConfigurationSource.registerCorsConfiguration("/**", corsConfig);
        return corsConfigurationSource;
    }
}
