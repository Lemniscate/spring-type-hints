package com.github.lemniscate.spring.typehint.beans;

import com.github.lemniscate.spring.typehint.HasTypeHints;
import com.github.lemniscate.spring.typehint.TypeHintSpringApplication;
import com.github.lemniscate.spring.typehint.annotation.TypeHints;
import com.github.lemniscate.spring.typehint.beans.TypeHintListableBeanFactory;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.util.Assert;

/**
 * Created by dave on 2/25/15.
 */
public class TypeHintListableBeanFactoryTest {

    private DefaultListableBeanFactory bf;
    private ConfigurableApplicationContext ctx;

    @Configuration
    public static class BasicConfig {
        @Bean
        public Service<Long> longService(){
            return new ServiceImpl<Long>(1L);
        }

        @Bean
        public Service<String> stringService(){
            return new ServiceImpl<String>("Hi there");
        }

        @Bean
        public Service<String> concreteStringService(){
            return new StringService("I'm just a guy");
        }
        @Bean
        public TypedClient<String> stringClient(){
            return new TypedClient<>();
        }
    }

    @Configuration
    public static class TooManyServicesConfig extends BasicConfig {
        @Bean
        public Service<String> concreteStringService2(){
            return new StringService("I'm the primary");
        }
    }

    @Configuration
    public static class PrimaryConfig extends TooManyServicesConfig{
        @Bean
        @Primary
        @Override
        public Service<String> concreteStringService2() {
            return super.concreteStringService2();
        }
    }

    @Configuration
    public static class HasTypeHintsConfig extends BasicConfig{
        @Bean
        @Override
        public Service<String> concreteStringService() {
            return new HasTypeHintService("Type hinted");
        }
    }


    @Before
    public void before(){
        bf = new TypeHintListableBeanFactory();
        bf.registerBeanDefinition("annotationPostProcessor", new RootBeanDefinition(AutowiredAnnotationBeanPostProcessor.class));
        bf.addBeanPostProcessor(bf.getBean(AutowiredAnnotationBeanPostProcessor.class));

        ctx = new GenericApplicationContext(bf);
        ctx.addBeanFactoryPostProcessor(new ConfigurationClassPostProcessor());
    }


    @Test
    public void testBasicConfig() throws Throwable {
        bf.registerBeanDefinition("conf", new RootBeanDefinition(BasicConfig.class));
        ctx.refresh();
        Object client = ctx.getBean(TypedClient.class);
        Assert.notNull(client);
    }

    @Test
    public void testPrimaryConfig() throws Throwable {
        bf.registerBeanDefinition("conf", new RootBeanDefinition(PrimaryConfig.class));
        ctx.refresh();
        Object client = ctx.getBean(TypedClient.class);
        Assert.notNull(client);
    }

    @Test
    public void testHasTypeHintsConfig() throws Throwable {
        bf.registerBeanDefinition("conf", new RootBeanDefinition(HasTypeHintsConfig.class));
        ctx.refresh();
        Object client = ctx.getBean(TypedClient.class);
        Assert.notNull(client);
    }


    @Test(expected = TypeHintListableBeanFactory.NoTypeHintedBeanDefinitionException.class)
    public void testTooManyServiceDefinitions() throws Throwable {
        try {
            bf.registerBeanDefinition("conf", new RootBeanDefinition(TooManyServicesConfig.class));
            ctx.refresh();
            Object client = ctx.getBean(TypedClient.class);
            Assert.notNull(client);
        }catch(BeanCreationException bce){
            throw bce.getCause().getCause();
        }
    }

    @Test
    public void testSpringApplicationExample(){
        new TypeHintSpringApplication(BasicConfig.class).run();
    }



    public static interface Service<T> {
        T get();
    }


    public static class ServiceImpl<T> implements Service<T>{

        private final T t;

        public ServiceImpl(T t) {
            this.t = t;
        }

        @Override
        public T get() {
            return t;
        }
    }

    public static class StringService extends ServiceImpl<String>{
        public StringService(String s) {
            super(s);
        }
    }

    public static class HasTypeHintService extends ServiceImpl<String> implements HasTypeHints{

        public HasTypeHintService(String s) {
            super(s);
        }

        @Override
        public Class<?>[] getTypeHints() {
            return new Class<?>[]{String.class};
        }
    }

    public static class TypedClient<T> {
        @Autowired
        @TypeHints({String.class})
        private Service<T> service;
    }




}
