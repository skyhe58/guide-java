package com.example.basics.stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Stream API 核心行为测试
 */
class StreamTest {

    record Employee(String name, String dept, double salary) {}

    @Test
    @DisplayName("filter 过滤符合条件的元素")
    void filterShouldKeepMatchingElements() {
        List<Integer> result = List.of(1, 2, 3, 4, 5).stream()
                .filter(n -> n % 2 == 0)
                .collect(Collectors.toList());
        assertEquals(List.of(2, 4), result);
    }

    @Test
    @DisplayName("map 将元素转换为另一种类型")
    void mapShouldTransformElements() {
        List<String> result = List.of("hello", "world").stream()
                .map(String::toUpperCase)
                .collect(Collectors.toList());
        assertEquals(List.of("HELLO", "WORLD"), result);
    }

    @Test
    @DisplayName("flatMap 展平嵌套集合")
    void flatMapShouldFlattenNestedCollections() {
        List<List<Integer>> nested = List.of(List.of(1, 2), List.of(3, 4), List.of(5));
        List<Integer> flat = nested.stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        assertEquals(List.of(1, 2, 3, 4, 5), flat);
    }

    @Test
    @DisplayName("reduce 归约求和")
    void reduceShouldSumElements() {
        int sum = IntStream.rangeClosed(1, 10).reduce(0, Integer::sum);
        assertEquals(55, sum);
    }

    @Test
    @DisplayName("groupingBy 按部门分组")
    void groupingByShouldGroupByDepartment() {
        List<Employee> employees = List.of(
                new Employee("Alice", "Eng", 8000),
                new Employee("Bob", "Eng", 9000),
                new Employee("Charlie", "Mkt", 7000)
        );

        Map<String, List<Employee>> grouped = employees.stream()
                .collect(Collectors.groupingBy(Employee::dept));

        assertEquals(2, grouped.get("Eng").size());
        assertEquals(1, grouped.get("Mkt").size());
    }

    @Test
    @DisplayName("groupingBy + averagingDouble 计算分组平均值")
    void shouldCalculateAverageSalaryByDept() {
        List<Employee> employees = List.of(
                new Employee("Alice", "Eng", 8000),
                new Employee("Bob", "Eng", 10000),
                new Employee("Charlie", "Mkt", 7000)
        );

        Map<String, Double> avgSalary = employees.stream()
                .collect(Collectors.groupingBy(
                        Employee::dept,
                        Collectors.averagingDouble(Employee::salary)));

        assertEquals(9000.0, avgSalary.get("Eng"));
        assertEquals(7000.0, avgSalary.get("Mkt"));
    }

    @Test
    @DisplayName("partitioningBy 按条件分区")
    void partitioningByShouldSplitIntoTwoGroups() {
        List<Integer> numbers = List.of(1, 2, 3, 4, 5, 6);
        Map<Boolean, List<Integer>> partitioned = numbers.stream()
                .collect(Collectors.partitioningBy(n -> n % 2 == 0));

        assertEquals(List.of(2, 4, 6), partitioned.get(true));
        assertEquals(List.of(1, 3, 5), partitioned.get(false));
    }

    @Test
    @DisplayName("joining 拼接字符串")
    void joiningShouldConcatenateStrings() {
        String result = List.of("a", "b", "c").stream()
                .collect(Collectors.joining(", ", "[", "]"));
        assertEquals("[a, b, c]", result);
    }

    @Test
    @DisplayName("distinct 去重")
    void distinctShouldRemoveDuplicates() {
        List<Integer> result = List.of(1, 2, 2, 3, 3, 3).stream()
                .distinct()
                .collect(Collectors.toList());
        assertEquals(List.of(1, 2, 3), result);
    }

    @Test
    @DisplayName("sorted 排序")
    void sortedShouldOrderElements() {
        List<Integer> result = List.of(3, 1, 4, 1, 5).stream()
                .sorted()
                .distinct()
                .collect(Collectors.toList());
        assertEquals(List.of(1, 3, 4, 5), result);
    }

    @Test
    @DisplayName("toMap 收集为 Map")
    void toMapShouldCreateMap() {
        List<String> words = List.of("hello", "world", "java");
        Map<String, Integer> wordLengths = words.stream()
                .collect(Collectors.toMap(w -> w, String::length));

        assertEquals(5, wordLengths.get("hello"));
        assertEquals(5, wordLengths.get("world"));
        assertEquals(4, wordLengths.get("java"));
    }

    @Test
    @DisplayName("Stream 只能消费一次")
    void streamShouldBeConsumedOnlyOnce() {
        Stream<String> stream = List.of("a", "b").stream();
        stream.count(); // 第一次消费
        assertThrows(IllegalStateException.class, stream::count); // 第二次抛异常
    }

    @Test
    @DisplayName("并行流 collect 结果正确")
    void parallelStreamCollectShouldBeCorrect() {
        List<Integer> numbers = IntStream.rangeClosed(1, 1000).boxed().collect(Collectors.toList());
        List<Integer> result = numbers.parallelStream()
                .filter(n -> n % 2 == 0)
                .collect(Collectors.toList());
        assertEquals(500, result.size());
        assertTrue(result.stream().allMatch(n -> n % 2 == 0));
    }

    @Test
    @DisplayName("自定义 Collector 正确工作")
    void customCollectorShouldWork() {
        Collector<String, StringBuilder, String> joiner = Collector.of(
                StringBuilder::new,
                (sb, s) -> {
                    if (!sb.isEmpty()) sb.append(", ");
                    sb.append(s);
                },
                (sb1, sb2) -> {
                    if (!sb1.isEmpty() && !sb2.isEmpty()) sb1.append(", ");
                    return sb1.append(sb2);
                },
                StringBuilder::toString
        );

        String result = List.of("a", "b", "c").stream().collect(joiner);
        assertEquals("a, b, c", result);
    }
}
