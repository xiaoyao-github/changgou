package com.changgou.token;

import org.junit.Test;
import org.springframework.security.jwt.Jwt;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.security.jwt.crypto.sign.RsaVerifier;

/*****
 * @Author: Steven
 * @Date: 2019/7/7 13:48
 * @Description: com.changgou.token
 *  使用公钥解密令牌数据
 ****/
public class ParseJwtTest {

    /***
     * 校验令牌
     */
    @Test
    public void testParseToken(){
        //令牌
        String token = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJyb2xlcyI6IlJPTEVfVklQLFJPTEVfVVNFUiIsIm5hbWUiOiJpdGhlaW1hIiwiaWQiOiIxIn0.0_CiVSq78XWLODTn4wgXBwgaECI3l6yPI8cVeIwDNd5opn3A6kZylxi0D6jsjQuM1GjAjN231y7caD_8Nh7DsgWRBwZV2q3pJIzzPXRlRIfBQYVw_Nq1CEZ_7ZttePyF46bcO_wU22iBfSpKHygHQNsPwcWYcpD3cYezPRQyr4I2CJRFtkipxqe7ext5q3GuCNHBzB7X9YkpSkILE1nfixMKna9GFMdjwvazU4dRDhuGRRnrKgm9_NRpRBc4E7Pf1bzwalUtPCcVof5AUtI-BXWCtwKAfAoZ_r1U4NdcdjuOkskKwyPE9FgCNDey9x0TWIJcqW3NC1xO1EeWKFoUCA";

        //公钥
        //String publickey = "-----BEGIN PUBLIC KEY-----MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAvFsEiaLvij9C1Mz+oyAmt47whAaRkRu/8kePM+X8760UGU0RMwGti6Z9y3LQ0RvK6I0brXmbGB/RsN38PVnhcP8ZfxGUH26kX0RK+tlrxcrG+HkPYOH4XPAL8Q1lu1n9x3tLcIPxq8ZZtuIyKYEmoLKyMsvTviG5flTpDprT25unWgE4md1kthRWXOnfWHATVY7Y/r4obiOL1mS5bEa/iNKotQNnvIAKtjBM4RlIDWMa6dmz+lHtLtqDD2LF1qwoiSIHI75LQZ/CNYaHCfZSxtOydpNKq8eb1/PGiLNolD4La2zf0/1dlcr5mkesV570NxRmU1tFm8Zd3MZlZmyv9QIDAQAB-----END PUBLIC KEY-----";
        String publickey = "-----BEGIN PUBLIC KEY-----MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA+xTLE3NWYUrvDlTXErJ5xxmrQ7Osjx2L/4MOKKLSo8d7C+qvcfvoicX5cyroG/uL2rgPMMVyn4IYcm3lzyVSru48bQjsVhODtZmZiZEtGzos3Kq4uNm9Gem+4zULE+TWUGkIzsHgByeSLsrm5OWFwnm9dHoHdHxlRUjEKZ7afjk8VpSWjExE7e/7ODdF+rdqDuHdpDQoN+SXDnRh63dmCsCgkAH68DjCEbu8VwQBKjDIjV4JFPqhaiDj69U17+X6cZqXR4YooBbcmaVI/0XkUxZNw0cTsO+3IsRo4ThN1Uq6vQ+0A6I3BxFqkG4SBgMjghVxUy4aK8vSuXQm5veWKwIDAQAB-----END PUBLIC KEY-----";

        //校验Jwt
        Jwt jwt = JwtHelper.decodeAndVerify(token, new RsaVerifier(publickey));

        //获取Jwt原始内容
        String claims = jwt.getClaims();
        System.out.println(claims);
        //jwt令牌
        String encoded = jwt.getEncoded();
        System.out.println(encoded);
    }
}
