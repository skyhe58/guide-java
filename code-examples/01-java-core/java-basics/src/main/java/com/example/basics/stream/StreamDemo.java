package com.example.basics.stream;

import java.util.*;
import java.util.stream.*;

/**
 * Stream API 深入解析示例
 *
 * 核心知识点：
 * 1. Stream 惰性求值原理
 * 2. 常用中间操作与终端操作
 * 3. 并行流的使用与陷阱
 * 4. 自定义 Collector
 * 5. flatMap 扁平化映射
 *
 * 对应文档：docs/java-basics/lambda-stream.md
 */
public class StreamDemo {

    /** 员工记录（JDK 16+ Record） */
    record Employee(String name, String dept, double salary) {}

    public static void main(String[] args) {
        System.out.println("===== 1. Stream 惰性求值 =====");
        demonstrateLazyEvaluation();

        System.out.println("\n===== 2. 常用操作 =====");
        demonstrateCommonOperations();

        System.out.println("\n===== 3. flatMap 扁平化 =====");
        demonstrateFlatMap();

        System.out.println("\n===== 4. Collectors 收集器 =====");
        demonstrateCollectors();

        System.out.println("\n===== 5. 自定义 Collector =====");
        demonstrateCustomCollector();

        System.out.println("\n===== 6. 并行流 =====");
        demonstrateParallelStream();
    }

    /**
     * 惰性求值：中间操作不会立即执行，只有终端操作触发时才执行
     */
    static void demonstrateLazyEvaluation() {
        List<String> names = List.of("Alice", "Bob", "Charlie", "David", "Eve");

        // 没有终端操作，中间操作不会执行
        Stream<String> stream = names.stream()
                .filter(s -> {
                    System.out.println("  filter: " + s); // 不会打印
                    return s.length() > 3;
                })
                .map(s -> {
                    System.out.println("  map: " + s);    // 不会打印
                    return s.toUpperCase();
                });
        System.out.println("中间操作已定义，但未执行");

        // 终端操作触发执行
        System.out.println("调用终端操作 findFirst():");
        Optional<String> first = names.stream()
                .filter(s -> {
                    System.out.println("  filter: " + s);
                    return s.length() > 3;
                })
                .map(s -> {
                    System.out.println("  map: " + s);
                    return s.toUpperCase();
                })
                .findFirst(); // 找到第一个就停止，不会遍历所有元素

        System.out.println("结果: " + first.orElse("无"));
        // 注意：只处理了 Alice 和 Bob 和 Charlie，找到 Alice 后就停止了
    }

    /**
     * 常用操作演示
     */
    static void demonstrateCommonOperations() {
        List<Employee> employees = List.of(
                new Employee("Alice", "Engineering", 8000),
                new Employee("Bob", "Engineering", 9000),
                new Employee("Charlie", "Marketing", 7000),
                new Employee("David", "Marketing", 7500),
                new Employee("Eve", "Engineering", 10000)
        );

        // filter + map + sorted + collect
        List<String> highPaid = employees.stream()
                .filter(e -> e.salary() > 7500)
                .sorted(Comparator.comparingDouble(Employee::salary).reversed())
                .map(Employee::name)
                .collect(Collectors.toList());
        System.out.println("高薪员工(降序): " + highPaid);

        // max
        employees.stream()
                .max(Comparator.comparingDouble(Employee::salary))
                .ifPresent(e -> System.out.println("最高薪资: " + e));

        // reduce
        double totalSalary = employees.stream()
                .mapToDouble(Employee::salary)
                .sum();
        System.out.println("薪资总和: " + totalSalary);

        // count / anyMatch / allMatch
        long count = employees.stream().filter(e -> "Engineering".equals(e.dept())).count();
        System.out.println("工程部人数: " + count);

        boolean anyHighPaid = employees.stream().anyMatch(e -> e.salary() > 9000);
        System.out.println("是否有人薪资 > 9000: " + anyHighPaid);

        // distinct + limit + skip
        List<Integer> numbers = List.of(1, 2, 2, 3, 3, 4, 5, 5);
        List<Integer> result = numbers.stream()
                .distinct()  // 去重
                .skip(1)     // 跳过第一个
                .limit(3)    // 取 3 个
                .collect(Collectors.toList());
        System.out.println("distinct+skip+limit: " + result);
    }

    /**
     * flatMap：将嵌套结构展平
     */
    static void demonstrateFlatMap() {
        // 嵌套集合展平
        List<List<Integer>> nested = List.of(
                List.of(1, 2, 3),
                List.of(4, 5),
                List.of(6, 7, 8, 9)
        );
        List<Integer> flat = nested.stream()
                .flatMap(Collection::stream) // [[1,2,3],[4,5],[6,7,8,9]] → [1,2,3,4,5,6,7,8,9]
                .collect(Collectors.toList());
        System.out.println("flatMap 展平: " + flat);

        // 实际场景：将句子拆分为单词
        List<String> sentences = List.of("Hello World", "Java Stream API");
        List<String> words = sentences.stream()
                .flatMap(s -> Arrays.stream(s.split(" ")))
                .collect(Collectors.toList());
        System.out.println("句子拆分为单词: " + words);
    }

