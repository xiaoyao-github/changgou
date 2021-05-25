package com.changgou.test;

import io.jsonwebtoken.*;
import org.junit.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Steven
 * @version 1.0
 * @description com.changgou.test
 * @date 2019-12-29
 */
public class JwtTest {

    //加密-生成令牌
    @Test
    public void testCreateToken(){
        //1、创建Jwt构建器-jwtBuilder = Jwts.builder()
        JwtBuilder jwtBuilder = Jwts.builder();
        //2、设置唯一编号-setId
        jwtBuilder.setId("007");
        //3、设置主题，可以是JSON数据-setSubject()
        jwtBuilder.setSubject("{id:007,name='小姐姐'}");
        //4、设置签发日期-setIssuedAt
        jwtBuilder.setIssuedAt(new Date());
        //5、设置签发人-setIssuer
        jwtBuilder.setIssuer("sz.itheima");
        //6、设置签证-signWith(指定加密算法【HS256】,密钥)
        jwtBuilder.signWith(SignatureAlgorithm.HS256, "sz.itheima.key");

        //设置令牌的过期时间
        long timer = System.currentTimeMillis() + 300000;
        jwtBuilder.setExpiration(new Date(timer));

        //自定义claims
        Map<String, Object> user = new HashMap<>();
        user.put("name", "steven");
        user.put("age", "18");
        user.put("address", "深圳市.黑马程序员");
        jwtBuilder.addClaims(user);

        //7、生成令牌-compact()
        String token = jwtBuilder.compact();
        //8、输出结果-令牌
        System.out.println(token);
    }

    //解密-验证令牌
    @Test
    public void testParseJwt(){
        String token = "eyJhbGciOiJIUzI1NiJ9.eyJqdGkiOiIwMDciLCJzdWIiOiJ7aWQ6MDA3LG5hbWU9J-Wwj-WnkOWnkCd9IiwiaWF0IjoxNTc3NTkxMTY0LCJpc3MiOiJzei5pdGhlaW1hIiwiZXhwIjoxNTc3NTkxNDY1LCJhZGRyZXNzIjoi5rex5Zyz5biCLum7kemprOeoi-W6j-WRmCIsIm5hbWUiOiJzdGV2ZW4iLCJhZ2UiOiIxOCJ9.DUkAm4O8My-lZccyDYo5w8tKEIvqshvbu2TffVpkMsI";
        //解析器
        JwtParser jwtParser = Jwts.parser();
        //设置密钥
        jwtParser.setSigningKey("sz.itheima.key");
        //解密
        Claims claims = jwtParser.parseClaimsJws(token).getBody();
        System.out.println(claims);
    }
}
