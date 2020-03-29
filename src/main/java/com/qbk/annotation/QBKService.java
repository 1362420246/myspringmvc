package com.qbk.annotation;

import java.lang.annotation.*;

@Target({ElementType.TYPE}) //该注解适用于class,interface,enum等类级别的目标对象
@Retention(RetentionPolicy.RUNTIME)//运行时
@Documented //javadoc
public @interface QBKService {
    String value() default "";
}
