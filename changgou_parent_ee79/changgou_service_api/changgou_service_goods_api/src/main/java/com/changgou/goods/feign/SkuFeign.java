package com.changgou.goods.feign;

import com.changgou.goods.pojo.Sku;
import entity.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * @author Steven
 * @version 1.0
 * @description com.changgou.goods.feign
 * @date 2019-12-25
 */
//FeignClient(name:微服务的名称)
@FeignClient(name = "goods")
@RequestMapping("/sku")
public interface SkuFeign {
    //查询所有匹配状态的sku列表
    @GetMapping("/status/{status}")
    public Result<List<Sku>> findByStatus(@PathVariable String status);

    /***
     * 根据ID查询Sku数据
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public Result<Sku> findById(@PathVariable Long id);

    /**
     * 扣减库存
     *
     * @param username 当前登录用户
     * @return 数据库更新的行数
     */
    @RequestMapping("/decr/count/{username}")
    public Result decrCount(@PathVariable String username);
}
