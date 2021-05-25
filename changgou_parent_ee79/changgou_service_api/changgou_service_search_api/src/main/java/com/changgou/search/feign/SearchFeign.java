package com.changgou.search.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * @author Steven
 * @version 1.0
 * @description com.changgou.search.feign
 * @date 2019-12-28
 */
@FeignClient(name = "search")
@RequestMapping("search")
public interface SearchFeign {
    @GetMapping
    //@RequestBody 当我们入参是一个JSON串的时候使用
    //@RequestParam 我们传统入参http://www.a.com?keyword=华为&...
    public Map search(@RequestParam(required = false) Map<String, String> searchMap);
}
