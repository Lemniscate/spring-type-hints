package com.github.lemniscate.spring.typehint.beans;

import com.github.lemniscate.spring.typehint.TypeHintingDependency;
import com.github.lemniscate.spring.typehint.annotation.TypeHints;
import org.springframework.beans.BeansException;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.GenericTypeResolver;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
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
                Class<?> sourceType = descriptor.getField().getDeclaringClass();
                TypeHints hints = descriptor.getField().getAnnotation(TypeHints.class);
                if (hints != null) {
                    Class<?> requiredType = e.getBeanType();


                    Class<?>[] srcHints = null;

                    // load our hints from the supplied static method
                    if(StringUtils.hasText(hints.staticMethodName())) {
                        Method method = ReflectionUtils.findMethod(sourceType, hints.staticMethodName(), Class.class);
                        Assert.notNull(method, "Could not determine the static lookup method " + sourceType.getSimpleName() + "#" + hints.staticMethodName());
                        srcHints = (Class<?>[]) ReflectionUtils.invokeMethod(method, null, requiredType);
                    }

                    // still no hints? grab whatever the annotation had
                    if( srcHints == null || srcHints.length == 0) {
                        srcHints = hints.value();
                    }

                    // TODO blow up if we don't have any hints?


                    Map<String, Object> candidates = new HashMap<>();
                    Map<String, ?> potentials = getBeansOfType(requiredType);
                    for (String name : potentials.keySet()) {
                        Object potential = potentials.get(name);

                        Class<?>[] depHints;
                        if( TypeHintingDependency.class.isAssignableFrom(potential.getClass()) ){
                            depHints = ((TypeHintingDependency) potential).getDependencyTypeHints(requiredType);
                        }else{
                            depHints = GenericTypeResolver.resolveTypeArguments(potential.getClass(), requiredType);
                        }

                        if (Arrays.equals(depHints, srcHints)) {
                            candidates.put(name, potential);
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
