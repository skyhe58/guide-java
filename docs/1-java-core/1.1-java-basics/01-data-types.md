---
title: "数据类型与包装类"
module: "java-basics"
difficulty: "beginner"
interviewFrequency: "high"
tags:
  - "Java 基础"
  - "数据类型"
  - "装箱拆箱"
  - "缓存池"
  - "面试高频"
codeExample: "01-java-core/java-basics/src/main/java/com/example/basics/datatypes/"
relatedEntries:
  - "/1-java-core/1.1-java-basics/02-value-passing"
  - "/1-java-core/1.1-java-basics/03-string-deep-dive"
  - "/1-java-core/1.1-java-basics/05-collections"
prerequisites: []
estimatedTime: "30min"
---

# 数据类型与包装类

## 概念说明

Java 是强类型语言，每个变量都必须声明类型。Java 的数据类型分为两大类：

- **基本类型（Primitive Types）**：直接存储值，存放在栈内存中，共 8 种
- **引用类型（Reference Types）**：存储对象的引用（地址），对象本身在堆内存中

每种基本类型都有对应的包装类（Wrapper Class），Java 5 引入了自动装箱/拆箱机制，使得基本类型和包装类之间可以自动转换。

### 8 种基本类型一览

| 基本类型 | 大小 | 默认值 | 包装类 | 取值范围 |
|---------|------|--------|--------|---------|
| `byte` | 1 字节 | 0 | `Byte` | -128 ~ 127 |
| `short` | 2 字节 | 0 | `Short` | -32768 ~ 32767 |
| `int` | 4 字节 | 0 | `Integer` | -2^31 ~ 2^31-1 |
| `long` | 8 字节 | 0L | `Long` | -2^63 ~ 2^63-1 |
| `float` | 4 字节 | 0.0f | `Float` | IEEE 754 |
| `double` | 8 字节 | 0.0d | `Double` | IEEE 754 |
| `char` | 2 字节 | '\u0000' | `Character` | 0 ~ 65535 |
| `boolean` | 不确定 | false | `Boolean` | true / false |

> ⚠️ `boolean` 的大小在 JVM 规范中没有明确规定。HotSpot 中单独使用时占 4 字节（当作 int 处理），在数组中占 1 字节。

## 核心原理

### 自动装箱与拆箱

**装箱（Boxing）**：基本类型 → 包装类，编译器自动调用 `valueOf()` 方法。

**拆箱（Unboxing）**：包装类 → 基本类型，编译器自动调用 `xxxValue()` 方法。

```java
// 装箱：编译器转换为 Integer.valueOf(10)
Integer a = 10;

// 拆箱：编译器转换为 a.intValue()
int b = a;
```

**底层原理**：通过 `javap -c` 反编译可以看到，装箱调用的是 `Integer.valueOf()`，拆箱调用的是 `Integer.intValue()`。

### 缓存池机制（Cache Pool）

Java 对部分包装类实现了缓存池，在一定范围内的值会复用同一个对象，避免频繁创建对象。

```mermaid
flowchart TD
    A["Integer.valueOf(n)"] --> B{n >= -128 且 n <= 127?}
    B -->|是| C[返回缓存池中的对象]
    B -->|否| D[new Integer(n)]
    C --> E[多次调用返回同一对象]
    D --> F[每次创建新对象]
```

**各包装类缓存范围**：

| 包装类 | 缓存范围 | 是否可配置 |
|--------|---------|-----------|
| `Byte` | -128 ~ 127（全部） | 否 |
| `Short` | -128 ~ 127 | 否 |
| `Integer` | -128 ~ 127（默认） | 是，`-XX:AutoBoxCacheMax` |
| `Long` | -128 ~ 127 | 否 |
| `Character` | 0 ~ 127 | 否 |
| `Boolean` | `TRUE` / `FALSE` | 否 |
| `Float` | 无缓存 | — |
| `Double` | 无缓存 | — |

**Integer 缓存池源码**：

```java
// Integer.valueOf() 源码
public static Integer valueOf(int i) {
    if (i >= IntegerCache.low && i <= IntegerCache.high)
        return IntegerCache.cache[i + (-IntegerCache.low)];
    return new Integer(i);
}

// IntegerCache 内部类
private static class IntegerCache {
    static final int low = -128;
    static final int high; // 默认 127，可通过 JVM 参数调整
    static final Integer[] cache;
    // 静态初始化块中创建缓存数组
}
```

### == 与 equals 的区别

- `==`：比较基本类型的值，或引用类型的内存地址
- `equals()`：比较对象的内容（前提是重写了 equals 方法）

```java
Integer a = 127;
Integer b = 127;
System.out.println(a == b);      // true（缓存池，同一对象）

Integer c = 128;
Integer d = 128;
System.out.println(c == d);      // false（超出缓存范围，不同对象）
System.out.println(c.equals(d)); // true（值相等）
```

### 装箱拆箱的陷阱

**1. NPE（空指针异常）**：

```java
Integer a = null;
int b = a; // NullPointerException！拆箱时调用 a.intValue()
```

**2. 性能问题**：循环中频繁装箱会创建大量对象。

```java
// 反面示例：每次 += 都会触发拆箱和装箱
Long sum = 0L;
for (long i = 0; i < Integer.MAX_VALUE; i++) {
    sum += i; // 拆箱 → 相加 → 装箱，性能极差
}

// 正确做法：使用基本类型
long sum = 0L;
for (long i = 0; i < Integer.MAX_VALUE; i++) {
    sum += i;
}
```

**3. 三目运算符中的拆箱**：

