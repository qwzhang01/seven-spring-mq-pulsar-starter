package com.github.spring.mq.pulsar.annotation;

import org.springframework.core.annotation.AliasFor;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.*;

/**
 * Pulsar 事务注解
 * <p>
 * 这是一个组合注解，结合了 Spring 的 @Transactional 注解，
 * 专门用于 Pulsar 事务管理。
 *
 * @author avinzhang
 * @since 1.0.0
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Transactional("pulsarTransactionManager")
public @interface PulsarTransactional {

    /**
     * 事务管理器的名称
     */
    @AliasFor(annotation = Transactional.class, attribute = "transactionManager")
    String transactionManager() default "pulsarTransactionManager";

    /**
     * 事务传播行为
     */
    @AliasFor(annotation = Transactional.class, attribute = "propagation")
    Propagation propagation() default Propagation.REQUIRED;

    /**
     * 事务隔离级别
     */
    @AliasFor(annotation = Transactional.class, attribute = "isolation")
    Isolation isolation() default Isolation.DEFAULT;

    /**
     * 事务超时时间（秒）
     */
    @AliasFor(annotation = Transactional.class, attribute = "timeout")
    int timeout() default -1;

    /**
     * 事务超时时间字符串表示
     */
    @AliasFor(annotation = Transactional.class, attribute = "timeoutString")
    String timeoutString() default "";

    /**
     * 是否只读事务
     */
    @AliasFor(annotation = Transactional.class, attribute = "readOnly")
    boolean readOnly() default false;

    /**
     * 指定哪些异常类型会导致事务回滚
     */
    @AliasFor(annotation = Transactional.class, attribute = "rollbackFor")
    Class<? extends Throwable>[] rollbackFor() default {};

    /**
     * 指定哪些异常类名会导致事务回滚
     */
    @AliasFor(annotation = Transactional.class, attribute = "rollbackForClassName")
    String[] rollbackForClassName() default {};

    /**
     * 指定哪些异常类型不会导致事务回滚
     */
    @AliasFor(annotation = Transactional.class, attribute = "noRollbackFor")
    Class<? extends Throwable>[] noRollbackFor() default {};

    /**
     * 指定哪些异常类名不会导致事务回滚
     */
    @AliasFor(annotation = Transactional.class, attribute = "noRollbackForClassName")
    String[] noRollbackForClassName() default {};

    /**
     * 事务标签，用于标识事务
     */
    @AliasFor(annotation = Transactional.class, attribute = "label")
    String[] label() default {};
}