package com.qbk.servlet.v3.aop.aspect;

import lombok.Data;

import java.lang.reflect.Method;

/**
 *  增强
 */
@Data
public class QBKAdvice {
    /**
     * 增强 对象 (切面对象)
     */
    private Object aspect;
    /**
     * 增强 方法 （切面中的方法/通知）
     */
    private Method adviceMethod;
    /**
     * 增强 异常
     */
    private String throwName;

    public QBKAdvice(Object aspect, Method adviceMethod) {
        this.aspect = aspect;
        this.adviceMethod = adviceMethod;
    }

}
