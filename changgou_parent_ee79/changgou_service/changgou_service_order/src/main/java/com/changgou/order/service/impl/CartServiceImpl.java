package com.changgou.order.service.impl;

import com.changgou.goods.feign.SkuFeign;
import com.changgou.goods.feign.SpuFeign;
import com.changgou.goods.pojo.Sku;
import com.changgou.goods.pojo.Spu;
import com.changgou.order.pojo.OrderItem;
import com.changgou.order.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Steven
 * @version 1.0
 * @description com.changgou.order.service.impl
 * @date 2020-1-2
 */
@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private SkuFeign skuFeign;
    @Autowired
    private SpuFeign spuFeign;
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public void add(Integer num, Long skuId, String username) {

        //如果传入的数量小于1，说明是不买当前商品
        if(num < 1){
            //删除当前购物车商品
            redisTemplate.boundHashOps("Carts_" + username).delete(skuId);
            return;
        }

        //1、查询商品信息
        //查询sku
        Sku sku = skuFeign.findById(skuId).getData();
        if(sku != null) {
            //查询spu
            Spu spu = spuFeign.findById(sku.getSpuId()).getData();
            //2、包装购物车信息
            OrderItem orderItem = new OrderItem();
            //设置商品三级分类
            orderItem.setCategoryId1(spu.getCategory1Id());
            orderItem.setCategoryId2(spu.getCategory2Id());
            orderItem.setCategoryId3(spu.getCategory3Id());
            orderItem.setSpuId(spu.getId());
            orderItem.setSkuId(skuId);
            orderItem.setName(sku.getName());  //商品名称
            orderItem.setPrice(sku.getPrice());  //单价
            orderItem.setNum(num);  //购买数量
            orderItem.setMoney(sku.getPrice() * num);  //小计
            orderItem.setPayMoney(orderItem.getMoney());  //实付金额
            orderItem.setImage(sku.getImage());
            orderItem.setWeight(sku.getWeight());  //重量
            orderItem.setIsReturn("0");  //未退货
            //3、保存购物车到Redis中
            redisTemplate.boundHashOps("Carts_" + username).put(skuId, orderItem);
        }
    }

    @Override
    public List<OrderItem> list(String username) {
        List<OrderItem> orderItemList = redisTemplate.boundHashOps("Carts_" + username).values();
        return orderItemList;
    }
}
