package com.qbk.servlet.v3.webmvc.servlet;

import lombok.Data;

import java.lang.reflect.Method;
import java.util.regex.Pattern;

/**
 * HandlerMapping 处理器映射器
 * 保存 url 和 method 映射
 */
@Data
public class QBKHandlerMapping {
    /**
     * URL
     */
    private Pattern pattern;
    /**
     * 对应的Method
     */
    private Method method;
    /**
     * Method对应的实例对象
     */
    private Object controller;

    public QBKHandlerMapping(Pattern pattern, Object controller, Method method) {
        this.pattern = pattern;
        this.method = method;
        this.controller = controller;
    }

}
