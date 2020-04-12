package com.qbk.servlet.v3.beans.config;

import lombok.Data;

/**
 * 配置
 */
@Data
public class QBKBeanDefinition {
    /**
     * bean name
     */
    private String factoryBeanName;
    /**
     * class name
     */
    private String beanClassName;
}
