package com.changgou.goods.dao;
import com.changgou.goods.pojo.Brand;
import org.apache.ibatis.annotations.Select;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

/****
 * @Author:sz.itheima
 * @Description:Brand的Dao
 * @Date 2019/6/14 0:12
 *****/
public interface BrandMapper extends Mapper<Brand> {

    /**
     * 根据分类id查询品牌列表
     * @param cid  分类id
     * @return
     */
    @Select("SELECT b.* FROM tb_category_brand cb,tb_brand b WHERE cb.`brand_id` = b.`id` AND cb.`category_id` = #{cid}")
    List<Brand> findByCategoryId(Integer cid);
}
