package com.changgou.order.controller;

import com.changgou.order.pojo.OrderItem;
import com.changgou.order.service.CartService;
import entity.Result;
import entity.StatusCode;
import entity.TokenDecode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * @author Steven
 * @version 1.0
 * @description com.changgou.order.controller
 * @date 2020-1-2
 */
@RestController
@RequestMapping("cart")
public class CartController {

    @Autowired
    private CartService cartService;

    @RequestMapping("add")
    public Result add(Integer num, Long skuId) {
        //登录用户
        String username = "zhangsan";
        cartService.add(num,skuId,username);
        return new Result(true, StatusCode.OK,"购物车添加成功");
    }

    @RequestMapping("list")
    public Result<List<OrderItem>> list() {
        //登录用户
        //String username = "zhangsan";

        //获取令牌
        Map<String, String> map = TokenDecode.getUserInfo();
        System.out.println(map);
        String username = map.get("username");
        List<OrderItem> list = cartService.list(username);
        return new Result(true, StatusCode.OK, "购物车查询成功", list);
    }
}
