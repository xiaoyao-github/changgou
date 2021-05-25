package com.changgou.test;

import com.github.wxpay.sdk.WXPayUtil;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Steven
 * @version 1.0
 * @description com.changgou.test
 * @date 2020-1-5
 */
public class WXTest {

    @Test
    public void testWx() throws Exception{
        //获取随机字符串-请求入参
        System.out.println(WXPayUtil.generateNonceStr());
        //把Map转换成带签名的XML
        Map data = new HashMap();
        data.put("id", "001");
        data.put("name", "风清扬");
        String xml = WXPayUtil.generateSignedXml(data, "fs24jl2l2j2l2lk4");
        System.out.println(xml);
        //把xml转换成Map
        Map<String, String> reslut = WXPayUtil.xmlToMap(xml);
        System.out.println(reslut);
    }
}
