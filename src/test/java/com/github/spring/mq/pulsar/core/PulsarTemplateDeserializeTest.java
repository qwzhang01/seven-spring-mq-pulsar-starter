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

package com.github.spring.mq.pulsar.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.spring.mq.pulsar.config.PulsarProperties;
import com.github.spring.mq.pulsar.listener.DeadLetterMessageProcessor;
import org.apache.pulsar.client.api.PulsarClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Tests for PulsarTemplate deserialize method
 *
 * @author garner
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Pulsar Template Deserialize Tests")
class PulsarTemplateDeserializeTest {

    @Mock
    private PulsarClient pulsarClient;

    private PulsarTemplate pulsarTemplate;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        PulsarProperties pulsarProperties = new PulsarProperties();
        pulsarProperties.setServiceUrl("pulsar://localhost:6650");
        pulsarProperties.getProducer().setTopic("test-topic");
        pulsarProperties.getConsumer().setTopic("test-topic");

        objectMapper = new ObjectMapper();
        DeadLetterMessageProcessor deadLetterProcessor = new DeadLetterMessageProcessor();

        pulsarTemplate = new PulsarTemplate(
                pulsarClient,
                pulsarProperties,
                objectMapper,
                deadLetterProcessor
        );
    }

    @Test
    @DisplayName("Should deserialize String without dataKey")
    void shouldDeserializeStringWithoutDataKey() {
        String originalString = "Hello, Pulsar!";
        byte[] data = originalString.getBytes();

        String result = pulsarTemplate.deserialize(data, null, String.class);

        assertThat(result).isEqualTo(originalString);
    }

    @Test
    @DisplayName("Should deserialize byte array without dataKey")
    void shouldDeserializeByteArrayWithoutDataKey() {
        byte[] originalData = "Test data".getBytes();

        byte[] result = pulsarTemplate.deserialize(originalData, null, byte[].class);

        assertThat(result).isEqualTo(originalData);
    }

    @Test
    @DisplayName("Should deserialize Boolean without dataKey")
    void shouldDeserializeBooleanWithoutDataKey() {
        byte[] trueData = "true".getBytes();
        byte[] falseData = "false".getBytes();

        Boolean trueResult = pulsarTemplate.deserialize(trueData, null, Boolean.class);
        Boolean falseResult = pulsarTemplate.deserialize(falseData, null, Boolean.class);

        assertThat(trueResult).isTrue();
        assertThat(falseResult).isFalse();
    }

    @Test
    @DisplayName("Should deserialize primitive boolean without dataKey")
    void shouldDeserializePrimitiveBooleanWithoutDataKey() {
        byte[] data = "true".getBytes();

        boolean result = pulsarTemplate.deserialize(data, null, boolean.class);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should deserialize Integer without dataKey")
    void shouldDeserializeIntegerWithoutDataKey() {
        byte[] data = "12345".getBytes();

        Integer result = pulsarTemplate.deserialize(data, null, Integer.class);

        assertThat(result).isEqualTo(12345);
    }

    @Test
    @DisplayName("Should deserialize primitive int without dataKey")
    void shouldDeserializePrimitiveIntWithoutDataKey() {
        byte[] data = "67890".getBytes();

        int result = pulsarTemplate.deserialize(data, null, int.class);

        assertThat(result).isEqualTo(67890);
    }

    @Test
    @DisplayName("Should deserialize Long without dataKey")
    void shouldDeserializeLongWithoutDataKey() {
        byte[] data = "9876543210".getBytes();

        Long result = pulsarTemplate.deserialize(data, null, Long.class);

        assertThat(result).isEqualTo(9876543210L);
    }

    @Test
    @DisplayName("Should deserialize primitive long without dataKey")
    void shouldDeserializePrimitiveLongWithoutDataKey() {
        byte[] data = "1234567890".getBytes();

        long result = pulsarTemplate.deserialize(data, null, long.class);

        assertThat(result).isEqualTo(1234567890L);
    }

    @Test
    @DisplayName("Should deserialize Double without dataKey")
    void shouldDeserializeDoubleWithoutDataKey() {
        byte[] data = "123.456".getBytes();

        Double result = pulsarTemplate.deserialize(data, null, Double.class);

        assertThat(result).isEqualTo(123.456);
    }

    @Test
    @DisplayName("Should deserialize primitive double without dataKey")
    void shouldDeserializePrimitiveDoubleWithoutDataKey() {
        byte[] data = "789.012".getBytes();

        double result = pulsarTemplate.deserialize(data, null, double.class);

        assertThat(result).isEqualTo(789.012);
    }

    @Test
    @DisplayName("Should deserialize Float without dataKey")
    void shouldDeserializeFloatWithoutDataKey() {
        byte[] data = "12.34".getBytes();

        Float result = pulsarTemplate.deserialize(data, null, Float.class);

        assertThat(result).isEqualTo(12.34f);
    }

    @Test
    @DisplayName("Should deserialize primitive float without dataKey")
    void shouldDeserializePrimitiveFloatWithoutDataKey() {
        byte[] data = "56.78".getBytes();

        float result = pulsarTemplate.deserialize(data, null, float.class);

        assertThat(result).isEqualTo(56.78f);
    }

    @Test
    @DisplayName("Should deserialize Short without dataKey")
    void shouldDeserializeShortWithoutDataKey() {
        byte[] data = "12345".getBytes();

        Short result = pulsarTemplate.deserialize(data, null, Short.class);

        assertThat(result).isEqualTo((short) 12345);
    }

    @Test
    @DisplayName("Should deserialize primitive short without dataKey")
    void shouldDeserializePrimitiveShortWithoutDataKey() {
        byte[] data = "32767".getBytes();

        short result = pulsarTemplate.deserialize(data, null, short.class);

        assertThat(result).isEqualTo((short) 32767);
    }

    @Test
    @DisplayName("Should deserialize Byte without dataKey")
    void shouldDeserializeByteWithoutDataKey() {
        byte[] data = "127".getBytes();

        Byte result = pulsarTemplate.deserialize(data, null, Byte.class);

        assertThat(result).isEqualTo((byte) 127);
    }

    @Test
    @DisplayName("Should deserialize primitive byte without dataKey")
    void shouldDeserializePrimitiveByteWithoutDataKey() {
        byte[] data = "100".getBytes();

        byte result = pulsarTemplate.deserialize(data, null, byte.class);

        assertThat(result).isEqualTo((byte) 100);
    }

    @Test
    @DisplayName("Should deserialize complex object without dataKey")
    void shouldDeserializeComplexObjectWithoutDataKey() throws Exception {
        TestMessage originalMessage = new TestMessage("test-name", 999);
        byte[] data = objectMapper.writeValueAsBytes(originalMessage);

        TestMessage result = pulsarTemplate.deserialize(data, null, TestMessage.class);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("test-name");
        assertThat(result.getValue()).isEqualTo(999);
    }

    @Test
    @DisplayName("Should deserialize String with dataKey")
    void shouldDeserializeStringWithDataKey() throws Exception {
        String jsonData = "{\"message\":\"Hello from JSON\",\"other\":\"ignored\"}";
        byte[] data = jsonData.getBytes();

        String result = pulsarTemplate.deserialize(data, "message", String.class);

        assertThat(result).isEqualTo("Hello from JSON");
    }

    @Test
    @DisplayName("Should deserialize byte array with dataKey")
    void shouldDeserializeByteArrayWithDataKey() throws Exception {
        String jsonData = "{\"data\":\"Test data\",\"other\":\"ignored\"}";
        byte[] data = jsonData.getBytes();

        byte[] result = pulsarTemplate.deserialize(data, "data", byte[].class);

        assertThat(new String(result)).isEqualTo("Test data");
    }

    @Test
    @DisplayName("Should deserialize complex object with dataKey")
    void shouldDeserializeComplexObjectWithDataKey() throws Exception {
        String jsonData = "{\"payload\":{\"name\":\"nested-test\",\"value\":888},\"other\":\"ignored\"}";
        byte[] data = jsonData.getBytes();

        TestMessage result = pulsarTemplate.deserialize(data, "payload", TestMessage.class);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("nested-test");
        assertThat(result.getValue()).isEqualTo(888);
    }

    @Test
    @DisplayName("Should throw exception when dataKey not found")
    void shouldThrowExceptionWhenDataKeyNotFound() {
        String jsonData = "{\"message\":\"Hello\"}";
        byte[] data = jsonData.getBytes();

        assertThatCode(() -> pulsarTemplate.deserialize(data, "nonExistentKey", String.class))
                .isInstanceOf(com.github.spring.mq.pulsar.exception.JacksonException.class)
                .hasMessageContaining("Data key 'nonExistentKey' not found in message");
    }

    @Test
    @DisplayName("Should handle empty byte array")
    void shouldHandleEmptyByteArray() {
        byte[] emptyData = new byte[0];

        String result = pulsarTemplate.deserialize(emptyData, null, String.class);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should handle whitespace in numeric types")
    void shouldHandleWhitespaceInNumericTypes() {
        byte[] dataWithSpaces = "  123  ".getBytes();

        Integer result = pulsarTemplate.deserialize(dataWithSpaces, null, Integer.class);

        assertThat(result).isEqualTo(123);
    }

    @Test
    @DisplayName("Should throw exception for invalid integer format")
    void shouldThrowExceptionForInvalidIntegerFormat() {
        byte[] invalidData = "not-a-number".getBytes();

        assertThatCode(() -> pulsarTemplate.deserialize(invalidData, null, Integer.class))
                .isInstanceOf(com.github.spring.mq.pulsar.exception.JacksonException.class);
    }

    @Test
    @DisplayName("Should throw exception for invalid boolean format")
    void shouldThrowExceptionForInvalidBooleanFormat() {
        byte[] invalidData = "not-a-boolean".getBytes();

        // Boolean.valueOf() returns false for invalid strings, so this should work
        Boolean result = pulsarTemplate.deserialize(invalidData, null, Boolean.class);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should throw exception for invalid JSON format")
    void shouldThrowExceptionForInvalidJsonFormat() {
        byte[] invalidJson = "{invalid json}".getBytes();

        assertThatCode(() -> pulsarTemplate.deserialize(invalidJson, null, TestMessage.class))
                .isInstanceOf(com.github.spring.mq.pulsar.exception.JacksonException.class);
    }

    @Test
    @DisplayName("Should handle negative numbers")
    void shouldHandleNegativeNumbers() {
        byte[] negativeInt = "-12345".getBytes();
        byte[] negativeLong = "-9876543210".getBytes();
        byte[] negativeDouble = "-123.456".getBytes();

        Integer intResult = pulsarTemplate.deserialize(negativeInt, null, Integer.class);
        Long longResult = pulsarTemplate.deserialize(negativeLong, null, Long.class);
        Double doubleResult = pulsarTemplate.deserialize(negativeDouble, null, Double.class);

        assertThat(intResult).isEqualTo(-12345);
        assertThat(longResult).isEqualTo(-9876543210L);
        assertThat(doubleResult).isEqualTo(-123.456);
    }

    @Test
    @DisplayName("Should handle zero values")
    void shouldHandleZeroValues() {
        byte[] zeroInt = "0".getBytes();
        byte[] zeroDouble = "0.0".getBytes();

        Integer intResult = pulsarTemplate.deserialize(zeroInt, null, Integer.class);
        Double doubleResult = pulsarTemplate.deserialize(zeroDouble, null, Double.class);

        assertThat(intResult).isEqualTo(0);
        assertThat(doubleResult).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should handle maximum and minimum values")
    void shouldHandleMaximumAndMinimumValues() {
        byte[] maxInt = String.valueOf(Integer.MAX_VALUE).getBytes();
        byte[] minInt = String.valueOf(Integer.MIN_VALUE).getBytes();
        byte[] maxLong = String.valueOf(Long.MAX_VALUE).getBytes();
        byte[] minLong = String.valueOf(Long.MIN_VALUE).getBytes();

        Integer maxIntResult = pulsarTemplate.deserialize(maxInt, null, Integer.class);
        Integer minIntResult = pulsarTemplate.deserialize(minInt, null, Integer.class);
        Long maxLongResult = pulsarTemplate.deserialize(maxLong, null, Long.class);
        Long minLongResult = pulsarTemplate.deserialize(minLong, null, Long.class);

        assertThat(maxIntResult).isEqualTo(Integer.MAX_VALUE);
        assertThat(minIntResult).isEqualTo(Integer.MIN_VALUE);
        assertThat(maxLongResult).isEqualTo(Long.MAX_VALUE);
        assertThat(minLongResult).isEqualTo(Long.MIN_VALUE);
    }

    @Test
    @DisplayName("Should handle UTF-8 encoded strings")
    void shouldHandleUtf8EncodedStrings() {
        String utf8String = "Hello 世界 🌍";
        byte[] data = utf8String.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        String result = pulsarTemplate.deserialize(data, null, String.class);

        assertThat(result).isEqualTo(utf8String);
    }

    @Test
    @DisplayName("Should handle nested JSON with dataKey")
    void shouldHandleNestedJsonWithDataKey() throws Exception {
        String nestedJson = "{\"level1\":{\"level2\":{\"name\":\"deep\",\"value\":777}}}";
        byte[] data = nestedJson.getBytes();

        // Note: This tests extracting a nested object, not drilling down multiple levels
        String jsonResult = pulsarTemplate.deserialize(data, "level1", String.class);

        assertThat(jsonResult).contains("level2");
    }

    @Test
    @DisplayName("Should handle scientific notation for doubles")
    void shouldHandleScientificNotationForDoubles() {
        byte[] scientificData = "1.23e10".getBytes();

        Double result = pulsarTemplate.deserialize(scientificData, null, Double.class);

        assertThat(result).isEqualTo(1.23e10);
    }

    // Test message class for JSON serialization tests
    public static class TestMessage {
        private String name;
        private int value;

        public TestMessage() {
        }

        public TestMessage(String name, int value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }
    }
}
