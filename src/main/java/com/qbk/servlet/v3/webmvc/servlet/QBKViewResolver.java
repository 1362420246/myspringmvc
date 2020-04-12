package com.qbk.servlet.v3.webmvc.servlet;

import java.io.File;

/**
 * 试图解析器
 * 试图转换器，模板引擎
 */
public class QBKViewResolver {
    /**
     * 默认模板后缀
     */
    private final String DEFAULT_TEMPLATE_SUFFIX = ".html";

    /**
     * 模板 根路径
     */
    private File tempateRootDir;

    /**
     * 保存模板 根路径
     * @param templateRoot 模板路径配置
     */
    public QBKViewResolver(String templateRoot) {
        //根路径
        String templateRootPath = this.getClass().getClassLoader().getResource(templateRoot).getFile();
        tempateRootDir = new File(templateRootPath);
    }

    /**
     * 视图名称 解析
     */
    public QBKView resolveViewName(String viewName){
        if(null == viewName || "".equals(viewName.trim())) {
            return null;
        }
        viewName = viewName.endsWith(DEFAULT_TEMPLATE_SUFFIX)? viewName : (viewName + DEFAULT_TEMPLATE_SUFFIX);
        //创建模板
        File templateFile = new File((tempateRootDir.getPath() + "/" + viewName).replaceAll("/+","/"));
        return new QBKView(templateFile);
    }
}
