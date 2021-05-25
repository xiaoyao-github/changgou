package com.changgou.search.web.controller;

import com.changgou.search.feign.SearchFeign;
import entity.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * @author Steven
 * @version 1.0
 * @description com.changgou.search.web.controller
 * @date 2019-12-28
 */
@Controller
@RequestMapping("search")
public class SearchController {

    @Autowired
    private SearchFeign searchFeign;


    //注意此处必须加上一个请求url用于区分SearchFeign的请求地址
    @GetMapping("list")
    //@RequestBody 当我们入参是一个JSON串的时候使用
    //@RequestParam 我们传统入参http://www.a.com?keyword=华为&...
    public String search(@RequestParam(required = false) Map<String, String> searchMap, Model model){
        //查询商品信息
        Map result = searchFeign.search(searchMap);
        //返回查询结果
        model.addAttribute("result", result);
        //返回查询条件
        model.addAttribute("searchMap", searchMap);
        //计算url的参数
        String url = getUrl(searchMap);
        //返回url
        model.addAttribute("url", url);
        //返回分页对象
        Page page = new Page(
                new Long(result.get("total").toString()),
                new Integer(result.get("pageNum").toString()),
                new Integer(result.get("pageSize").toString()));
        model.addAttribute("page", page);
        //响应视图名字
        return "search";
    }

    /**
     * 计算url的参数
     * @param searchMap
     * @return
     */
    private String getUrl(@RequestParam(required = false) Map<String, String> searchMap) {
        //计算返回url
        String url = "/search/list?";
        //目标：/search/list?keywords=华为&category=笔记本&brand=华为&spec_手机屏幕尺寸=5寸
        for (String key : searchMap.keySet()) {
            //排除掉key以sort开头的参数，不追加到url后面
            if(key.startsWith("sort") || "pageNum".equalsIgnoreCase(key)){
                continue;
            }
            url += key + "=" + searchMap.get(key) + "&";
        }
        url = url.substring(0, url.length() - 1);
        return url;
    }
}
