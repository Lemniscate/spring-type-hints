package com.github.lemniscate.spring.typehint;

/**
 * Created by dave on 2/25/15.
 */
public interface TypeHintingDependency {
    Class<?>[] getDependencyTypeHints(Class<?> type);
}
