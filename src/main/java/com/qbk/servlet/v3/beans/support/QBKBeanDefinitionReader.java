package com.qbk.servlet.v3.beans.support;

import com.qbk.servlet.v3.beans.config.QBKBeanDefinition;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * 读取配置
 */
public class QBKBeanDefinitionReader {

    /**
     * 保存扫描的结果
     * 需要注册的 beanClass
     */
    private List<String> regitryBeanClasses = new ArrayList<>();

    /**
     * 配置文件
     */
    private Properties contextConfig = new Properties();

    /**
     * 加载配置文件
     */
    public QBKBeanDefinitionReader(String... configLocations) {
        //1.加载配置文件
        doLoadConfig(configLocations[0]);
        //2.扫描配置文件中的配置的相关的类
        doScanner(contextConfig.getProperty("scanPackage"));
    }
    /**
     * 1、加载配置文件
     */
    private void doLoadConfig(String contextConfigLocation) {
        //通过URL定位找到其所对应的文件，然后转换为文件流
        InputStream is = this.getClass().getClassLoader().getResourceAsStream(
                contextConfigLocation.replaceAll("classpath:","")
        );
        try {
            contextConfig.load(is);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if(null != is){
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    /**
     * 2、扫描配置文件中的配置的相关的类
     * 递归
     */
    private void doScanner(String scanPackage) {
        //转换为文件路径，实际上就是把.替换为/
        URL url = this.getClass().getClassLoader().getResource(
                "/" + scanPackage.replaceAll("\\.","/"));

        //扫描的根路径
        File classPath = new File(url.getFile());

        //当成是一个ClassPath文件夹
        for (File file : classPath.listFiles()) {
            if(file.isDirectory()){
                doScanner(scanPackage + "." + file.getName());
            }else {
                if(!file.getName().endsWith(".class")){
                    continue;
                }
                if(file.getAbsolutePath().contains("servlet")){
                    continue;
                }
                //全类名 = 包名.类名
                String className = (scanPackage + "." + file.getName().replace(".class", ""));
                //Class.forName(className);
                regitryBeanClasses.add(className);
            }
        }
    }

    /**
     *  解析配置文件，封装成BeanDefinition
     */
    public List<QBKBeanDefinition> loadBeanDefinitions() {
        List<QBKBeanDefinition> result = new ArrayList<>();
        try {
            for (String className : regitryBeanClasses) {

                //全类名反射
                Class<?> beanClass = Class.forName(className);

                //如果是一个接口，是不能实例化的。用它实现类来实例化
                if(beanClass.isInterface()) {
                    continue;
                }

                //保存类对应的ClassName（全类名）、还有beanName
                //beanName有三种情况:
                //1、默认是类名首字母小写
                result.add(doCreateBeanDefinition(toLowerFirstCase(beanClass.getSimpleName()), beanClass.getName()));
                //2、自定义 TODO
                //3、接口注入
                for (Class<?> i : beanClass.getInterfaces()) {
                    //如果是多个实现类，只能覆盖
                    //为什么？因为Spring没那么智能，就是这么傻
                    //这个时候，可以自定义名字
                    result.add(doCreateBeanDefinition(i.getName(),beanClass.getName()));
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return result;
    }

    /**
     *  创建BeanDefinition
     */
    private QBKBeanDefinition doCreateBeanDefinition(String beanName, String beanClassName) {
        QBKBeanDefinition beanDefinition = new QBKBeanDefinition();
        beanDefinition.setFactoryBeanName(beanName);
        beanDefinition.setBeanClassName(beanClassName);
        return beanDefinition;
    }

    /**
     * 获取配置
     */
    public Properties getContextConfig() {
        return contextConfig;
    }

    /**
     * 首字母小写
     */
    private String toLowerFirstCase(String simpleName) {
        char [] chars = simpleName.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }
}
