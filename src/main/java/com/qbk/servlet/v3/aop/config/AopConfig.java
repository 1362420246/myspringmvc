package com.qbk.servlet.v3.aop.config;

import lombok.Data;

/**
 * aop配置文件
 */
@Data
public class AopConfig {
    /**
     * 切点表达式
     */
    private String pointCut;
    /**
     * 切面
     */
    private String aspectClass;
    /**
     * 前置通知回调方法
     */
    private String aspectBefore;
    /**
     * 后置通知回调方法
     */
    private String aspectAfter;
    /**
     * 异常通知回调方法
     */
    private String aspectAfterThrow;
    /**
     * 异常类型捕获
     */
    private String aspectAfterThrowingName;
}
