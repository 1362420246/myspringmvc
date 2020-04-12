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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * servlet v3版本
 * 委派模式
 * 职责：负责任务调度，请求分发
 */
public class DispatcherServlet extends HttpServlet{

    public DispatcherServlet() {
        super();
        System.out.println("servlet init");
    }

    /**
     * ApplicationContext
     */
    private QBKApplicationContext applicationContext;

    /**
     * 保存Contrller中所有Mapping的对应关系
     */
    private List<QBKHandlerMapping> handlerMappings = new ArrayList<>();

    /**
     * HandlerMapping 和 HandlerAdapter 的关联
     */
    private Map<QBKHandlerMapping,QBKHandlerAdapter> handlerAdapters = new HashMap<>();

    /**
     * 保存试图解析器
     */
    private List<QBKViewResolver> viewResolvers = new ArrayList<>();

    @Override
    public void init(ServletConfig config) throws ServletException {
        //初始化Spring核心IoC容器
        //完成了IoC、DI
        applicationContext = new QBKApplicationContext(config.getInitParameter("contextConfigLocation"));

        //初始化mvc 九大组件
        initStrategies(applicationContext);
    }

    /**
     * 初始化九大组件
     */
    private void initStrategies(QBKApplicationContext context) {
//        //多文件上传的组件
//        initMultipartResolver(context);
//        //初始化本地语言环境
//        initLocaleResolver(context);
//        //初始化模板处理器
//        initThemeResolver(context);
        //初始化处理器映射器
        initHandlerMappings(context);
        //初始化参数适配器
        initHandlerAdapters(context);
//        //初始化异常拦截器
//        initHandlerExceptionResolvers(context);
//        //初始化视图预处理器
//        initRequestToViewNameTranslator(context);
        //初始化视图转换器
        initViewResolvers(context);
//        //FlashMap管理器
//        initFlashMapManager(context);
    }

    /**
     * 初始化HandlerMapping
     */
    private void initHandlerMappings(QBKApplicationContext context) {
        if(this.applicationContext.getBeanDefinitionCount() == 0){
            return;
        }
        for (String beanName : this.applicationContext.getBeanDefinitionNames()) {
            Object instance = applicationContext.getBean(beanName);
            Class<?> clazz = instance.getClass();
            //只处理controller
            if (clazz.isAnnotationPresent(QBKController.class)) {
                //提取 class上配置的url
                String baseUrl = "";
                //有没有加RequestMapping注解
                if(clazz.isAnnotationPresent(QBKRequestMapping.class)){
                    QBKRequestMapping requestMapping = clazz.getAnnotation(QBKRequestMapping.class);
                    baseUrl = requestMapping.value();
                }
                //获取Method的url配置
                Method[] methods = clazz.getMethods();
                for (Method method : methods) {
                    //有没有加RequestMapping注解
                    if(!method.isAnnotationPresent(QBKRequestMapping.class)){
                        continue;
                    }
                    //提取每个方法上面配置的url
                    QBKRequestMapping requestMapping =  method.getAnnotation(QBKRequestMapping.class);
                    //路径
                    String regex = ("/" + baseUrl + "/" +
                            requestMapping.value().replaceAll("\\*",".*"))
                            .replaceAll("/+","/");
                    Pattern pattern = Pattern.compile(regex);
                    handlerMappings.add(new QBKHandlerMapping(pattern,instance,method));
                }
            }
        }
    }

    /**
     * 初始化参数适配器
     */
    private void initHandlerAdapters(QBKApplicationContext context) {
        for (QBKHandlerMapping handlerMapping : handlerMappings) {
            this.handlerAdapters.put(handlerMapping,new QBKHandlerAdapter());
        }
    }

    /**
     * 初始化视图转换器
     */
    private void initViewResolvers(QBKApplicationContext context) {
        //读取模板根路径配置
        String templateRoot = context.getConfig().getProperty("templateRoot");
        this.viewResolvers.add(new QBKViewResolver(templateRoot));
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //委派，根据url ，找到对应的  method，然后执行
        try {
            doDispatch(req,resp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 委派，根据url ，找到对应的  method，然后执行
     */
    private void doDispatch(HttpServletRequest req, HttpServletResponse resp)throws Exception {
        //完成了对HandlerMapping的封装
        //完成了对方法返回值的封装ModelAndView

        //1、通过URL获得一个HandlerMapping
        QBKHandlerMapping handler = this.getHandler(req);
        if(handler == null){
            processDispatchResult(resp,new QBKModelAndView("404"));
            return;
        }

        //2、根据一个HandlerMaping获得一个HandlerAdapter
        QBKHandlerAdapter ha = getHandlerAdapter(handler);

        //3、解析某一个方法的形参和返回值之后，统一封装为ModelAndView对象
        QBKModelAndView mv = ha.handler(req,resp,handler);

        // 就把ModelAndView变成一个ViewResolver
        processDispatchResult(resp,mv);

    }

    /**
     * 获取 handlerMaping
     */
    private QBKHandlerMapping getHandler(HttpServletRequest req) {
        if(this.handlerMappings.isEmpty()){
            return  null;
        }
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replaceAll(contextPath,"").replaceAll("/+","/");
        for (QBKHandlerMapping mapping : handlerMappings) {
            //url 正则匹配
            Matcher matcher = mapping.getPattern().matcher(url);
            if(!matcher.matches()){
                continue;
            }
            return mapping;
        }
        return null;
    }

    /**
     * 获取 QBKHandlerAdapter
     */
    private QBKHandlerAdapter getHandlerAdapter(QBKHandlerMapping handler) {
        if(this.handlerAdapters.isEmpty()){
            return null;
        }
        return this.handlerAdapters.get(handler);
    }

    /**
     * 输出结果
     * 把ModelAndView变成一个ViewResolver
     * 从ViewResolver解析处 view
     * view 显然页面
     */
    private void processDispatchResult( HttpServletResponse resp, QBKModelAndView mv) throws Exception {
        if(null == mv){
            return;
        }
        if(this.viewResolvers.isEmpty()){
            return;
        }

        for (QBKViewResolver viewResolver : this.viewResolvers) {
            //试图解析
            QBKView view = viewResolver.resolveViewName(mv.getViewName());
            //直接往浏览器输出
            view.render(mv.getModel(),resp);
            return;
        }
    }

}
