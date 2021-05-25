package com.changgou.seckill.task;

import com.changgou.seckill.dao.SeckillGoodsMapper;
import com.changgou.seckill.pojo.SeckillGoods;
import entity.DateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tk.mybatis.mapper.entity.Example;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * @author Steven
 * @version 1.0
 * @description com.changgou.seckill.task
 * @date 2020-1-6
 */
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@Component
public class SeckillGoodsPushTask {

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;
    @Autowired
    private RedisTemplate redisTemplate;

    //秒 分 小时 月份中的日期 月份 星期中的日期 年份
    @Scheduled(cron = "1/30 * * * * *")
    public void loadGoodsPushRedis(){
        System.out.println("定时器被调用，当前时间为：" + new Date());

        //获取当前时间表段的5个范围
        List<Date> dateMenus = DateUtil.getDateMenus();
        //遍历时间段，分别查出当前时间段的所有附合条件商品列表，保存到redis中
        for (Date now : dateMenus) {
            //yyyyMMddHH
            String nowStr = DateUtil.data2str(now, DateUtil.PATTERN_YYYYMMDDHH);
            //构建查询条件
            Example exampl = new Example(SeckillGoods.class);
            Example.Criteria criteria = exampl.createCriteria();
            criteria.andEqualTo("status", "1");  //审核通过的商品
            criteria.andGreaterThan("stockCount", 0);  //有货
            //startTime <= now <= endTime
            //开始时间 <= now
            criteria.andLessThanOrEqualTo("startTime", now);
            //结束时间 >= now
            criteria.andGreaterThanOrEqualTo("endTime", now);

            //排除已经存在Redis中的商品列表
            Set ids = redisTemplate.boundHashOps("SeckillGoods_" + nowStr).keys();
            if(ids != null && ids.size() > 0) {
                criteria.andNotIn("id", ids);
            }
            //查询商品列表
            List<SeckillGoods> seckillGoodsList = seckillGoodsMapper.selectByExample(exampl);
            System.out.println(nowStr + "时间段，查询到商品的个数为：" + seckillGoodsList.size());
            //把商品信息，存入Redis中
            for (SeckillGoods seckillGoods : seckillGoodsList) {
                redisTemplate.boundHashOps("SeckillGoods_" + nowStr).put(seckillGoods.getId(), seckillGoods);
                //超卖解决方案一：分布式队列
                for (int i = 0; i < seckillGoods.getStockCount(); i++) {
                    redisTemplate.boundListOps("SeckillGoodsCountList_" + seckillGoods.getId()).leftPush(i);
                }
                //超卖解决方案二：自减
                redisTemplate.boundHashOps("SeckillGoodsCount").increment(seckillGoods.getId(), seckillGoods.getStockCount());
            }
        }
    }
}

