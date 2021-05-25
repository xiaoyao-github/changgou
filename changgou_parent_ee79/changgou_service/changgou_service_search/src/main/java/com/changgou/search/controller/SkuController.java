package com.changgou.search.controller;

import com.changgou.search.service.SkuService;
import entity.Result;
import entity.StatusCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * @author Steven
 * @version 1.0
 * @description com.changgou.search.controller
 * @date 2019-12-25
 */
@RestController
@CrossOrigin
//注意此处名称要区分我们的SkuFeign的请求地址
@RequestMapping("search")
public class SkuController {
    @Autowired
    private SkuService skuService;

    @GetMapping("import")
    public Result importSku(){
        skuService.importSku();
        return new Result(true, StatusCode.OK, "导入数据成功");
    }

    @GetMapping
    //@RequestBody 当我们入参是一个JSON串的时候使用
    //@RequestParam 我们传统入参http://www.a.com?keyword=华为&...
    public Map search(@RequestParam(required = false) Map<String, String> searchMap){
        Map resultMap = skuService.search(searchMap);
        return resultMap;
    }
}
