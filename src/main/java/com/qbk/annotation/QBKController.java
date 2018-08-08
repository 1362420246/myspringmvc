package com.qbk.annotation;

import java.lang.annotation.*;

/**
 * Created by 13624 on 2018/8/9.
 */
@Target({ElementType.TYPE}) //该注解适用于class,interface,enum等类级别的目标对象
@Retention(RetentionPolicy.RUNTIME)//运行时
@Documented //javadoc
public @interface QBKController {
    String value() default "" ;
}
