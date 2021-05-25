package com.changgou.gateway.filter;

import com.changgou.gateway.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import org.apache.commons.lang.StringUtils;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 认证过滤，在此处实现认证流程
 * @author Steven
 * @version 1.0
 * @description com.changgou.gateway.filter
 * @date 2019-12-29
 */
@Component
public class AuthorizeFilter implements GlobalFilter, Ordered {

    //令牌的key
    private static final String AUTHORIZE_TOKEN = "Authorization";
    //登录url
    private static final String USER_LOGIN_URL = "http://localhost:9001/oauth/login";


    /**
     * 实现过滤的真实逻辑
     * @param exchange  交换机
     * @param chain 网关过滤器操作对象
     * @return 处理结果
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        //1、获取Request、Response对象-exchange.get...
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        //2、获取请求的URI-request.getURI().getPath()
        String path = request.getURI().getPath();
        //3、如果是登录请求-uri.startsWith，放行-chain.filter
        if(URLFilter.hasAuthorize(path)){
            //放行
            return chain.filter(exchange);
        }else { //4、如果是非登录请求
            //4.1 获取前端传入的令牌-从请求头中获取-request.getHeaders().getFirst
            String token = request.getHeaders().getFirst(AUTHORIZE_TOKEN);
            //4.2 如果头信息中没有，从请求参数中获取-request.getQueryParams().getFirst
            //"  ".isBlank = true
            if (StringUtils.isBlank(token)) {
                token = request.getQueryParams().getFirst(AUTHORIZE_TOKEN);
            }
            //4.3 如果请求参数中没有，从cookie中获取-request.getCookies()-取值前先判断不为空-getFirst
            if (StringUtils.isBlank(token)) {
                HttpCookie cookie = request.getCookies().getFirst(AUTHORIZE_TOKEN);
                if(cookie != null){
                    token = cookie.getValue();
                }
            }
            //4.4 如果以上方式都取不到令牌-返回405错误-response.setStatusCode(405)-return response.setComplete
            if (StringUtils.isBlank(token)) {
                //response.setStatusCode(HttpStatus.METHOD_NOT_ALLOWED);

                //跳转到登录页
                response.setStatusCode(HttpStatus.SEE_OTHER);
                //获取原来请求url加入到参数中
                String url = USER_LOGIN_URL + "?FROM=" + request.getURI();
                //设置Location头信息，相当于跳转页面
                response.getHeaders().add("Location", url);

                return response.setComplete();
            }else{
                try {
                    //4.5 如果获取到了令牌，解析令牌-JwtUtil.parseJWT，放行-chain.filter(exchange)
                    //Claims claims = JwtUtil.parseJWT(token);
                    //4.5.1解析成功-把令牌返回-request.mutate().header(key,value)
                    //request.mutate().header(AUTHORIZE_TOKEN, claims.toString());

                    //修改把AUTHORIZE_TOKEN，作为请求头带到后面的微服务中
                    request.mutate().header(AUTHORIZE_TOKEN, "bearer " + token);
                } catch (Exception e) {
                    e.printStackTrace();
                    //返回认证失败-401
                    response.setStatusCode(HttpStatus.UNAUTHORIZED);
                    return response.setComplete();
                }
            }
        }
        return chain.filter(exchange);
    }

    //设置过滤器的执行顺序，数值越小，越先执行
    @Override
    public int getOrder() {
        return 0;
    }
}
