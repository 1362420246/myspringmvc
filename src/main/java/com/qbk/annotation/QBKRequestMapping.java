package com.qbk.annotation;

import java.lang.annotation.*;

@Target({ElementType.TYPE,ElementType.METHOD}) // class,interface,enum  和 方法
@Retention(RetentionPolicy.RUNTIME)//运行时
@Documented //javadoc
public @interface QBKRequestMapping {
    String value() default "";
}
