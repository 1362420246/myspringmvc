package com.qbk.annotation;

import java.lang.annotation.*;

/**
 * Created by 13624 on 2018/8/9.
 */
@Target({ElementType.FIELD}) //字段
@Retention(RetentionPolicy.RUNTIME)//运行时
@Documented //javadoc
public @interface QBKAutowired {
    String value() default "" ;
}
