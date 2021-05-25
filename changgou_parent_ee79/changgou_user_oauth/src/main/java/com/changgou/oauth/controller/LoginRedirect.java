package com.changgou.oauth.controller;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author Steven
 * @version 1.0
 * @description com.changgou.oauth.controller
 * @date 2020-1-3
 */
@Controller
@RequestMapping("oauth")
public class LoginRedirect {

    @RequestMapping("login")
    public String toLogin(String FROM, Model model){
        if (StringUtils.isNotBlank(FROM)) {
            model.addAttribute("from", FROM);
        }
        //返回视图的名字，默认后缀为.html
        return "login";
    }
}
