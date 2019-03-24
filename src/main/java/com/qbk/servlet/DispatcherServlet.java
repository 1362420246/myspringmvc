package com.qbk.servlet;

import com.qbk.annotation.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by 13624 on 2018/8/9.
 */
public class DispatcherServlet extends HttpServlet{

    List<String> classNames = new ArrayList<String>();
    Map<String,Object> beans = new ConcurrentHashMap<String, Object>();
    Map<String, Object> handerMap = new HashMap<String, Object>();

    public DispatcherServlet() {
        super();
        System.out.println("servlet启动成功");
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        //super.init(config);

        //扫描bean
        scanPackage("com.qbk");
        //实例化bean
        doInstance();
        //依赖注入
        doIoc();
        //url映射
        buildUrlMapping();
    }

    //扫描bean
    private void  scanPackage(String basePackage){
        System.out.println("basePackage:"+basePackage);
        URL url = this.getClass().getClassLoader().getResource("/" + basePackage.replaceAll("\\.", "/"));
        String pathFile = url.getFile();
        File file =new File(pathFile) ;
        String[] list = file.list();
        for (String  path : list){
            File filePath =new File(pathFile + path) ;
            if (filePath.isDirectory()){
                //递归
                scanPackage(basePackage +"."+path);
            }else {
                System.out.println("className:"+basePackage+"."+filePath.getName());
                classNames.add(basePackage+"."+filePath.getName());
            }
        }
        
        
    }

    //实例化bean
    private void  doInstance(){
        if(classNames.size() <= 0){
            System.out.println("扫描包失败。。。。。");
            return;
        }

        for (String className : classNames ) {
            String cn = className.replace(".class","");
            Class<?> clazz = null;
            try {
                clazz = Class.forName(cn);
                //判断此类是否使用了 某个注解
                if (clazz.isAnnotationPresent(QBKController.class)){//controller

                    Object instance = clazz.newInstance();

                    QBKRequestMapping requestMapping = clazz.getAnnotation(QBKRequestMapping.class);
                    String rmValue = requestMapping.value();

                    beans.put(rmValue , instance) ;
                }else if (clazz.isAnnotationPresent(QBKService.class)){//service

                    Object instance = clazz.newInstance();

                    QBKService service = clazz.getAnnotation(QBKService.class);
                    String serviceName = service.value();

                    beans.put( serviceName, instance) ;//MyServiceImpl
                }else {
                    continue;
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            }

        }
    }

    //依赖注入
    private void doIoc(){
        if(beans.entrySet().size()<=0){
            System.out.println("实例化bean失败。。。。。");
            return;
        }
        for (Map.Entry<String,Object> entry: beans.entrySet()){

            Object instance = entry.getValue();
            Class<?> clazz = instance.getClass();

            if (clazz.isAnnotationPresent(QBKController.class)){

                Field[] fields = clazz.getDeclaredFields();

                for (Field field :fields){
                    //判断此字段是否使用了 某个注解
                    if (field.isAnnotationPresent(QBKAutowired.class)){
                        //拿到注解
                        QBKAutowired annotation = field.getAnnotation(QBKAutowired.class);
                        //拿到注解 写的值
                        String serviceName = annotation.value();//MyServiceImpl
                        //可访问私有属性
                        field.setAccessible(true);
                        try {
                            //注入
                            field.set(instance,beans.get(serviceName));
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }

                    }else {
                        continue;
                    }
                }
            }else {
                continue;
            }
        }
    }

    //url映射
   private void buildUrlMapping(){
       if(beans.entrySet().size()<=0){
           System.out.println("实例化bean失败。。。。。");
           return;
       }

       for (Map.Entry<String, Object> entry : beans.entrySet()) {
          Object instance =  entry.getValue();
           Class<?> clazz = instance.getClass();

           if (clazz.isAnnotationPresent(QBKController.class)) {
               QBKRequestMapping requestMapping = clazz.getAnnotation(QBKRequestMapping.class);
               //类上路径
               String classPath = requestMapping.value();
               Method[] methods = clazz.getMethods();
               for (Method method : methods) {
                   if (method.isAnnotationPresent(QBKRequestMapping.class)) {
                       QBKRequestMapping methodMapping =  method.getAnnotation(QBKRequestMapping.class);
                       //方法上路径
                       String methodPath = methodMapping.value();
                       handerMap.put(classPath + methodPath, method);
                   } else {
                       continue;
                   }
               }
           } else {
               continue;
           }
       }
   }


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {


        String url = req.getRequestURI();
        String context = req.getContextPath();
        String path = url.replace(context, "");
        System.out.println("classNames:"+classNames);
        System.out.println("beans:"+beans);
        System.out.println("handerMap:"+handerMap);
        Method method = (Method) handerMap.get(path);
        String bean = "/"+path.split("/")[1] ; 
        Object controller = beans.get(bean);

        Object[] args =hand(req,resp,method);
        try {
            Object invoke = method.invoke(controller, args);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private static Object[] hand(HttpServletRequest req, HttpServletResponse resp ,Method method){

       Class<?>[] paramClazzs = method.getParameterTypes();

       Object[] args = new Object[paramClazzs.length];

       int args_i = 0;
       int index = 0 ;
       for (Class<?> paramClazz : paramClazzs){
           if (ServletRequest.class.isAssignableFrom(paramClazz)){
               args[args_i++] = req ;
           }
           if (ServletResponse.class.isAssignableFrom(paramClazz)){
               args[args_i++] = resp;
           }
           Annotation[] paraAns = method.getParameterAnnotations()[index];
           if (paraAns.length > 0){
               for (Annotation paramAn : paraAns ) {
                   if(QBKRequestParam.class.isAssignableFrom(paramAn.getClass())){
                       QBKRequestParam rq = (QBKRequestParam) paramAn;
                       args[args_i++] = req.getParameter(rq.value());

                   }
               }
           }
           index ++ ;
       }
        return args;
    }

}
