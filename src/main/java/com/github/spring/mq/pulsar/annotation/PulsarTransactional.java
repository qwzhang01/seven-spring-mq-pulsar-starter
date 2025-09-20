package com.github.spring.mq.pulsar.annotation;

import org.springframework.core.annotation.AliasFor;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.*;

/**
 * Pulsar transaction annotation
 * 
 * <p>This is a composite annotation that combines Spring's @Transactional annotation,
 * specifically designed for Pulsar transaction management. It provides a convenient
 * way to enable transactional behavior for Pulsar message operations.
 * 
 * <p>Features:
 * <ul>
 *   <li>Automatic transaction management for Pulsar operations</li>
 *   <li>Integration with Spring's transaction infrastructure</li>
 *   <li>Configurable transaction behavior and isolation levels</li>
 *   <li>Support for rollback conditions and timeout settings</li>
 * </ul>
 * 
 * <p>Usage example:
 * <pre>
 * {@code
 * @PulsarTransactional
 * public void processMessage(MyMessage message) {
 *     // Transactional message processing
 *     pulsarTemplate.send("output-topic", processedMessage);
 * }
 * }
 * </pre>
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
     * Transaction manager name
     */
    @AliasFor(annotation = Transactional.class, attribute = "transactionManager")
    String transactionManager() default "pulsarTransactionManager";

    /**
     * Transaction propagation behavior
     */
    @AliasFor(annotation = Transactional.class, attribute = "propagation")
    Propagation propagation() default Propagation.REQUIRED;

    /**
     * Transaction isolation level
     */
    @AliasFor(annotation = Transactional.class, attribute = "isolation")
    Isolation isolation() default Isolation.DEFAULT;

    /**
     * Transaction timeout in seconds
     */
    @AliasFor(annotation = Transactional.class, attribute = "timeout")
    int timeout() default -1;

    /**
     * Transaction timeout string representation
     */
    @AliasFor(annotation = Transactional.class, attribute = "timeoutString")
    String timeoutString() default "";

    /**
     * Whether this is a read-only transaction
     */
    @AliasFor(annotation = Transactional.class, attribute = "readOnly")
    boolean readOnly() default false;

    /**
     * Exception types that will cause transaction rollback
     */
    @AliasFor(annotation = Transactional.class, attribute = "rollbackFor")
    Class<? extends Throwable>[] rollbackFor() default {};

    /**
     * Exception class names that will cause transaction rollback
     */
    @AliasFor(annotation = Transactional.class, attribute = "rollbackForClassName")
    String[] rollbackForClassName() default {};

    /**
     * Exception types that will NOT cause transaction rollback
     */
    @AliasFor(annotation = Transactional.class, attribute = "noRollbackFor")
    Class<? extends Throwable>[] noRollbackFor() default {};

    /**
     * Exception class names that will NOT cause transaction rollback
     */
    @AliasFor(annotation = Transactional.class, attribute = "noRollbackForClassName")
    String[] noRollbackForClassName() default {};

    /**
     * Transaction labels for transaction identification
     */
    @AliasFor(annotation = Transactional.class, attribute = "label")
    String[] label() default {};
}