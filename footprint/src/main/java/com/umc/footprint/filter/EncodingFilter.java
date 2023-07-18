package com.umc.footprint.filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.umc.footprint.utils.AES128;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EncodingFilter implements Filter{

    private static final Logger logger = LoggerFactory.getLogger(DecodingFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletResponse res = (HttpServletResponse) response;
        HttpServletRequest req = (HttpServletRequest) request;

        try{
            ResponseBodyEncryptWrapper responseWrapper = new ResponseBodyEncryptWrapper(res);

            // 암호화 되지 않고 들어온 Request인지 확인
            // 파라미터로 들어온 request로 부터 isEncrypted 정보 얻어옴
            String isEncrypted = req.getHeader("isEncrypted");

            chain.doFilter(request, responseWrapper);   // ** doFilter **

            // encode response body
            String url = req.getServletPath();
            log.info("url : " + url);

            if(!(isEncrypted == null) && !(url.equals("/walks/check/encrypt")) && !(url.equals("/walks/check/decrypt"))){
                String responseMessage = new String(responseWrapper.getDataStream(), StandardCharsets.UTF_8);

                JSONObject jsonObject = new JSONObject(responseMessage);

                int startIndex = responseMessage.indexOf("result") + 8;
                int endIndex = responseMessage.lastIndexOf("}");

                System.out.println("responseMessage = " + responseMessage);

                // 최종 메시지
                StringBuffer finalResponseMessage = new StringBuffer(responseMessage);

                // 오류 안났을 때만
                if (responseMessage.indexOf("result") != -1){
                    // result 부분 암호화 쌍따음표 붙여서
                    String encodedResultPart = '\u0022' + (AES128.encrypt(jsonObject.getString("result"))) + '\u0022';

                    // result 부분 암호화한걸로 치환
                    finalResponseMessage.replace(startIndex, endIndex, encodedResultPart);
                }

                response.getOutputStream().write(finalResponseMessage.toString().getBytes());
            }else{
                log.info("responseData : " + new String(responseWrapper.getDataStream(), StandardCharsets.UTF_8));
                response.getOutputStream().write(responseWrapper.getDataStream());
            }

        } catch (Exception exception){
            logger.error("인코딩이 불가합니다.");
        }

    }

    @Override
    public void destroy() {
        logger.info("End Decoding");
        Filter.super.destroy();
    }

}
