package com.changgou.seckill.service.impl;

import com.changgou.seckill.dao.SeckillGoodsMapper;
import com.changgou.seckill.dao.SeckillOrderMapper;
import com.changgou.seckill.pojo.SeckillGoods;
import com.changgou.seckill.pojo.SeckillOrder;
import com.changgou.seckill.service.SeckillOrderService;
import com.changgou.seckill.task.MultiThreadingCreateOrder;
import com.changgou.seckill.utils.SeckillStatus;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import entity.IdWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tk.mybatis.mapper.entity.Example;

import java.util.Date;
import java.util.List;

/****
 * @Author:sz.itheima
 * @Description:SeckillOrder业务层接口实现类
 * @Date 2019/6/14 0:16
 *****/
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@Service
public class SeckillOrderServiceImpl implements SeckillOrderService {

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private IdWorker idWorker;
    @Autowired
    private SeckillOrderMapper seckillOrderMapper;
    @Autowired
    private MultiThreadingCreateOrder multiThreadingCreateOrder;


    /**
     * SeckillOrder条件+分页查询
     * @param seckillOrder 查询条件
     * @param page 页码
     * @param size 页大小
     * @return 分页结果
     */
    @Override
    public PageInfo<SeckillOrder> findPage(SeckillOrder seckillOrder, int page, int size){
        //分页
        PageHelper.startPage(page,size);
        //搜索条件构建
        Example example = createExample(seckillOrder);
        //执行搜索
        return new PageInfo<SeckillOrder>(seckillOrderMapper.selectByExample(example));
    }

    /**
     * SeckillOrder分页查询
     * @param page
     * @param size
     * @return
     */
    @Override
    public PageInfo<SeckillOrder> findPage(int page, int size){
        //静态分页
        PageHelper.startPage(page,size);
        //分页查询
        return new PageInfo<SeckillOrder>(seckillOrderMapper.selectAll());
    }

    /**
     * SeckillOrder条件查询
     * @param seckillOrder
     * @return
     */
    @Override
    public List<SeckillOrder> findList(SeckillOrder seckillOrder){
        //构建查询条件
        Example example = createExample(seckillOrder);
        //根据构建的条件查询数据
        return seckillOrderMapper.selectByExample(example);
    }


    /**
     * SeckillOrder构建查询对象
     * @param seckillOrder
     * @return
     */
    public Example createExample(SeckillOrder seckillOrder){
        Example example=new Example(SeckillOrder.class);
        Example.Criteria criteria = example.createCriteria();
        if(seckillOrder!=null){
            // 主键
            if(!StringUtils.isEmpty(seckillOrder.getId())){
                    criteria.andEqualTo("id",seckillOrder.getId());
            }
            // 秒杀商品ID
            if(!StringUtils.isEmpty(seckillOrder.getSeckillId())){
                    criteria.andEqualTo("seckillId",seckillOrder.getSeckillId());
            }
            // 支付金额
            if(!StringUtils.isEmpty(seckillOrder.getMoney())){
                    criteria.andEqualTo("money",seckillOrder.getMoney());
            }
            // 用户
            if(!StringUtils.isEmpty(seckillOrder.getUserId())){
                    criteria.andEqualTo("userId",seckillOrder.getUserId());
            }
            // 创建时间
            if(!StringUtils.isEmpty(seckillOrder.getCreateTime())){
                    criteria.andEqualTo("createTime",seckillOrder.getCreateTime());
            }
            // 支付时间
            if(!StringUtils.isEmpty(seckillOrder.getPayTime())){
                    criteria.andEqualTo("payTime",seckillOrder.getPayTime());
            }
            // 状态，0未支付，1已支付
            if(!StringUtils.isEmpty(seckillOrder.getStatus())){
                    criteria.andEqualTo("status",seckillOrder.getStatus());
            }
            // 收货人地址
            if(!StringUtils.isEmpty(seckillOrder.getReceiverAddress())){
                    criteria.andEqualTo("receiverAddress",seckillOrder.getReceiverAddress());
            }
            // 收货人电话
            if(!StringUtils.isEmpty(seckillOrder.getReceiverMobile())){
                    criteria.andEqualTo("receiverMobile",seckillOrder.getReceiverMobile());
            }
            // 收货人
            if(!StringUtils.isEmpty(seckillOrder.getReceiver())){
                    criteria.andEqualTo("receiver",seckillOrder.getReceiver());
            }
            // 交易流水
            if(!StringUtils.isEmpty(seckillOrder.getTransactionId())){
                    criteria.andEqualTo("transactionId",seckillOrder.getTransactionId());
            }
        }
        return example;
    }

    /**
     * 删除
     * @param id
     */
    @Override
    public void delete(Long id){
        seckillOrderMapper.deleteByPrimaryKey(id);
    }

    /**
     * 修改SeckillOrder
     * @param seckillOrder
     */
    @Override
    public void update(SeckillOrder seckillOrder){
        seckillOrderMapper.updateByPrimaryKey(seckillOrder);
    }

    /**
     * 增加SeckillOrder
     * @param seckillOrder
     */
    @Override
    public void add(SeckillOrder seckillOrder){
        seckillOrderMapper.insert(seckillOrder);
    }

