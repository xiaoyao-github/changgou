package com.changgou.pay.service;

import java.util.Map;

/**
 * 微信支付接口
 * @author Steven
 * @description com.changgou.pay.service
 */
public interface WeixinPayService {
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
    public Map createNative(Map<String,String> param);

    /**
     * 查询支付状态
     * @param out_trade_no 商户订单号
     */
    public Map queryPayStatus(String out_trade_no);
}
