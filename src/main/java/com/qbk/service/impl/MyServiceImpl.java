package com.qbk.service.impl;

import com.qbk.annotation.QBKService;
import com.qbk.service.MyService;

import java.text.SimpleDateFormat;
import java.util.Date;

@QBKService("myService")
public class MyServiceImpl implements MyService{

    @Override
    public String fun(String name) {
        System.out.println("qbk:" + name);
        return "qbk:" + name;
    }

    @Override
    public String query(String name) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String time = sdf.format(new Date());
        return "{name:\"" + name + "\",time:\"" + time + "\"}";
    }
}
