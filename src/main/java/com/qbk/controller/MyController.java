package com.qbk.controller;

import com.qbk.annotation.QBKAutowired;
import com.qbk.annotation.QBKController;
import com.qbk.annotation.QBKRequestMapping;
import com.qbk.annotation.QBKRequestParam;
import com.qbk.service.MyService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * v1 v2 测试
 */
@QBKController
@QBKRequestMapping("/qbk")
public class MyController {

    //@QBKAutowired("myService")
    @QBKAutowired
    private MyService myService ;

    @QBKRequestMapping("/get")
    public void get(
            HttpServletResponse response ,
            HttpServletRequest request ,
            @QBKRequestParam("name")String name ,
            @QBKRequestParam("password")String password
    )  {
        PrintWriter writer = null;
        try {
            writer = response.getWriter();
            String result = myService.fun(name);
            writer.println(result + ":" + password);
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if (writer != null){
                writer.close();
            }
        }
    }
}
