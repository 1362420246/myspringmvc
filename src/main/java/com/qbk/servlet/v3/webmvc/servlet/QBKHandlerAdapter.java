package com.qbk.servlet.v3.webmvc.servlet;

import com.qbk.annotation.QBKRequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * HandlerAdapter 处理器适配器
 * 动态参数适配器
 */
public class QBKHandlerAdapter {

    /**
     * 解析某一个方法的形参和返回值之后，统一封装为ModelAndView对象
      * @param req req
     * @param resp  resp
     * @param handler handlerMapping
     */
    QBKModelAndView handler(HttpServletRequest req, HttpServletResponse resp, QBKHandlerMapping handler) throws Exception{

        //保存形参列表
        //将 参数名称 和 参数的位置，这种关系保存起来
        Map<String,Integer> paramIndexMapping = new HashMap<>();

        //提取方法中加了注解的参数
        Annotation[] [] pa = handler.getMethod().getParameterAnnotations();
        for (int i = 0; i < pa.length ; i ++) {
            for(Annotation a : pa[i]){
                if(a instanceof QBKRequestParam){
                    String paramName = ((QBKRequestParam) a).value();
                    if(!"".equals(paramName.trim())){
                        paramIndexMapping.put(paramName,i);
                    }
                }
            }
        }

        //获取方法的形参列表
        Class<?> [] paramTypes = handler.getMethod().getParameterTypes();

        for (int i = 0; i < paramTypes.length; i++) {
            Class<?> paramterType = paramTypes[i];
            if(paramterType == HttpServletRequest.class || paramterType == HttpServletResponse.class){
                paramIndexMapping.put(paramterType.getName(),i);
            }
        }

        //request里面的参数 , 实参列表
        Map<String,String[]> params = req.getParameterMap();

        //保存赋值参数的列表
        Object [] paramValues = new Object[paramTypes.length];

        //迭代 实参列表
        for (Map.Entry<String,String[]> param : params.entrySet()) {
            String value = Arrays.toString(params.get(param.getKey()))
                    //数组参数
                    .replaceAll("\\[|\\]","")
                    //空格
                    .replaceAll("\\s+",",");

            if(!paramIndexMapping.containsKey(param.getKey())){
                continue;
            }
            //找到 参数的位置
            int index = paramIndexMapping.get(param.getKey());

            //赋值
            //允许自定义的类型转换器Converter
            paramValues[index] = castStringValue(value,paramTypes[index]);
        }

        //赋值
        if(paramIndexMapping.containsKey(HttpServletRequest.class.getName())){
            int index = paramIndexMapping.get(HttpServletRequest.class.getName());
            paramValues[index] = req;
        }

        //赋值
        if(paramIndexMapping.containsKey(HttpServletResponse.class.getName())){
            int index = paramIndexMapping.get(HttpServletResponse.class.getName());
            paramValues[index] = resp;
        }

        //反射 执行方法
        Object result = handler.getMethod().invoke(handler.getController(),paramValues);
        //返回值为空
        if(result == null || result instanceof Void){
            return null;
        }

        //返回  ModelAndView
        boolean isModelAndView = handler.getMethod().getReturnType() == QBKModelAndView.class;
        if(isModelAndView){
            return (QBKModelAndView)result;
        }

        return null;
    }

    /**
     * 类型转换
     */
    private Object castStringValue(String value, Class<?> paramType) {
        if(String.class == paramType){
            return value;
        }else if(Integer.class == paramType){
            return Integer.valueOf(value);
        }else if(Double.class == paramType){
            return Double.valueOf(value);
        }else {
            if(value != null){
                return value;
            }
            return null;
        }
    }
}