    /**
     * Collectors 收集器
     */
    static void demonstrateCollectors() {
        List<Employee> employees = List.of(
                new Employee("Alice", "Engineering", 8000),
                new Employee("Bob", "Engineering", 9000),
                new Employee("Charlie", "Marketing", 7000),
                new Employee("David", "Marketing", 7500),
                new Employee("Eve", "Engineering", 10000)
        );

        // groupingBy：按部门分组
        Map<String, List<Employee>> byDept = employees.stream()
                .collect(Collectors.groupingBy(Employee::dept));
        byDept.forEach((dept, emps) ->
                System.out.println(dept + ": " + emps.stream().map(Employee::name).toList()));

        // groupingBy + averagingDouble：按部门计算平均薪资
        Map<String, Double> avgSalary = employees.stream()
                .collect(Collectors.groupingBy(
                        Employee::dept,
                        Collectors.averagingDouble(Employee::salary)));
        System.out.println("部门平均薪资: " + avgSalary);

        // partitioningBy：分区（true/false 两组）
        Map<Boolean, List<Employee>> partitioned = employees.stream()
                .collect(Collectors.partitioningBy(e -> e.salary() > 8000));
        System.out.println("薪资 > 8000: " + partitioned.get(true).stream().map(Employee::name).toList());
        System.out.println("薪资 <= 8000: " + partitioned.get(false).stream().map(Employee::name).toList());

        // joining：字符串拼接
        String names = employees.stream()
                .map(Employee::name)
                .collect(Collectors.joining(", ", "[", "]"));
        System.out.println("员工列表: " + names);

        // summarizingDouble：统计信息
        DoubleSummaryStatistics stats = employees.stream()
                .collect(Collectors.summarizingDouble(Employee::salary));
        System.out.println("薪资统计: count=" + stats.getCount()
                + ", avg=" + stats.getAverage()
                + ", min=" + stats.getMin()
                + ", max=" + stats.getMax());

        // toMap
        Map<String, Double> salaryMap = employees.stream()
                .collect(Collectors.toMap(Employee::name, Employee::salary));
        System.out.println("姓名→薪资: " + salaryMap);
    }

    /**
     * 自定义 Collector
     */
    static void demonstrateCustomCollector() {
        List<String> items = List.of("apple", "banana", "cherry", "date");

        // 自定义 Collector：用逗号连接字符串
        Collector<String, StringBuilder, String> joiner = Collector.of(
                StringBuilder::new,                          // supplier：创建容器
                (sb, s) -> {                                 // accumulator：累加
                    if (!sb.isEmpty()) sb.append(", ");
                    sb.append(s);
                },
                (sb1, sb2) -> {                              // combiner：合并（并行流用）
                    if (!sb1.isEmpty() && !sb2.isEmpty()) sb1.append(", ");
                    return sb1.append(sb2);
                },
                StringBuilder::toString                      // finisher：最终转换
        );

        String result = items.stream().collect(joiner);
        System.out.println("自定义 Collector: " + result);

        // 自定义 Collector：收集到不可变 List
        Collector<String, List<String>, List<String>> toUnmodifiableList = Collector.of(
                ArrayList::new,
                List::add,
                (l1, l2) -> { l1.addAll(l2); return l1; },
                Collections::unmodifiableList
        );

        List<String> immutableList = items.stream()
                .filter(s -> s.length() > 4)
                .collect(toUnmodifiableList);
        System.out.println("不可变列表: " + immutableList);
    }

    /**
     * 并行流的使用与陷阱
     */
    static void demonstrateParallelStream() {
        List<Integer> numbers = IntStream.rangeClosed(1, 100).boxed().collect(Collectors.toList());

        // 并行流求和
        int sum = numbers.parallelStream()
                .mapToInt(Integer::intValue)
                .sum();
        System.out.println("并行流求和: " + sum);

        // ❌ 陷阱：并行流中修改共享变量
        List<Integer> unsafeResult = new ArrayList<>(); // 非线程安全！
        numbers.parallelStream().forEach(unsafeResult::add);
        System.out.println("❌ 非线程安全收集 size: " + unsafeResult.size()
                + " (期望 100，实际可能丢失数据)");

        // ✅ 正确：使用 collect
        List<Integer> safeResult = numbers.parallelStream()
                .collect(Collectors.toList());
        System.out.println("✅ collect 收集 size: " + safeResult.size());

        // forEach 不保证顺序
        System.out.print("parallelStream forEach 顺序: ");
        List.of(1, 2, 3, 4, 5).parallelStream().forEach(n -> System.out.print(n + " "));
        System.out.println();

        // forEachOrdered 保证顺序
        System.out.print("parallelStream forEachOrdered: ");
        List.of(1, 2, 3, 4, 5).parallelStream().forEachOrdered(n -> System.out.print(n + " "));
        System.out.println();

        // 性能提示
        System.out.println("\n并行流使用建议:");
        System.out.println("  ✅ 数据量大（万级以上）+ 计算密集型");
        System.out.println("  ✅ 数据源支持高效分割（ArrayList）");
        System.out.println("  ❌ 数据量小时反而更慢（线程切换开销）");
        System.out.println("  ❌ LinkedList 不适合并行（不支持高效分割）");
        System.out.println("  ❌ 不要在并行流中修改共享变量");
    }
}
