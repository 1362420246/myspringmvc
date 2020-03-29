package com.qbk.annotation;

import java.lang.annotation.*;

@Target({ElementType.FIELD}) //字段
@Retention(RetentionPolicy.RUNTIME)//运行时
@Documented //javadoc
public @interface QBKAutowired {
    String value() default "" ;
}
