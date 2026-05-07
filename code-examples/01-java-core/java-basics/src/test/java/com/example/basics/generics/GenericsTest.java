package com.example.basics.generics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 泛型核心行为测试
 */
class GenericsTest {

    @Test
    @DisplayName("泛型类 Pair 正确存储和返回键值对")
    void pairShouldStoreKeyAndValue() {
        var pair = new GenericsDemo.Pair<>("name", 42);
        assertEquals("name", pair.getKey());
        assertEquals(42, pair.getValue());
    }

    @Test
    @DisplayName("有界类型参数 max 返回较大值")
    void maxShouldReturnLargerValue() {
        assertEquals("banana", GenericsDemo.max("apple", "banana"));
        assertEquals(20, GenericsDemo.max(10, 20));
    }

    @Test
    @DisplayName("类型擦除后 List<String> 和 List<Integer> 的 Class 相同")
    void typeErasureShouldProduceSameClass() {
        List<String> stringList = new ArrayList<>();
        List<Integer> intList = new ArrayList<>();
        assertSame(stringList.getClass(), intList.getClass());
    }

    @Test
    @DisplayName("上界通配符 (? extends Number) 可以读取为 Number")
    void upperBoundWildcardShouldAllowReading() {
        List<Integer> intList = Arrays.asList(1, 2, 3);
        double sum = sumOfList(intList);
        assertEquals(6.0, sum);

        List<Double> doubleList = Arrays.asList(1.5, 2.5);
        sum = sumOfList(doubleList);
        assertEquals(4.0, sum);
    }

    @Test
    @DisplayName("下界通配符 (? super Integer) 可以写入 Integer")
    void lowerBoundWildcardShouldAllowWriting() {
        List<Number> numberList = new ArrayList<>();
        addIntegers(numberList);
        assertEquals(3, numberList.size());
        assertEquals(1, numberList.get(0));
    }

    @Test
    @DisplayName("PECS copy 从 extends 读取写入 super")
    void pecsCopyShouldWork() {
        List<Integer> src = Arrays.asList(1, 2, 3);
        List<Number> dest = new ArrayList<>();
        copy(src, dest);
        assertEquals(3, dest.size());
        assertEquals(1, dest.get(0));
        assertEquals(2, dest.get(1));
        assertEquals(3, dest.get(2));
    }

    // Helper methods matching GenericsDemo patterns
    private double sumOfList(List<? extends Number> list) {
        double sum = 0;
        for (Number n : list) {
            sum += n.doubleValue();
        }
        return sum;
    }

    private void addIntegers(List<? super Integer> list) {
        list.add(1);
        list.add(2);
        list.add(3);
    }

    private <T> void copy(List<? extends T> src, List<? super T> dest) {
        for (T item : src) {
            dest.add(item);
        }
    }
}
