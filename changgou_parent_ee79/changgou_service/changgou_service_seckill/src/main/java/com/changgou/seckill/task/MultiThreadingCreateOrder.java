package com.changgou.seckill.task;

import com.changgou.seckill.dao.SeckillGoodsMapper;
import com.changgou.seckill.dao.SeckillOrderMapper;
import com.changgou.seckill.pojo.SeckillGoods;
import com.changgou.seckill.pojo.SeckillOrder;
import com.changgou.seckill.utils.SeckillStatus;
import entity.IdWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * @author Steven
 * @version 1.0
 * @description com.changgou.seckill.task
 * @date 2020-1-6
 */
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@Component
public class MultiThreadingCreateOrder {

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private IdWorker idWorker;
    @Autowired
    private SeckillOrderMapper seckillOrderMapper;

    //@Async用于标识一个方法是多线程，调用此方法时，默认开始一个新线程执行
    @Async
    public void createOrder(){
        /*try {
            System.out.println("begin,进入了多线程方法....");
            //模拟器业务处理时间2秒
            Thread.sleep(10000);
            System.out.println("end,退出了多线程方法....");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/

        //马上从Redis中取出分布式队列-右出
        SeckillStatus seckillStatus = (SeckillStatus) redisTemplate.boundListOps("SeckillOrderQueue").rightPop();

        //便于测试我们这里的参数先写死
        //时间区间
        String time = seckillStatus.getTime();
        //用户登录名
        String username = seckillStatus.getUsername();
        //用户抢购商品
        Long id = seckillStatus.getGoodsId();

        //1、跟据商品id查询商品信息
        SeckillGoods seckillGoods = (SeckillGoods) redisTemplate.boundHashOps("SeckillGoods_" + time).get(id);
        if (seckillGoods == null) {
            throw new RuntimeException("当前要抢购的商品信息不存在或者已下架！");
        }
        //2、判断库存是否充足
        /*if(seckillGoods.getStockCount() < 1){
            throw new RuntimeException("你来晚了一步，当前商品已被抢购一空！");
        }*/
        //3、扣减库存
        //取库存设置进去，方式一：
        /*int size = 0;
        BoundListOperations listOps = redisTemplate.boundListOps("SeckillGoodsCountList_" + id);
        if (listOps != null) {
            size = listOps.size().intValue();
        }*/
        //取库存设置,方式二：
        int size = redisTemplate.boundHashOps("SeckillGoodsCount").increment(id, 0).intValue();

        seckillGoods.setStockCount(size);
        redisTemplate.boundHashOps("SeckillGoods_" + time).put(id, seckillGoods);
        //4、扣减后，商品库存不足时，要把数据同步回mysql
        if (seckillGoods.getStockCount() == 0) {
            seckillGoodsMapper.updateByPrimaryKeySelective(seckillGoods);
            //清除Redis商品信息(不建议删除)
        }
        //5、构建订单对象，保存订单到Redis
        SeckillOrder seckillOrder = new SeckillOrder();
        seckillOrder.setId(idWorker.nextId());
        seckillOrder.setSeckillId(id);
        seckillOrder.setMoney(seckillGoods.getCostPrice());  //支付金额-秒杀价
        seckillOrder.setUserId(username);  //下单用户
        seckillOrder.setCreateTime(new Date());  //下单时间
        seckillOrder.setStatus("0");  //未支付
        //保存订单到Redis
        redisTemplate.boundHashOps("SeckillOrder").put(username, seckillOrder);

        //修改排队状态
        seckillStatus.setStatus(2);  //已下单成功，等待支付
        seckillStatus.setMoney(new Float(seckillOrder.getMoney()));  //支付金额
        seckillStatus.setOrderId(seckillOrder.getId());  //订单号
        redisTemplate.boundHashOps("UserQueueStatus").put(username, seckillStatus);
    }
}
