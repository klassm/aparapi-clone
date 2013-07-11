package com.amd.aparapi.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * This annotation is for internal use only
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface OpenCLMapping {
   String mapTo() default "";

   boolean atomic32() default false;

   boolean atomic64() default false;
}
