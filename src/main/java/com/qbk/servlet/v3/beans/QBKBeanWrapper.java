package com.qbk.servlet.v3.beans;

import lombok.Data;

/**
 * bean的包装
 */
@Data
public class QBKBeanWrapper {
    private Object wrapperInstance;
    private Class<?> wrappedClass;

    public QBKBeanWrapper(Object instance) {
        this.wrapperInstance = instance;
        this.wrappedClass = instance.getClass();
    }
}
