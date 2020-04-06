package com.qbk.servlet.v3.webmvc.servlet;

import com.qbk.annotation.*;
import com.qbk.servlet.v3.beans.QBKBeanWrapper;
import com.qbk.servlet.v3.context.QBKApplicationContext;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * servlet v3版本
 * 委派模式
 * 职责：负责任务调度，请求分发
 */
public class DispatcherServlet extends HttpServlet{

    private QBKApplicationContext applicationContext;

    /**
     * 保存Contrller中所有Mapping的对应关系
     */
    private Map<String, Method> handlerMapping = new HashMap<>();

    public DispatcherServlet() {
        super();
        System.out.println("servlet init");
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        //初始化Spring核心IoC容器
        applicationContext = new QBKApplicationContext(config.getInitParameter("contextConfigLocation"));

        //=============MVC部分================
        //5.初始化HandlerMapping
        doInitHandlerMapping();
    }


    /**
     * 5.初始化HandlerMapping
     */
   private void doInitHandlerMapping(){
       Map<String, QBKBeanWrapper> factoryBeanInstanceCache = applicationContext.getFactoryBeanInstanceCache();
       if(factoryBeanInstanceCache.isEmpty()){
           return;
       }
       for (Map.Entry<String, QBKBeanWrapper> entry : factoryBeanInstanceCache.entrySet()) {
           Object instance =  entry.getValue().getWrapperInstance();
           Class<?> clazz = instance.getClass();
           if (clazz.isAnnotationPresent(QBKController.class)) {
               QBKRequestMapping requestMapping = clazz.getAnnotation(QBKRequestMapping.class);
               //类上路径
               String baseUrl = requestMapping.value();
               //获取Method的url配置
               Method[] methods = clazz.getMethods();
               for (Method method : methods) {
                   //有没有加RequestMapping注解
                   if (method.isAnnotationPresent(QBKRequestMapping.class)) {
                       //映射URL
                       QBKRequestMapping methodMapping =  method.getAnnotation(QBKRequestMapping.class);
                       //方法上路径
                       String methodPath = methodMapping.value();
                       String url = ("/" + baseUrl + "/" + methodPath)
                               .replaceAll("/+", "/");
                       handlerMapping.put(url, method);
                   }
               }
           }
       }
   }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //6.委派，根据url ，找到对应的  method，然后执行
        try {
            doDispatch(req,resp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp)throws Exception {
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replaceAll(contextPath,"").replaceAll("/+","/");
        if(!this.handlerMapping.containsKey(url)){
            resp.getWriter().write("404 Not Found!!");
            return;
        }
        Method method = this.handlerMapping.get(url);

        //request 里面的参数
        Map<String,String[]> params = req.getParameterMap();
        //获取方法的形参列表
        Class<?> [] parameterTypes = method.getParameterTypes();
        //保存赋值参数的列表
        Object [] paramValues = new Object[parameterTypes.length];
        //按根据参数位置动态赋值
        for (int i = 0; i < parameterTypes.length; i ++){
            Class parameterType = parameterTypes[i];
            if(parameterType == HttpServletRequest.class){
                paramValues[i] = req;
            }else if(parameterType == HttpServletResponse.class){
                paramValues[i] = resp;
            }else if(parameterType == String.class){
                //提取方法中加了注解的参数
                Annotation[] [] pa = method.getParameterAnnotations();
                for (int j = 0; j < pa.length ; j ++) {
                    for(Annotation a : pa[i]){
                        if(a instanceof QBKRequestParam){
                            String paramName = ((QBKRequestParam) a).value();
                            if(!"".equals(paramName.trim())){
                                String value = Arrays.toString(params.get(paramName))
                                        .replaceAll("\\[|\\]","")
                                        .replaceAll("\\s",",");
                                paramValues[i] = value;
                            }
                        }
                    }
                }
            }
        }
        //暂时硬编码
        String beanName = toLowerFirstCase(method.getDeclaringClass().getSimpleName());
        //赋值实参列表
        Map<String, QBKBeanWrapper> factoryBeanInstanceCache = applicationContext.getFactoryBeanInstanceCache();
        method.invoke(factoryBeanInstanceCache.get(beanName).getWrapperInstance(),paramValues);
    }

    /**
     * 首字母变小写
     */
    private String toLowerFirstCase(String simpleName) {
        char [] chars = simpleName.toCharArray();
        chars[0] += 32;
        return  String.valueOf(chars);
    }

}