```java
boolean flag = true;
Integer a = 1;
Integer b = null;
// 三目运算符会对两个分支做类型统一，触发拆箱
Integer result = flag ? a * 2 : b; // 如果 flag=false，b 拆箱时 NPE
```

## 代码示例

```java
public class DataTypesDemo {
    public static void main(String[] args) {
        // 1. 缓存池验证
        Integer a = 127, b = 127;
        Integer c = 128, d = 128;
        System.out.println("127 == 127: " + (a == b));   // true
        System.out.println("128 == 128: " + (c == d));   // false

        // 2. 装箱拆箱
        Integer x = 10;           // 自动装箱：Integer.valueOf(10)
        int y = x;                // 自动拆箱：x.intValue()

        // 3. new Integer vs valueOf
        Integer m = new Integer(127);  // 始终创建新对象（已废弃）
        Integer n = Integer.valueOf(127); // 使用缓存池
        System.out.println(m == n);    // false
    }
}
```

> 💻 完整可运行代码：[code-examples/01-java-core/java-basics/src/main/java/com/example/basics/datatypes/](https://github.com/skyhe58/guide-java/tree/main/code-examples/01-java-core/java-basics/src/main/java/com/example/basics/datatypes/)
> <!-- 本地路径：code-examples/01-java-core/java-basics/src/main/java/com/example/basics/datatypes/ -->

## 常见面试题

### Q1: Integer 缓存池的范围是多少？为什么要有缓存池？

**难度**：⭐⭐ | **频率**：🔥🔥🔥

**答题思路**：

1. 先说默认范围：-128 ~ 127
2. 再说可配置：`-XX:AutoBoxCacheMax` 可以调整上限
3. 解释为什么：小整数使用频率高，缓存可以减少对象创建，节省内存
4. 提到其他包装类的缓存情况

**标准答案**：

Integer 缓存池默认范围是 -128 到 127。当调用 `Integer.valueOf(int)` 时，如果值在此范围内，会直接返回缓存池中的对象，而不是创建新对象。这是因为小整数在程序中使用非常频繁（如循环计数器、数组索引等），缓存可以显著减少对象创建次数，降低 GC 压力。上限可以通过 JVM 参数 `-XX:AutoBoxCacheMax=<size>` 调整。Byte、Short、Long 也有 -128~127 的缓存，Character 缓存 0~127，Float 和 Double 没有缓存。

**深入追问**：

- `new Integer(127)` 和 `Integer.valueOf(127)` 有什么区别？
- 为什么 Float 和 Double 没有缓存池？（浮点数在小范围内的值不像整数那样有限且常用）
- 如何验证缓存池的存在？（用 `==` 比较两个相同值的 Integer 对象）

**易错点**：

- 误以为所有 Integer 对象都会被缓存
- 忘记 `new Integer()` 不走缓存池（Java 9+ 已废弃该构造方法）
- 混淆 `==` 和 `equals()` 在包装类上的行为

### Q2: 自动装箱和拆箱的原理是什么？有哪些陷阱？

**难度**：⭐⭐ | **频率**：🔥🔥🔥

**答题思路**：

1. 说明装箱调用 `valueOf()`，拆箱调用 `xxxValue()`
2. 列举常见陷阱：NPE、性能问题、三目运算符
3. 给出最佳实践

**标准答案**：

自动装箱是编译器在基本类型赋值给包装类时，自动插入 `valueOf()` 调用；自动拆箱是包装类赋值给基本类型时，自动插入 `xxxValue()` 调用。常见陷阱包括：（1）包装类为 null 时拆箱会抛 NPE；（2）循环中频繁装箱导致大量临时对象，影响性能；（3）三目运算符中两个分支类型不一致时会触发隐式拆箱。最佳实践是：局部变量优先使用基本类型，集合元素必须用包装类，比较包装类对象用 `equals()` 而非 `==`。

**深入追问**：

- 为什么集合中不能使用基本类型？（泛型类型擦除后是 Object，基本类型不是 Object 的子类）
- `Integer a = 1; Long b = 1L; a.equals(b)` 的结果是什么？（false，类型不同）

**易错点**：

- 忘记 null 拆箱会 NPE
- 在循环中使用包装类累加

### Q3: 基本类型和包装类的区别？什么时候用哪个？

**难度**：⭐ | **频率**：🔥🔥

**答题思路**：

1. 从存储位置、默认值、是否可为 null、性能四个维度对比
2. 给出使用场景建议

**标准答案**：

基本类型存储在栈上（局部变量）或对象内部（成员变量），有默认值（如 int 默认 0），不能为 null，性能更好。包装类是对象，存储在堆上，默认值为 null，可以用于泛型。使用建议：方法内的局部变量优先用基本类型；POJO 类的属性建议用包装类（因为数据库字段可能为 null，用基本类型会有默认值误导）；集合的泛型参数必须用包装类。

**深入追问**：

- 为什么阿里巴巴开发手册建议 POJO 属性用包装类？
- `int` 和 `Integer` 作为 HashMap 的 key 有什么区别？

**易错点**：

- 误以为基本类型一定在栈上（成员变量随对象在堆上）
- 忘记包装类的 `equals()` 会先判断类型

## 参考资料

- [Java Language Specification - Primitive Types](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.2)
- [Java Language Specification - Boxing Conversion](https://docs.oracle.com/javase/specs/jls/se21/html/jls-5.html#jls-5.1.7)
- [Integer.valueOf() 源码](https://github.com/openjdk/jdk/blob/master/src/java.base/share/classes/java/lang/Integer.java)
