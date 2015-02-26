package com.github.lemniscate.spring.typehint.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by dave on 2/25/15.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface TypeHints{
    Class<?>[] value() default {};
    Class<? extends TypeHintResolver> resolver() default TypeHintResolver.class;

    public static interface TypeHintResolver{
        Class<?>[] getTypeHints(Class<?> source, Class<?> type);
    }
}
