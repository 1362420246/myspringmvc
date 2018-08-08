package com.qbk.annotation;

import java.lang.annotation.*;

/**
 * Created by 13624 on 2018/8/9.
 */
@Target({ElementType.TYPE,ElementType.METHOD}) // class,interface,enum  和 方法
@Retention(RetentionPolicy.RUNTIME)//运行时
@Documented //javadoc
public @interface QBKRequestMapping {
    String value() default "";
}
