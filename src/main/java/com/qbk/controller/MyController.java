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
 * Created by 13624 on 2018/8/9.
 */
@QBKController
@QBKRequestMapping("/qbk")
public class MyController {

    @QBKAutowired("MyServiceImpl")
    private MyService myService ;

    @QBKRequestMapping("/get")
    public  void  get(HttpServletResponse response , HttpServletRequest request ,
                      @QBKRequestParam("name")String name)  {

        PrintWriter writer = null;
        try {
            writer = response.getWriter();
            String result = myService.fun(name);
            writer.println(result);
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
