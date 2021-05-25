package com.changgou.pay.controller;

import com.alibaba.fastjson.JSON;
import com.changgou.pay.service.WeixinPayService;
import com.github.wxpay.sdk.WXPayUtil;
import entity.Result;
import entity.StatusCode;
import entity.TokenDecode;
import org.apache.commons.io.IOUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Steven
 * @version 1.0
 * @description com.changgou.pay.controller
 * @date 2020-1-5
 */
@RestController
@RequestMapping("/weixin/pay")
public class WeixinPayController {
    @Autowired
    private WeixinPayService weixinPayService;

    /**
     * 生成微信支付二维码
     * @param out_trade_no 订单号
     * @param total_fee 金额(分)
     * @return
     */
    /**
     * 生成微信支付二维码
     * @param param {
     *                 out_trade_no 订单号,
     *                 total_fee 金额(分),
     *                 exchange 交换机,
     *                 routingKey 路由Key
     *                 }
     * @return
     */
    @RequestMapping("/create/native")
    public Result<Map> createNative(@RequestParam Map param) {
        //String username = TokenDecode.getUserInfo().get("username");
        String username = "zhangsan";
        param.put("username", username);
        Map map = weixinPayService.createNative(param);
        return new Result<Map>(true, StatusCode.OK, "生成二维码成功", map);
    }

    /**
     * 查询支付状态
     * @param out_trade_no 商户订单号
     */
    @RequestMapping("/status/query")
    public Result<Map> queryPayStatus(String out_trade_no){
        Map map = weixinPayService.queryPayStatus(out_trade_no);
        return new Result<Map>(true, StatusCode.OK, "查询订单状态成功", map);
    }

    @Value("${mq.pay.exchange.order}")
    private String exchange;
    @Value("${mq.pay.queue.order}")
    private String queue;
    @Value("${mq.pay.routing.key}")
    private String routing;
    @Autowired
    private RabbitTemplate rabbitTemplate;


    /**
     * 支付完成后，微信会把相关支付结果及用户信息通过数据流的形式发送给商户，
     * 商户需要接收处理，并按文档规范返回应答。
     * @param request 请求数据
     * @return
     */
    @RequestMapping("/notify/url")
    public String notifyUrl(HttpServletRequest request){
        try {
            //读取数据流
            InputStream inputStream = request.getInputStream();
            //把输入流转换成字符串
            String resultXml = IOUtils.toString(inputStream, "utf-8");
            System.out.println("微信支付回调，传入参数为：" + resultXml);
            //把xml转换成Map
            Map<String, String> resultMap = WXPayUtil.xmlToMap(resultXml);

            //获取附加参数
            String attachJson = resultMap.get("attach");
            Map<String,String> attachMap = JSON.parseObject(attachJson, Map.class);
            exchange = attachMap.get("exchange");  //交换机
            routing = attachMap.get("routingKey");  //路由key
            //把消息发送到MQ中
            rabbitTemplate.convertAndSend(exchange,routing, JSON.toJSONString(resultMap));

            //按照微信文档规范返回应答
            Map respMap = new HashMap();
            respMap.put("return_code","SUCCESS");
            respMap.put("return_msg","OK");
            return WXPayUtil.mapToXml(respMap);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //返回null代表处理失败，等待微信下一次通知
        return null;
    }

    @RequestMapping("/test/queue")
    public String testQueue(){
        //普通订单测试-把消息发送到MQ中
        //rabbitTemplate.convertAndSend(exchange,routing, "{'return_code':'fail'}");

        //秒杀订单测试-把消息发送到MQ中
        rabbitTemplate.convertAndSend(exchange,"queue.seckillorder", "{'return_code':'fail'}");
        return "ok";
    }
}
