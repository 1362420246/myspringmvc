package com.qbk.service.impl;

import com.qbk.annotation.QBKService;
import com.qbk.service.MyService;

@QBKService("myService")
public class MyServiceImpl implements MyService{

    @Override
    public String fun(String name) {
        System.out.println("qbk:" + name);
        return "qbk:" + name;
    }
}
