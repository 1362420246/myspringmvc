package com.qbk.controller;


import com.qbk.annotation.QBKAutowired;
import com.qbk.annotation.QBKController;
import com.qbk.annotation.QBKRequestMapping;
import com.qbk.annotation.QBKRequestParam;
import com.qbk.service.MyService;
import com.qbk.servlet.v3.webmvc.servlet.QBKModelAndView;

import java.util.HashMap;
import java.util.Map;

/**
 * v3 测试
 *
 */
@QBKController
@QBKRequestMapping("/")
public class PageController {

    @QBKAutowired
    private MyService myService;

    @QBKRequestMapping("/first.html")
    public QBKModelAndView query(@QBKRequestParam("name") String name){
        String result = myService.query(name);
        Map<String,Object> model = new HashMap<String,Object>();
        model.put("name", name);
        model.put("data", result);
        model.put("token", "123456");
        return new QBKModelAndView("first.html",model);
    }

}
