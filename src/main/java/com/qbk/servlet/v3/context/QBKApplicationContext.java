package com.qbk.servlet.v3.context;

import com.qbk.annotation.QBKAutowired;
import com.qbk.annotation.QBKController;
import com.qbk.annotation.QBKService;
import com.qbk.servlet.v3.beans.QBKBeanWrapper;
import com.qbk.servlet.v3.beans.config.QBKBeanDefinition;
import com.qbk.servlet.v3.beans.support.QBKBeanDefinitionReader;
import lombok.Data;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 职责：完成Bean的创建和DI
 */
@Data
public class QBKApplicationContext {

    private QBKBeanDefinitionReader reader;

    /**
     * 保存 BeanDefinition配置信息
     */
    private Map<String,QBKBeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>();

    /**
     * IoC容器
     */
    private Map<String,QBKBeanWrapper> factoryBeanInstanceCache = new ConcurrentHashMap<>();

    /**
     * 保存 原生bean
     */
    private Map<String,Object> factoryBeanObjectCache = new ConcurrentHashMap<>();

    public QBKApplicationContext(String... configLocations) {
        //1、加载配置文件
        reader = new QBKBeanDefinitionReader(configLocations);
        try {
            //2、加载配置文件，扫描相关的类，把它们封装成BeanDefinition
            List<QBKBeanDefinition> beanDefinitions = reader.loadBeanDefinitions();
            //3、把BeanDefintion缓存起来
            doRegistBeanDefinition(beanDefinitions);
            //4、把不是延时加载的类，有提前初始化
            doAutowrited();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 把BeanDefintion缓存起来
     */
    private void doRegistBeanDefinition(List<QBKBeanDefinition> beanDefinitions) throws Exception {
        for (QBKBeanDefinition beanDefinition : beanDefinitions) {
            if(this.beanDefinitionMap.containsKey(beanDefinition.getFactoryBeanName())){
                throw new Exception("The " + beanDefinition.getFactoryBeanName() + "is exists");
            }
            beanDefinitionMap.put(beanDefinition.getFactoryBeanName(),beanDefinition);
            beanDefinitionMap.put(beanDefinition.getBeanClassName(),beanDefinition);
        }
    }
    /**
     * 调用getBean()
     */
    private void doAutowrited() {
        //这一步，所有的Bean并没有真正的实例化，还只是配置阶段
        for (Map.Entry<String,QBKBeanDefinition> beanDefinitionEntry : this.beanDefinitionMap.entrySet()) {
            String beanName = beanDefinitionEntry.getKey();
            getBean(beanName);
        }
    }

    /**
     * Bean的实例化，DI是从而这个方法开始的
     * 通过读取BeanDefinition中的信息
     * 然后，通过反射机制创建一个实例并返回
     * Spring做法是，不会把最原始的对象放出去，会用一个BeanWrapper来进行一次包装
     */
    public Object getBean(String beanName){
        //1、先拿到BeanDefinition配置信息
        QBKBeanDefinition beanDefinition = this.beanDefinitionMap.get(beanName);
        //2、反射实例化newInstance();
        Object instance = instantiateBean(beanName,beanDefinition);
        //3、封装成一个叫做BeanWrapper
        QBKBeanWrapper beanWrapper = new QBKBeanWrapper(instance);
        //4、保存到IoC容器
        factoryBeanInstanceCache.put(beanName,beanWrapper);
        //5、执行依赖注入
        populateBean(beanName,beanDefinition,beanWrapper);
        return beanWrapper.getWrapperInstance();
    }

    /**
     * 创建真正的实例对象
     */
    private Object instantiateBean(String beanName, QBKBeanDefinition beanDefinition) {
        String className = beanDefinition.getBeanClassName();
        Object instance = null;
        try {
            Class<?> clazz = Class.forName(className);
            instance = clazz.newInstance();
            this.factoryBeanObjectCache.put(beanName, instance);
        }catch (Exception e){
            e.printStackTrace();
        }
        return instance;
    }

    /**
     * 依赖注入
     */
    private void populateBean(String beanName, QBKBeanDefinition beanDefinition, QBKBeanWrapper beanWrapper) {
        //可能涉及到循环依赖？
        //A{ B b}
        //B{ A b}
        //用两个缓存，循环两次
        //1、把第一次读取结果为空的BeanDefinition存到第一个缓存
        //2、等第一次循环之后，第二次循环再检查第一次的缓存，再进行赋值

        Object instance = beanWrapper.getWrapperInstance();

        Class<?> clazz = beanWrapper.getWrappedClass();

        //判断只有加了注解的类，才执行依赖注入
        if(!(clazz.isAnnotationPresent(QBKController.class) || clazz.isAnnotationPresent(QBKService.class))){
            return;
        }

        //把所有的包括private/protected/default/public 修饰字段都取出来
        for (Field field : clazz.getDeclaredFields()) {

            if(!field.isAnnotationPresent(QBKAutowired.class)){
                continue;
            }
            QBKAutowired autowired = field.getAnnotation(QBKAutowired.class);

            //如果用户没有自定义的beanName，就默认根据类型注入
            String autowiredBeanName = autowired.value().trim();
            if("".equals(autowiredBeanName)){
                //field.getType().getName() 获取字段的类型
                autowiredBeanName = field.getType().getName();
            }

            //暴力访问
            field.setAccessible(true);

            try {
                if(this.factoryBeanInstanceCache.get(autowiredBeanName) == null){
                    continue;
                }
                //ioc.get(beanName) 相当于通过接口的全名拿到接口的实现的实例
                field.set(instance,this.factoryBeanInstanceCache.get(autowiredBeanName).getWrapperInstance());
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    public Object getBean(Class beanClass){
        return getBean(beanClass.getName());
    }
}
