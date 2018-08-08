package com.qbk.service.impl;

import com.qbk.annotation.QBKController;
import com.qbk.annotation.QBKService;
import com.qbk.service.MyService;

/**
 * Created by 13624 on 2018/8/9.
 */
@QBKService("MyServiceImpl")
public class MyServiceImpl implements MyService{

    @Override
    public String fun(String name) {
        System.out.println(name);
        return name;
    }
}
