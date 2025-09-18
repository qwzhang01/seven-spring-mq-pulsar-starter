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
 * Pulsar 监听器注解处理器
 *
 * <p>这是一个 Spring Bean 后置处理器，负责扫描和处理带有 @PulsarListener 注解的方法。
 * 它的主要职责是：
 * <ul>
 *   <li>在 Spring 容器初始化 Bean 后，扫描 Bean 中的所有方法</li>
 *   <li>找到带有 @PulsarListener 注解的方法</li>
 *   <li>为每个监听器方法创建对应的 Pulsar 消费者容器</li>
 *   <li>启动消费者容器开始监听消息</li>
 *   <li>在应用关闭时清理所有容器资源</li>
 * </ul>
 *
 * <p>工作流程：
 * <pre>
 * 1. Spring 容器创建 Bean 实例
 * 2. 调用 postProcessAfterInitialization() 方法
 * 3. 扫描 Bean 的所有方法，查找 @PulsarListener 注解
 * 4. 为每个监听器方法创建 PulsarListenerContainer
 * 5. 启动容器开始消费消息
 * 6. 应用关闭时调用 destroy() 方法清理资源
 * </pre>
 *
 * @author avinzhang
 * @see BeanPostProcessor Spring Bean 后置处理器接口
 * @see BeanFactoryAware 用于获取 BeanFactory 引用
 * @see DisposableBean 用于在 Bean 销毁时清理资源
 * @see PulsarListener 监听器注解
 * @see PulsarListenerContainer 监听器容器
 */
public class PulsarListenerAnnotationBeanPostProcessor implements BeanPostProcessor, BeanFactoryAware, DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(PulsarListenerAnnotationBeanPostProcessor.class);

    /**
     * 监听器容器工厂，用于创建 PulsarListenerContainer 实例
     * 每个带有 @PulsarListener 注解的方法都会通过这个工厂创建一个对应的容器
     */
    private final PulsarListenerContainerFactory containerFactory;

    /**
     * 存储所有创建的监听器容器
     * 一个 topic，也即一个 Consume，一个监听器
     */
    private final ConcurrentHashMap<String, PulsarListenerContainer> containers = new ConcurrentHashMap<>();

    /**
     * Spring Bean 工厂引用
     * 可以用于获取其他 Bean 实例（虽然在当前实现中暂未使用）
     */
    private BeanFactory beanFactory;

    /**
     * 构造函数
     *
     * @param containerFactory 监听器容器工厂，用于创建监听器容器
     */
    public PulsarListenerAnnotationBeanPostProcessor(PulsarListenerContainerFactory containerFactory) {
        this.containerFactory = containerFactory;
    }

    /**
     * 实现 BeanFactoryAware 接口的方法
     * Spring 容器会自动调用此方法，注入 BeanFactory 实例
     *
     * @param beanFactory Spring Bean 工厂
     * @throws BeansException 如果设置过程中出现异常
     */
    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    /**
     * Bean 后置处理器的核心方法
     *
     * <p>在每个 Bean 完成初始化后被调用，用于扫描和处理 @PulsarListener 注解。
     * 这个方法会：
     * <ol>
     *   <li>获取 Bean 的 Class 对象</li>
     *   <li>遍历 Bean 的所有方法（包括继承的方法）</li>
     *   <li>检查每个方法是否有 @PulsarListener 注解</li>
     *   <li>如果有注解，则创建对应的监听器容器</li>
     * </ol>
     *
     * @param bean     Spring 容器中的 Bean 实例
     * @param beanName Bean 的名称
     * @return 返回原始的 Bean 实例（不做任何修改）
     * @throws BeansException 如果处理过程中出现异常
     */
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        // 获取 Bean 的实际类型（可能是代理类，需要获取目标类）
        Class<?> targetClass = bean.getClass();

        // 使用 Spring 的反射工具遍历所有方法
        // ReflectionUtils.doWithMethods 会遍历类及其父类的所有方法
        ReflectionUtils.doWithMethods(targetClass, method -> {
            // 查找方法上的 @PulsarListener 注解
            // AnnotationUtils.findAnnotation 会在方法、类、接口上查找注解
            PulsarListener annotation = AnnotationUtils.findAnnotation(method, PulsarListener.class);
            if (annotation != null) {
                // 如果找到注解，处理这个监听器方法
                processListenerMethod(bean, method, annotation);
            }
        });

        // 返回原始 Bean，不做任何修改
        return bean;
    }

    /**
     * 处理单个监听器方法
     *
     * <p>为带有 @PulsarListener 注解的方法创建监听器容器并启动。
     * 这个方法会：
     * <ol>
     *   <li>通过容器工厂创建 PulsarListenerContainer</li>
     *   <li>将容器添加到管理列表中</li>
     *   <li>启动容器开始监听消息</li>
     *   <li>记录日志信息</li>
     * </ol>
     *
     * @param bean       包含监听器方法的 Bean 实例
     * @param method     带有 @PulsarListener 注解的方法
     * @param annotation @PulsarListener 注解实例，包含配置信息
     * @throws PulsarClientInitException 如果创建或启动容器失败
     */
    private void processListenerMethod(Object bean, Method method, PulsarListener annotation) {
        try {
            PulsarListenerContainer container = containers.get(annotation.topic());
            if (container == null) {
                // 通过工厂创建监听器容器
                // 容器会封装 Pulsar Consumer 和消息处理逻辑
                container = containerFactory.createContainer(bean, method, annotation);
                if (!containers.contains(container)) {
                    // 将容器添加到管理列表，用于后续的生命周期管理
                    containers.put(annotation.topic(), container);

                    // 启动容器，开始监听指定 topic 的消息
                    container.start();

                    // 记录成功创建监听器的日志
                    logger.info("Created Pulsar listener for method: {} on topic: {}",
                            method.getName(), annotation.topic());
                }
            } else {
                container.addMethod(bean, method, annotation);
            }
        } catch (Exception e) {
            // 记录错误日志并抛出运行时异常
            logger.error("Failed to create Pulsar listener for method: " + method.getName(), e);
            throw new PulsarClientInitException("Failed to create Pulsar listener for method: " + method.getName(), e);
        }
    }

    /**
     * 实现 DisposableBean 接口的方法
     *
     * <p>在 Spring 容器关闭时被调用，用于清理所有监听器容器资源。
     * 这个方法会：
     * <ol>
     *   <li>遍历所有创建的监听器容器</li>
     *   <li>调用每个容器的 stop() 方法停止消费</li>
     *   <li>清空容器列表</li>
     * </ol>
     *
     * <p>这确保了：
     * <ul>
     *   <li>Pulsar Consumer 被正确关闭</li>
     *   <li>网络连接被释放</li>
     *   <li>线程资源被清理</li>
     *   <li>避免资源泄漏</li>
     * </ul>
     */
    @Override
    public void destroy() {
        logger.info("Destroying {} Pulsar listener containers", containers.size());

        // 停止所有监听器容器
        for (PulsarListenerContainer container : containers.values()) {
            try {
                container.stop();
                logger.debug("Stopped Pulsar listener container: {}", container);
            } catch (Exception e) {
                logger.warn("Failed to stop Pulsar listener container: {}", container, e);
            }
        }

        // 清空容器列表
        containers.clear();
        logger.info("All Pulsar listener containers have been destroyed");
    }
}