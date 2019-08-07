package com.code.zuul.fallback;

import com.code.zuul.model.Msg;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.netflix.zuul.filters.route.FallbackProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * create by liuliang
 * on 2019-08-07  10:37
 */
@Slf4j
@Configuration
public class MicroFallbackProvider implements FallbackProvider {

    @Override
    public String getRoute() {
        //表明为哪个为服务提供回退，* 表示全部
        return "*";
    }


    /**
     * 这是路由熔断处理方式
     *
     * @param route
     * @param cause
     * @return
     */
    @Override
    public ClientHttpResponse fallbackResponse(String route, Throwable cause) {
        log.info("route:{}", route);
        log.info("exception:", cause.getMessage());
        return new ClientHttpResponse() {
            @Override
            public HttpHeaders getHeaders() {
                HttpHeaders headers = new HttpHeaders();
                //和body中的内容编码一致，否则容易乱码
                headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
                return headers;
            }

            @Override
            public InputStream getBody() throws IOException {
                //响应体
                Msg msg = new Msg();
                msg.setCode(502);
                msg.setMsg("呜呼·· " + route + " 服务器正在开小差");
                ObjectMapper objectMapper = new ObjectMapper();
                String content = objectMapper.writeValueAsString(msg);
                return new ByteArrayInputStream(content.getBytes());

            }

            @Override
            public HttpStatus getStatusCode() throws IOException {
                return HttpStatus.OK;
            }

            @Override
            public int getRawStatusCode() throws IOException {
                return HttpStatus.OK.value();
            }

            @Override
            public String getStatusText() throws IOException {
                return HttpStatus.OK.getReasonPhrase();
            }

            @Override
            public void close() {

            }
        };
    }
}
