package com.qbk.annotation;

import java.lang.annotation.*;

/**
 * Created by 13624 on 2018/8/9.
 */
@Target({ElementType.PARAMETER}) //参数
@Retention(RetentionPolicy.RUNTIME)//运行时
@Documented //javadoc
public @interface QBKRequestParam {
    String value() default "";
}
