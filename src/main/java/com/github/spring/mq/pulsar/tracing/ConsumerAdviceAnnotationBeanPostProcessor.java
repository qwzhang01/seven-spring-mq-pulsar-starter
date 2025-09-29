package com.github.spring.mq.pulsar.tracing;

import com.github.spring.mq.pulsar.annotation.ConsumerExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;


/**
 * 消费者增强注解Bean后处理器
 *
 * @author avinzhang
 */
public class ConsumerAdviceAnnotationBeanPostProcessor implements BeanPostProcessor, BeanFactoryAware, DisposableBean {
    private static final Logger logger = LoggerFactory.getLogger(ConsumerAdviceAnnotationBeanPostProcessor.class);
    private final ConsumeExceptionHandlerContainerFactory exceptionHandlerContainerFactory;
    private BeanFactory beanFactory;

    public ConsumerAdviceAnnotationBeanPostProcessor(ConsumeExceptionHandlerContainerFactory exceptionHandlerContainerFactory) {
        this.exceptionHandlerContainerFactory = exceptionHandlerContainerFactory;
    }


    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return BeanPostProcessor.super.postProcessBeforeInitialization(bean, beanName);
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> targetClass = bean.getClass();

        ReflectionUtils.doWithMethods(targetClass, method -> {
            // Find @PulsarListener annotation on method
            // AnnotationUtils.findAnnotation will search for annotations on methods, classes, interfaces
            ConsumerExceptionHandler annotation = AnnotationUtils.findAnnotation(method, ConsumerExceptionHandler.class);
            if (annotation != null) {
                // If annotation found, process this listener method
                processExceptionMethod(bean, method, annotation);
            }
        });

        return bean;
    }

    private void processExceptionMethod(Object bean, Method method, ConsumerExceptionHandler annotation) {
        exceptionHandlerContainerFactory.create(bean, method, annotation);
    }

    @Override
    public void destroy() throws Exception {

    }
}
