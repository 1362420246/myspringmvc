package com.qbk.servlet.v2;

import com.qbk.annotation.*;
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
 * servlet v2版本
 */
public class DispatcherServlet extends HttpServlet{

    /**
     * 存储aplication.properties的配置内容
     */
    private Properties contextConfig = new Properties();

    /**
     * 存储所有扫描到的类
     */
    private List<String> classNames = new ArrayList<>();

    /**
     * IoC 容器，key默认是首字母小写，value是实例对象
     */
    private Map<String,Object> ioc = new ConcurrentHashMap<>();

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
        //1.加载配置文件
        doLoadConfig(config.getInitParameter("contextConfigLocation"));

        //2.扫描相关的类
        doScanner(contextConfig.getProperty("scanPackage"));

        //=============IoC部分================
        //3.初始化ioc容器，将扫描的相关的类实例化，保存在ioc容器中
        doInstance();

        //=============AOP部分================
        //AOP,新生成的代理对象

        //=============DI部分================
        //4.完成依赖注入
        doAutowired();

        //=============MVC部分================
        //5.初始化HandlerMapping
        doInitHandlerMapping();
    }

    /**
     * 1.加载配置文件
     */
    private void doLoadConfig(String contextConfiglocation) {
        InputStream is =this.getClass().getClassLoader().getResourceAsStream(contextConfiglocation);
        try {
            contextConfig.load(is);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if(is != null){
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 2.扫描相关的类
     */
    private void  doScanner(String scanPackage){
        //包传过来包下面的所有的类全部扫描进来的
        URL url = this.getClass().getClassLoader()
                .getResource("/" + scanPackage.replaceAll("\\.","/"));
        File classPath = new File(url.getFile());

        for (File file : classPath.listFiles()) {
            if(file.isDirectory()){
                doScanner(scanPackage + "." + file.getName());
            }else {
                if(!file.getName().endsWith(".class")){
                    continue;
                }
                String className = (scanPackage + "." + file.getName()).replace(".class","");
                classNames.add(className);
            }
        }
    }

    /**
     * 3.初始化ioc容器
     */
    private void  doInstance(){
        if(classNames.isEmpty()){
            return;
        }
        for (String className : classNames ) {
            Class<?> clazz;
            try {
                clazz = Class.forName(className);
                //判断此类是否使用了 某个注解
                if (clazz.isAnnotationPresent(QBKController.class)){
                    //controller
                    Object instance = clazz.newInstance();
                    //bean 类名首字母小写
                    String beanName = toLowerFirstCase(clazz.getSimpleName());
                    ioc.put(beanName , instance) ;
                }else if (clazz.isAnnotationPresent(QBKService.class)){
                    //service
                    Object instance = clazz.newInstance();
                    //1、bean 类名首字母小写
                    String beanName = toLowerFirstCase(clazz.getSimpleName());
                    //2、自定义命名
                    QBKService service = clazz.getAnnotation(QBKService.class);
                    if(!"".equals(service.value())){
                        beanName = service.value();
                    }
                    ioc.put( beanName, instance) ;
                    //3、如果是接口 。如果多个实现类
                    for (Class<?> i : clazz.getInterfaces()) {
                        if(ioc.containsKey(i.getName())){
                            throw new Exception("The beanName is exists!!");
                        }
                        ioc.put(i.getName(),instance);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 首字母变小写
     */
    private String toLowerFirstCase(String simpleName) {
        char [] chars = simpleName.toCharArray();
        chars[0] += 32;
        return  String.valueOf(chars);
    }
    /**
     * 4.完成依赖注入
     */
    private void doAutowired(){
        if(ioc.entrySet().size()<=0){
            System.out.println("实例化bean失败。。。。。");
            return;
        }
        for (Map.Entry<String,Object> entry: ioc.entrySet()){
            Object instance = entry.getValue();
            Class<?> clazz = instance.getClass();
            if (clazz.isAnnotationPresent(QBKController.class)){
                //拿到实例对象中的所有属性
                Field[] fields = clazz.getDeclaredFields();
                for (Field field :fields){
                    //判断此字段是否使用了 某个注解
                    if (field.isAnnotationPresent(QBKAutowired.class)){
                        //拿到注解
                        QBKAutowired annotation = field.getAnnotation(QBKAutowired.class);
                        //拿到注解 写的值
                        String serviceName = annotation.value().trim();
                        if("".equals(serviceName)){
                            serviceName = field.getType().getName();
                        }
                        //可访问私有属性
                        field.setAccessible(true);
                        try {
                            //注入
                            field.set(instance,ioc.get(serviceName));
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    /**
     * 5.初始化HandlerMapping
     */
   private void doInitHandlerMapping(){
       if(ioc.isEmpty()){
           return;
       }
       for (Map.Entry<String, Object> entry : ioc.entrySet()) {
           Object instance =  entry.getValue();
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
        //再调用toLowerFirstCase获得beanName
        String beanName = toLowerFirstCase(method.getDeclaringClass().getSimpleName());
        method.invoke(ioc.get(beanName),paramValues);
    }

}
