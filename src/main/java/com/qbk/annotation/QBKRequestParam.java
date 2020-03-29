package com.qbk.annotation;

import java.lang.annotation.*;

@Target({ElementType.PARAMETER}) //参数
@Retention(RetentionPolicy.RUNTIME)//运行时
@Documented //javadoc
public @interface QBKRequestParam {
    String value() default "";
}