    /**
     * 根据ID查询SeckillOrder
     * @param id
     * @return
     */
    @Override
    public SeckillOrder findById(Long id){
        return  seckillOrderMapper.selectByPrimaryKey(id);
    }

    /**
     * 查询SeckillOrder全部数据
     * @return
     */
    @Override
    public List<SeckillOrder> findAll() {
        return seckillOrderMapper.selectAll();
    }

    @Override
    public boolean add(Long id, String time, String username) {
        //先处理重复下单问题
        //increment(key,自增或者自减数量)
        /*Long count = redisTemplate.boundHashOps("UserQueueCount").increment(username, 1);
        //如果用户已经下单过了
        if (count > 1) {
            throw new RuntimeException("您有一张未支付的订单，请先支付！");
        }*/

        //优先处理库存问题
        //方式一：
        /*Object obj = redisTemplate.boundListOps("SeckillGoodsCountList_" + id).rightPop();
        //如果队列取到空值，表示库存没有了
        if (obj == null) {
            throw new RuntimeException("你来晚了一步，当前商品已被抢购一空！");
        }*/
        //方式一：
        Long count = redisTemplate.boundHashOps("SeckillGoodsCount").increment(id, -1);
        //如果库存小于0，说明没货了
        if (count < 0) {
            throw new RuntimeException("你来晚了一步，当前商品已被抢购一空！");
        }
        //排队-左进右出
        //创建排队记录对象
        SeckillStatus seckillStatus = new SeckillStatus(username, new Date(), 1, id, time);
        //左进-Redis的队列本身就分布式的
        //我们把排队信息记录在队列中，等回在多线程中取出来，参数就可以传入多线程方法
        redisTemplate.boundListOps("SeckillOrderQueue").leftPush(seckillStatus);
        //登记排队信息
        redisTemplate.boundHashOps("UserQueueStatus").put(username, seckillStatus);

        //System.out.println("进入了抢单Service的add方法....begin....");
        //调起多线程下单方法
        multiThreadingCreateOrder.createOrder();
        //System.out.println("进入了抢单Service的add方法....end....");
        return true;
    }

    @Override
    public SeckillStatus queryStatus(String username) {
        SeckillStatus seckillStatus = (SeckillStatus) redisTemplate.boundHashOps("UserQueueStatus").get(username);
        return seckillStatus;
    }

    @Override
    public void updatePayStatus(String out_trade_no, String transaction_id, String username) {
        //1、把当前用户的订单查询出来
        SeckillOrder seckillOrder = (SeckillOrder) redisTemplate.boundHashOps("SeckillOrder").get(username);
        if (seckillOrder != null) {
            //2、更新订单状态与记录流水号
            seckillOrder.setStatus("1");//已支付
            seckillOrder.setTransactionId(transaction_id);  //流水
            seckillOrder.setPayTime(new Date());  //支付时间
            //3、保存订单到Mysql
            seckillOrderMapper.insertSelective(seckillOrder);
            //4、清除Redis相关信息
            //清除下单记录
            redisTemplate.boundHashOps("UserQueueCount").delete(username);
            //清除排队信息
            redisTemplate.boundHashOps("UserQueueStatus").delete(username);
            //清除Redis订单
            redisTemplate.boundHashOps("SeckillOrder").delete(username);
        }

    }

    @Override
    public void closeOrder(String username) {
        //1.读取排队状态-redis.UserQueueStatus
        SeckillStatus seckillStatus = (SeckillStatus) redisTemplate.boundHashOps("UserQueueStatus").get(username);
        //2.获取Redis中订单信息-SeckillOrder
        SeckillOrder seckillOrder = (SeckillOrder) redisTemplate.boundHashOps("SeckillOrder").get(username);
        //3.如果Redis中有订单信息，说明用户未支付
        if (seckillOrder != null) {
            //3.1删除订单-SeckillOrder.delete
            redisTemplate.boundHashOps("SeckillOrder").delete(username);
            //3.2回滚库存
            //3.2.1从Redis中获取该商品-SeckillGoods_time.get(id)
            SeckillGoods seckillGoods = (SeckillGoods) redisTemplate.boundHashOps("SeckillGoods_" + seckillStatus.getTime()).get(seckillStatus.getGoodsId());
            //3.2.2如果Redis中没有，则从数据库中加载--省略
            //3.2.3数量+1  (递增数量+1，队列数量+1)
            //推荐使用此方式
            Long count = redisTemplate.boundHashOps("SeckillGoodsCount").increment(seckillGoods.getId(), 1);
            //两者只需使用其一
            //redisTemplate.boundListOps("SeckillGoodsCountList_" + seckillGoods.getId()).leftPush(1);

            //3.3.数据同步到Redis中-SeckillGoods_time,UserQueueCount,UserQueueStatus
            seckillGoods.setStockCount(count.intValue());
            redisTemplate.boundHashOps("SeckillGoods_" + seckillStatus.getTime()).put(seckillGoods.getId(), seckillGoods);

            //清除下单记录
            redisTemplate.boundHashOps("UserQueueCount").delete(username);
            //清除排队信息
            redisTemplate.boundHashOps("UserQueueStatus").delete(username);
        }

    }
}
