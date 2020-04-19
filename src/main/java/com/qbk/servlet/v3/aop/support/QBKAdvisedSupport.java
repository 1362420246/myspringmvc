package com.qbk.servlet.v3.aop.support;

import com.qbk.servlet.v3.aop.aspect.QBKAdvice;
import com.qbk.servlet.v3.aop.config.AopConfig;
import lombok.Data;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 解析AOP配置的工具类
 */
@Data
public class QBKAdvisedSupport {
    /**
     *  aop配置文件
     */
    private AopConfig config;
    /**
     * 目标对象
     */
    private Object target;
    /**
     * 目标类
     */
    private Class targetClass;

    /**
     * 切点表达式 正则匹配器
     */
    private Pattern pointCutClassPattern;

    /**
     * 目标代理类的方法 和 通知 的关联
     */
    private Map<Method,Map<String,QBKAdvice>> methodCache = new HashMap<>();

    public QBKAdvisedSupport(AopConfig config) {
        this.config = config;
    }

    /**
     * 设置目标类
     */
    public void setTargetClass(Class<?> targetClass) {
        this.targetClass = targetClass;
        //解析配置
        parse();
    }

    /**
     * 解析配置文件的方法
     */
    private void parse() {
        //把Spring的Excpress变成Java能够识别的正则表达式
        String pointCut = config.getPointCut()
                .replaceAll("\\.", "\\\\.")
                .replaceAll("\\\\.\\*", ".*")
                .replaceAll("\\(", "\\\\(")
                .replaceAll("\\)", "\\\\)");

        //保存专门匹配Class的正则
        String pointCutForClassRegex = pointCut.substring(0, pointCut.lastIndexOf("\\(") - 4);
        //切点的  正则
        pointCutClassPattern = Pattern.compile("class " + pointCutForClassRegex.substring(pointCutForClassRegex.lastIndexOf(" ") + 1));

        //保存专门匹配方法的正则
        Pattern pointCutPattern = Pattern.compile(pointCut);
        try{
            //反射切面类
            Class aspectClass = Class.forName(this.config.getAspectClass());
            //保存 切面中的 通知（方法）
            Map<String,Method> aspectMethods = new HashMap<>();
            for (Method method : aspectClass.getMethods()) {
                aspectMethods.put(method.getName(),method);
            }

            //目标代理类的方法
            for (Method method : this.targetClass.getMethods()) {
                String methodString = method.toString();
                if(methodString.contains("throws")){
                    //去掉异常
                    methodString = methodString.substring(0,methodString.lastIndexOf("throws")).trim();
                }
                //切点中的 方法  正则匹配
                Matcher matcher = pointCutPattern.matcher(methodString);
                //方法 是否 符合切点 规则
                if(matcher.matches()){
                    //用于保存通知 集合
                    Map<String,QBKAdvice> advices = new HashMap<>();

                    //判断 各种通知
                    if(!(null == config.getAspectBefore() || "".equals(config.getAspectBefore()))){
                        //切面对象
                        Object aspect = aspectClass.newInstance();
                        //配置中 的 前置通知
                        String aspectBefore = config.getAspectBefore();
                        //对应 切面类中 的 通知方法
                        Method beforeMethod = aspectMethods.get(aspectBefore);
                        //封装 增强
                        QBKAdvice qbkAdvice = new QBKAdvice(aspect, beforeMethod);
                        //添加到 通知 集合中
                        advices.put("before",qbkAdvice);
                    }
                    if(!(null == config.getAspectAfter() || "".equals(config.getAspectAfter()))){
                        advices.put("after",new QBKAdvice(aspectClass.newInstance(),aspectMethods.get(config.getAspectAfter())));
                    }
                    if(!(null == config.getAspectAfterThrow() || "".equals(config.getAspectAfterThrow()))){
                        QBKAdvice advice = new QBKAdvice(aspectClass.newInstance(),aspectMethods.get(config.getAspectAfterThrow()));
                        advice.setThrowName(config.getAspectAfterThrowingName());
                        advices.put("afterThrow",advice);
                    }
                    //跟目标代理类的业务方法和Advices建立一对多个关联关系，以便在Porxy类中获得
                    methodCache.put(method,advices);
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 是否生成代理对象
     * 根据 切入点的表达式 正则匹配 目标类的 class
     */
    public boolean pointCutMath() {
        return pointCutClassPattern.matcher(this.targetClass.toString()).matches();
    }

    /**
     * 根据一个目标代理类的方法，获得其对应的通知
     */
    public Map<String, QBKAdvice> getAdvices(Method method) throws NoSuchMethodException {
        //享元设计模式的应用
        Map<String,QBKAdvice> cache = methodCache.get(method);
        if(null == cache){
            Method m = targetClass.getMethod(method.getName(),method.getParameterTypes());
            cache = methodCache.get(m);
            this.methodCache.put(m,cache);
        }
        return cache;
    }

}
