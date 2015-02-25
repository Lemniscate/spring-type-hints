package com.github.lemniscate.spring.typehint;

import com.github.lemniscate.spring.typehint.beans.TypeHintListableBeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;

/**
 * Created by dave on 2/25/15.
 */
public class TypeHintSpringApplication extends SpringApplication {

    public TypeHintSpringApplication(Object... sources) {
        super(sources);
    }

    public TypeHintSpringApplication(ResourceLoader resourceLoader, Object... sources) {
        super(resourceLoader, sources);
    }

    @Override
    protected ConfigurableApplicationContext createApplicationContext() {
        ConfigurableApplicationContext result = super.createApplicationContext();
        Assert.isInstanceOf(AbstractApplicationContext.class, result, "Cannot hijack a non-AbstractApplicationContext");
        TypeHintListableBeanFactory.hijack(((AbstractApplicationContext) result));
        return result;
    }
}
