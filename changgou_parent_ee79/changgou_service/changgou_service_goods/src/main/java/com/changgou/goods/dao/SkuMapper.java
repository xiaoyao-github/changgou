package com.changgou.goods.dao;
import com.changgou.goods.pojo.Sku;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import tk.mybatis.mapper.common.Mapper;

/****
 * @Author:sz.itheima
 * @Description:Sku的Dao
 * @Date 2019/6/14 0:12
 *****/
public interface SkuMapper extends Mapper<Sku> {
    /**
     * 扣减库存
     * @param id  商品id
     * @param num 购买数量
     * @return 数据库更新的行数
     */
    @Update("UPDATE tb_sku SET num=num-#{num},sale_num=sale_num+#{num} WHERE id = #{id} AND num > #{num}")
    int decrCount(@Param("id") Long id, @Param("num")Integer num);
}
