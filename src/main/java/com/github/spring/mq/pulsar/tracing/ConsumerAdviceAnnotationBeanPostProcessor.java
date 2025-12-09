/*
 * MIT License
 *
 * Copyright (c) 2024 avinzhang
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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
 * Bean post processor for consumer advice annotations
 *
 * <p>This post processor scans for methods annotated with {@link ConsumerExceptionHandler}
 * and registers them with the exception handler container factory. It processes beans
 * after initialization to discover and configure exception handling methods.
 *
 * @author avinzhang
 * @since 1.0.0
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
