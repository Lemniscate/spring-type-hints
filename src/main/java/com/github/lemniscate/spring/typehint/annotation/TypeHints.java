package com.github.lemniscate.spring.typehint.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by dave on 2/25/15.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface TypeHints{
    Class<?>[] value();
}
