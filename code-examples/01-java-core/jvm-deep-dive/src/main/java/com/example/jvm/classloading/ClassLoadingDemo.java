package com.example.jvm.classloading;

/**
 * 类加载过程演示 — 加载五阶段 + 被动引用不触发初始化
 *
 * <p>类的生命周期：加载 → 验证 → 准备 → 解析 → 初始化 → 使用 → 卸载
 * <ul>
 *   <li>加载（Loading）— 读取 .class 字节码，生成 Class 对象</li>
 *   <li>验证（Verification）— 校验字节码格式、语义</li>
 *   <li>准备（Preparation）— 为静态变量分配内存并赋默认值（零值）</li>
 *   <li>解析（Resolution）— 符号引用 → 直接引用</li>
 *   <li>初始化（Initialization）— 执行 &lt;clinit&gt;（静态代码块 + 静态变量赋值）</li>
 * </ul>
 */
public class ClassLoadingDemo {

    // ==================== 用于验证类加载时机的辅助类 ====================

    static class Parent {
        static int parentValue = 100;
        static {
            System.out.println("    [Parent] 静态代码块执行（类初始化）");
        }
    }

    static class Child extends Parent {
        static int childValue = 200;
        static {
            System.out.println("    [Child] 静态代码块执行（类初始化）");
        }
    }

    static class ConstantClass {
        static final String GREETING = "Hello";  // 编译期常量，放入常量池
        static final int RANDOM = new java.util.Random().nextInt(); // 运行时才确定，不是编译期常量
        static {
            System.out.println("    [ConstantClass] 静态代码块执行");
        }
    }

    static class ArrayElementClass {
        static {
            System.out.println("    [ArrayElementClass] 静态代码块执行");
        }
    }

    // ==================== 演示方法 ====================

    /** 演示1：准备阶段 — 静态变量的默认值 vs 初始值 */
    static void demoPreparationPhase() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示1：准备阶段 — 静态变量赋默认值");
        System.out.println("═══════════════════════════════════════════════════");

        System.out.println("\n  准备阶段：为静态变量分配内存并赋默认值（零值）");
        System.out.println("  初始化阶段：执行 <clinit>，赋真正的初始值");
        System.out.println();
        System.out.printf("    %-20s %-15s %-15s%n", "类型", "准备阶段（零值）", "初始化阶段（真值）");
        System.out.println("    " + "─".repeat(50));
        System.out.printf("    %-20s %-15s %-15s%n", "int", "0", "实际赋值");
        System.out.printf("    %-20s %-15s %-15s%n", "long", "0L", "实际赋值");
        System.out.printf("    %-20s %-15s %-15s%n", "boolean", "false", "实际赋值");
        System.out.printf("    %-20s %-15s %-15s%n", "Object 引用", "null", "实际赋值");
        System.out.printf("    %-20s %-15s %-15s%n", "static final 常量", "直接赋值", "（准备阶段就赋值）");

