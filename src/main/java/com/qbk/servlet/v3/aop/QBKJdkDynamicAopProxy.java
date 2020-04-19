package com.qbk.servlet.v3.aop;

import com.qbk.servlet.v3.aop.aspect.QBKAdvice;
import com.qbk.servlet.v3.aop.support.QBKAdvisedSupport;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

/**
 * 代理
 */
public class QBKJdkDynamicAopProxy implements InvocationHandler {

    private QBKAdvisedSupport support;

    public QBKJdkDynamicAopProxy(QBKAdvisedSupport support) {
        this.support = support;
    }

    /**
     *处理代理实例上的方法调用并返回结果。此方法将在调用处理程序上调用当一个方法在代理实例上被调用时联系在一起。
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        //根据方法 获取对应的 通知集合
        Map<String,QBKAdvice> advices = support.getAdvices(method);
        Object returnValue;
        try {
            //前置
            invokeAdivce(advices.get("before"));

            //执行原始方法
            returnValue = method.invoke(this.support.getTarget(),args);

            //后置
            invokeAdivce(advices.get("after"));
        }catch (Exception e){
            //异常
            invokeAdivce(advices.get("afterThrow"));
            throw e;
        }
        return returnValue;
    }

    /**
     * 执行通知
     */
    private void invokeAdivce(QBKAdvice advice) {
        try {
            //用 通知对象  执行通知方法
            advice.getAdviceMethod().invoke(advice.getAspect());
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取代理
     */
    public Object getProxy() {
        return Proxy.newProxyInstance(
                this.getClass().getClassLoader(),
                this.support.getTargetClass().getInterfaces(),
                this
        );
    }
}
