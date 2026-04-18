package com.netbridge.module.log.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationAudit {

    String action();

    String resourceType();

    String resourceId() default "";

    String successDetail() default "";

    String failureDetail() default "";

    boolean recordOnSuccess() default false;

    boolean recordOnFailure() default true;
}