        System.out.println("\n  特殊情况：static final + 编译期常量");
        System.out.println("    static final String S = \"hello\"; → 准备阶段直接赋值（ConstantValue 属性）");
        System.out.println("    static final int N = new Random().nextInt(); → 初始化阶段赋值（非编译期常量）");
        System.out.println();
    }

    /** 演示2：主动引用 — 触发类初始化的 6 种情况 */
    static void demoActiveReference() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示2：主动引用 — 触发类初始化的情况");
        System.out.println("═══════════════════════════════════════════════════");

        System.out.println("\n  触发类初始化的 6 种情况（主动引用）：");
        System.out.println("    1. new 实例化对象");
        System.out.println("    2. 读取/设置静态字段（非 final 编译期常量）");
        System.out.println("    3. 调用静态方法");
        System.out.println("    4. 反射调用（Class.forName）");
        System.out.println("    5. 初始化子类时，先初始化父类");
        System.out.println("    6. main 方法所在的类");

        // 验证：访问子类静态字段 → 先初始化父类再初始化子类
        System.out.println("\n  【验证】访问 Child.childValue → 先初始化 Parent 再初始化 Child：");
        int val = Child.childValue;
        System.out.printf("    Child.childValue = %d%n", val);
        System.out.println();
    }

    /** 演示3：被动引用 — 不触发类初始化 */
    static void demoPassiveReference() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示3：被动引用 — 不触发类初始化");
        System.out.println("═══════════════════════════════════════════════════");

        // 场景1：通过子类引用父类的静态字段 → 只初始化父类
        System.out.println("\n  【场景1】通过子类引用父类静态字段 → 只初始化父类，不初始化子类");
        // 注意：Parent 和 Child 可能已经在上面的演示中初始化了
        // 这里用说明方式展示
        System.out.println("    Child.parentValue → 只触发 Parent 初始化");
        System.out.println("    原因：parentValue 定义在 Parent 中，Child 只是继承");

        // 场景2：创建数组 → 不触发元素类的初始化
        System.out.println("\n  【场景2】创建类的数组 → 不触发该类初始化");
        ArrayElementClass[] array = new ArrayElementClass[10];
        System.out.printf("    new ArrayElementClass[10] → 数组长度=%d%n", array.length);
        System.out.println("    ArrayElementClass 的静态代码块未执行（未初始化）");
        System.out.println("    原因：创建的是数组类型 [L...ArrayElementClass，不是 ArrayElementClass 本身");

        // 场景3：引用编译期常量 → 不触发定义类的初始化
        System.out.println("\n  【场景3】引用编译期常量 → 不触发定义类初始化");
        String greeting = ConstantClass.GREETING;
        System.out.printf("    ConstantClass.GREETING = %s%n", greeting);
        System.out.println("    ConstantClass 的静态代码块未执行");
        System.out.println("    原因：编译期常量在编译时已内联到调用方的常量池中");

        // 场景4：引用非编译期常量 → 触发初始化
        System.out.println("\n  【场景4】引用非编译期常量 → 触发初始化");
        int random = ConstantClass.RANDOM;
        System.out.printf("    ConstantClass.RANDOM = %d%n", random);
        System.out.println("    ConstantClass 的静态代码块执行了（因为 RANDOM 不是编译期常量）");
        System.out.println();
    }

    /** 演示4：clinit 方法的特点 */
    static void demoClinit() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示4：<clinit> 方法的特点");
        System.out.println("═══════════════════════════════════════════════════");

        System.out.println("\n  <clinit> 方法由编译器自动生成，包含：");
        System.out.println("    1. 所有静态变量的赋值语句");
        System.out.println("    2. 所有 static {} 代码块");
        System.out.println("    3. 按源码中出现的顺序合并");

        System.out.println("\n  <clinit> 的特点：");
        System.out.println("    1. 线程安全：JVM 保证 <clinit> 只执行一次（加锁）");
        System.out.println("    2. 父类优先：子类 <clinit> 执行前，父类 <clinit> 一定已执行");
        System.out.println("    3. 非必须：如果类没有静态变量和静态代码块，不会生成 <clinit>");
        System.out.println("    4. 接口的 <clinit>：不需要先执行父接口的 <clinit>");

        // 利用 clinit 线程安全实现单例
        System.out.println("\n  利用 <clinit> 线程安全实现单例（静态内部类方式）：");
        System.out.println("    public class Singleton {");
        System.out.println("        private static class Holder {");
        System.out.println("            static final Singleton INSTANCE = new Singleton();");
        System.out.println("        }");
        System.out.println("        public static Singleton getInstance() {");
        System.out.println("            return Holder.INSTANCE; // 触发 Holder 初始化，线程安全");
        System.out.println("        }");
        System.out.println("    }");
        System.out.println();
    }

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║  类加载过程演示 — 五阶段 + 主动/被动引用               ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();

        demoPreparationPhase();
        demoActiveReference();
        demoPassiveReference();
        demoClinit();
    }
}
