package com.github.spring.mq.pulsar.listener;

import com.github.spring.mq.pulsar.annotation.PulsarListener;
import com.github.spring.mq.pulsar.exception.PulsarClientInitException;
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
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pulsar listener annotation processor
 *
 * <p>This is a Spring Bean post-processor responsible for scanning and processing
 * methods annotated with @PulsarListener. Its main responsibilities are:
 * <ul>
 *   <li>Scan all methods in beans after Spring container initialization</li>
 *   <li>Find methods annotated with @PulsarListener</li>
 *   <li>Create corresponding Pulsar consumer containers for each listener method</li>
 *   <li>Start consumer containers to begin listening for messages</li>
 *   <li>Clean up all container resources when application shuts down</li>
 * </ul>
 *
 * <p>Workflow:
 * <pre>
 * 1. Spring container creates Bean instances
 * 2. Call postProcessAfterInitialization() method
 * 3. Scan all methods in Bean, look for @PulsarListener annotations
 * 4. Create PulsarListenerContainer for each listener method
 * 5. Start containers to begin consuming messages
 * 6. Call destroy() method to clean up resources when application shuts down
 * </pre>
 *
 * @author avinzhang
 * @see BeanPostProcessor Spring Bean post-processor interface
 * @see BeanFactoryAware Used to get BeanFactory reference
 * @see DisposableBean Used to clean up resources when Bean is destroyed
 * @see PulsarListener Listener annotation
 * @see PulsarListenerContainer Listener container
 * @since 1.0.0
 */
public class PulsarListenerAnnotationBeanPostProcessor implements BeanPostProcessor, BeanFactoryAware, DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(PulsarListenerAnnotationBeanPostProcessor.class);

    /**
     * Listener container factory used to create PulsarListenerContainer instances
     * Each method annotated with @PulsarListener will create a corresponding container through this factory
     */
    private final PulsarListenerContainerFactory containerFactory;

    /**
     * Store all created listener containers
     * One topic, one consumer, one listener
     */
    private final ConcurrentHashMap<String, PulsarListenerContainer> containers = new ConcurrentHashMap<>();

    /**
     * Spring Bean factory reference
     * Can be used to get other Bean instances (although not currently used in this implementation)
     */
    private BeanFactory beanFactory;

    /**
     * Constructor
     *
     * @param containerFactory Listener container factory used to create listener containers
     */
    public PulsarListenerAnnotationBeanPostProcessor(PulsarListenerContainerFactory containerFactory) {
        this.containerFactory = containerFactory;
    }

    /**
     * Implementation of BeanFactoryAware interface method
     * Spring container will automatically call this method to inject BeanFactory instance
     *
     * @param beanFactory Spring Bean factory
     * @throws BeansException if an exception occurs during the setting process
     */
    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    /**
     * Core method of Bean post-processor
     *
     * <p>Called after each Bean completes initialization, used to scan and process @PulsarListener annotations.
     * This method will:
     * <ol>
     *   <li>Get the Bean's Class object</li>
     *   <li>Iterate through all methods of the Bean (including inherited methods)</li>
     *   <li>Check if each method has @PulsarListener annotation</li>
     *   <li>If annotated, create corresponding listener container</li>
     * </ol>
     *
     * @param bean     Bean instance in Spring container
     * @param beanName Bean name
     * @return Return original Bean instance (no modifications)
     * @throws BeansException if an exception occurs during processing
     */
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        // Get the actual type of Bean (may be proxy class, need to get target class)
        Class<?> targetClass = bean.getClass();

        // Use Spring's reflection utilities to iterate through all methods
        // ReflectionUtils.doWithMethods will iterate through all methods of the class and its parent classes
        ReflectionUtils.doWithMethods(targetClass, method -> {
            // Find @PulsarListener annotation on method
            // AnnotationUtils.findAnnotation will search for annotations on methods, classes, interfaces
            PulsarListener annotation = AnnotationUtils.findAnnotation(method, PulsarListener.class);
            if (annotation != null) {
                // If annotation found, process this listener method
                processListenerMethod(bean, method, annotation);
            }
        });

        // Return original Bean without any modifications
        return bean;
    }

    /**
     * Process single listener method
     *
     * <p>Create and start listener container for methods annotated with @PulsarListener.
     * This method will:
     * <ol>
     *   <li>Create PulsarListenerContainer through container factory</li>
     *   <li>Add container to management list</li>
     *   <li>Start container to begin listening for messages</li>
     *   <li>Log information</li>
     * </ol>
     *
     * @param bean       Bean instance containing listener method
     * @param method     Method annotated with @PulsarListener
     * @param annotation @PulsarListener annotation instance containing configuration information
     * @throws PulsarClientInitException if creating or starting container fails
     */
    private void processListenerMethod(Object bean, Method method, PulsarListener annotation) {
        try {
            PulsarListenerContainer container = containers.get(annotation.topic());
            if (container == null) {
                // Create listener container through factory
                // Container will encapsulate Pulsar Consumer and message processing logic
                container = containerFactory.createContainer(bean, method, annotation);
                if (!containers.contains(container)) {
                    // Add container to management list for subsequent lifecycle management
                    containers.put(annotation.topic(), container);

                    // Start container, begin listening for messages on specified topic
                    container.start();

                    // Log successful listener creation
                    logger.info("Created Pulsar listener for method: {} on topic: {}",
                            method.getName(), annotation.topic());
                }
            } else {
                container.addMethod(bean, method, annotation);
            }
        } catch (Exception e) {
            // Log error and throw runtime exception
            logger.error("Failed to create Pulsar listener for method: " + method.getName(), e);
            throw new PulsarClientInitException("Failed to create Pulsar listener for method: " + method.getName(), e);
        }
    }

    /**
     * Implementation of DisposableBean interface method
     *
     * <p>Called when Spring container shuts down, used to clean up all listener container resources.
     * This method will:
     * <ol>
     *   <li>Iterate through all created listener containers</li>
     *   <li>Call stop() method of each container to stop consumption</li>
     *   <li>Clear container list</li>
     * </ol>
     *
     * <p>This ensures:
     * <ul>
     *   <li>Pulsar Consumer is properly closed</li>
     *   <li>Network connections are released</li>
     *   <li>Thread resources are cleaned up</li>
     *   <li>Resource leaks are avoided</li>
     * </ul>
     */
    @Override
    public void destroy() {
        logger.info("Destroying {} Pulsar listener containers", containers.size());

        // Stop all listener containers
        for (PulsarListenerContainer container : containers.values()) {
            try {
                container.stop();
                logger.debug("Stopped Pulsar listener container: {}", container);
            } catch (Exception e) {
                logger.warn("Failed to stop Pulsar listener container: {}", container, e);
            }
        }

        // Clear container list
        containers.clear();
        logger.info("All Pulsar listener containers have been destroyed");
    }
}