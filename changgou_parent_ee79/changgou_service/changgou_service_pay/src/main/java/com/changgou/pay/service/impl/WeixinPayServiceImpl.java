package com.changgou.pay.service.impl;

import com.alibaba.fastjson.JSON;
import com.changgou.pay.service.WeixinPayService;
import com.github.wxpay.sdk.WXPayUtil;
import entity.HttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Steven
 * @version 1.0
 * @description com.changgou.pay.service.impl
 * @date 2020-1-5
 */
@Service
public class WeixinPayServiceImpl implements WeixinPayService {

    @Value("${weixin.appid}")
    private String appid;  //公众号
    @Value("${weixin.partner}")
    private String partner;  //商户号
    @Value("${weixin.notifyurl}")
    private String notifyurl; //回调地址
    @Value("${weixin.partnerkey}")
    private String partnerkey;  //支付密钥

    /**
     * 生成微信支付二维码
     * @param param {
     *                 out_trade_no 订单号,
     *                 total_fee 金额(分),
     *                 exchange 交换机,
     *                 routingKey 路由Key
     *                 username 用户名
     *                 }
     * @return
     */
    @Override
    public Map createNative(Map<String,String> param) {
        Map map = null;
        try {
            map = new HashMap();
            //1、包装统一下单需要的参数
            Map paramMap = new HashMap();
            paramMap.put("appid", this.appid);  //公众号
            paramMap.put("mch_id", this.partner);  //商户号
            paramMap.put("nonce_str", WXPayUtil.generateNonceStr());  //随机字符串
            paramMap.put("body", "畅购");  //商品描述，用户扫码后看见的商品信息
            paramMap.put("out_trade_no", param.get("out_trade_no"));  //商户订单号
            paramMap.put("total_fee", param.get("total_fee"));  //支付金额(分)
            paramMap.put("spbill_create_ip", "127.0.0.1");  //终端ip
            paramMap.put("notify_url", this.notifyurl);  //回调地址
            paramMap.put("trade_type", "NATIVE");  //交易类型

            //附加参数
            Map<String, String> attachMap = new HashMap<String,String>();
            attachMap.put("exchange", param.get("exchange"));  //交换机
            attachMap.put("routingKey", param.get("routingKey"));  //交换机
            attachMap.put("username", param.get("username"));  //用户名
            String attach = JSON.toJSONString(attachMap);
            paramMap.put("attach", attach);  //附加参数：String(127)

            //签名我们有个方法可以直接生成.........
            //2、把参数转换成xml
            String paramXml = WXPayUtil.generateSignedXml(paramMap, this.partnerkey);
            System.out.println("正在发起微信统一下单接口，请求参数为：" + paramXml);
            //3、通过HttpClient发起请求，得到结果
            HttpClient client = new HttpClient("https://api.mch.weixin.qq.com/pay/unifiedorder");
            client.setHttps(true);
            client.setXmlParam(paramXml);  //设置请求参数
            client.post();//发起请求
            String resultXml = client.getContent();
            System.out.println("发起微信统一下单接口成功，响应参数为：" + resultXml);
            //4、解析请求结果，包装成map返回
            Map<String, String> resultMap = WXPayUtil.xmlToMap(resultXml);
            //订单号
            map.put("out_trade_no", param.get("out_trade_no"));
            //支付金额
            map.put("total_fee", param.get("total_fee"));
            //二维码内容
            map.put("code_url", resultMap.get("code_url"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    @Override
    public Map queryPayStatus(String out_trade_no) {
        try {
            //1、包装统一下单需要的参数
            Map paramMap = new HashMap();
            paramMap.put("appid", this.appid);  //公众号
            paramMap.put("mch_id", this.partner);  //商户号
            paramMap.put("out_trade_no", out_trade_no);  //商户订单号
            paramMap.put("nonce_str", WXPayUtil.generateNonceStr());  //随机字符串

            //签名我们有个方法可以直接生成.........
            //2、把参数转换成xml
            String paramXml = WXPayUtil.generateSignedXml(paramMap, this.partnerkey);
            System.out.println("正在发起微信查询订单接口，请求参数为：" + paramXml);
            //3、通过HttpClient发起请求，得到结果
            HttpClient client = new HttpClient("https://api.mch.weixin.qq.com/pay/orderquery");
            client.setHttps(true);
            client.setXmlParam(paramXml);  //设置请求参数
            client.post();//发起请求
            String resultXml = client.getContent();
            System.out.println("发起微信查询订单接口成功，响应参数为：" + resultXml);
            //4、解析请求结果，包装成map返回
            Map<String, String> resultMap = WXPayUtil.xmlToMap(resultXml);
            return resultMap;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
