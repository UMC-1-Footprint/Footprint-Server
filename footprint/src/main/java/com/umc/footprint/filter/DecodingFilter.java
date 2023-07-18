package com.umc.footprint.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DecodingFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(DecodingFilter.class);
    private static Boolean isEncrypted;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("Start Decoding");
        Filter.super.init(filterConfig);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;

        try{
            logger.info("Request URI: {}", req.getRequestURL());

            log.info(req.getMethod());

            if(req.getMethod().equals("POST") || req.getMethod().equals("PATCH")){
                RequestBodyDecryptWrapper requestWrapper = new RequestBodyDecryptWrapper(req);

                chain.doFilter(requestWrapper, response);   // ** doFilter **
            } else {
                chain.doFilter(request, response);   // ** doFilter **
            }

            logger.info("Return URI: {}, method: {}", req.getRequestURL(), req.getMethod());
        } catch (Exception exception){
            logger.error("디코딩이 불가합니다.");
        }

    }

    @Override
    public void destroy() {
        logger.info("End Decoding");
        Filter.super.destroy();
    }

}
