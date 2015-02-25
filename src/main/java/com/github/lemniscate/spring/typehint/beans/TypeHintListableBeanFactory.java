package com.github.lemniscate.spring.typehint.beans;

import com.github.lemniscate.spring.typehint.HasTypeHints;
import com.github.lemniscate.spring.typehint.annotation.TypeHints;
import org.springframework.beans.BeansException;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.GenericTypeResolver;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Created by dave on 2/25/15.
 */
public class TypeHintListableBeanFactory extends DefaultListableBeanFactory {

    @Override
    public Object doResolveDependency(DependencyDescriptor descriptor, String beanName, Set<String> autowiredBeanNames, TypeConverter typeConverter) throws BeansException {
        try {
            return super.doResolveDependency(descriptor, beanName, autowiredBeanNames, typeConverter);
        } catch (NoUniqueBeanDefinitionException e) {

            if( e.getNumberOfBeansFound() > 1 ) {
                TypeHints hints = descriptor.getField().getAnnotation(TypeHints.class);
                if (hints != null) {
                    Class<?> requiredType = e.getBeanType();

                    Map<String, Object> candidates = new HashMap<String, Object>();
                    Map<String, ?> potentials = getBeansOfType(requiredType);
                    for (String name : potentials.keySet()) {
                        Object o = potentials.get(name);
                        Class<?>[] args;
                        if( HasTypeHints.class.isAssignableFrom(o.getClass()) ){
                            args = ((HasTypeHints) o).getTypeHints();
                        }else{
                            args = GenericTypeResolver.resolveTypeArguments(o.getClass(), requiredType);
                        }
                        if (Arrays.equals(args, hints.value())) {
                            candidates.put(name, o);
                        }
                    }

                    switch(candidates.size()){
                        case 0:
                            throw new NoTypeHintedBeanDefinitionException(requiredType, "Could not determine a single match from TypeHint");
                        case 1:
                            return candidates.values().iterator().next();
                        default:
                            String primaryCandidate = determinePrimaryCandidate(candidates, requiredType);
                            if (primaryCandidate != null) {
                                return candidates.get(primaryCandidate);
                            }
                            String priorityCandidate = determineHighestPriorityCandidate(candidates, requiredType);
                            if (priorityCandidate != null) {
                                return candidates.get(priorityCandidate);
                            }
                            throw new NoTypeHintedBeanDefinitionException(requiredType, candidates.size(), "Multiple matches on TypeHint");
                    }


                }
            }
            throw e;
        }
    }

    public static void hijack(AbstractApplicationContext context){
        hijack(context, "beanFactory");
    }

    public static void hijack(AbstractApplicationContext context, String fieldName){
        Field field = ReflectionUtils.findField(context.getClass(), fieldName);
        Assert.notNull(field, "Could not find the BeanFactory field " + fieldName + " on " + context.getClass());
        ReflectionUtils.makeAccessible(field);
        ReflectionUtils.setField(field, context, new TypeHintListableBeanFactory());
    }

    public static class NoTypeHintedBeanDefinitionException extends NoUniqueBeanDefinitionException{

        public NoTypeHintedBeanDefinitionException(Class<?> type, int numberOfBeansFound, String message) {
            super(type, numberOfBeansFound, message);
        }

        public NoTypeHintedBeanDefinitionException(Class<?> type, Collection<String> beanNamesFound) {
            super(type, beanNamesFound);
        }

        public NoTypeHintedBeanDefinitionException(Class<?> type, String... beanNamesFound) {
            super(type, beanNamesFound);
        }
    }
}
