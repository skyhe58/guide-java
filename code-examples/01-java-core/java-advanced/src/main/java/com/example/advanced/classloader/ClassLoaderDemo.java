package com.example.advanced.classloader;

/**
 * Java 类加载器演示 — 双亲委派模型 + 自定义 ClassLoader
 *
 * <p>本示例演示 Java 类加载器的核心机制：
 * <ul>
 *   <li>三层类加载器：Bootstrap → Extension/Platform → Application</li>
 *   <li>双亲委派模型：先委托父加载器，父加载器无法加载时才自己加载</li>
 *   <li>自定义 ClassLoader：重写 findClass 方法</li>
 *   <li>打破双亲委派：重写 loadClass 方法（如 Tomcat、JDBC）</li>
 *   <li>类的唯一性：同一个类被不同 ClassLoader 加载后是不同的 Class 对象</li>
 * </ul>
 *
 * <h3>双亲委派模型：</h3>
 * <pre>
 *  Bootstrap ClassLoader（C++ 实现，加载 rt.jar）
 *       ↑ 委托
 *  Extension/Platform ClassLoader（加载 ext/*.jar）
 *       ↑ 委托
 *  Application ClassLoader（加载 classpath）
 *       ↑ 委托
 *  自定义 ClassLoader
 *
 *  加载流程：
 *  1. 收到加载请求 → 先检查是否已加载
 *  2. 未加载 → 委托给父加载器
 *  3. 父加载器无法加载 → 自己尝试加载（findClass）
 * </pre>
 */
public class ClassLoaderDemo {

    // ==================== 演示方法 ====================

    /** 演示1：三层类加载器 */
    static void demoClassLoaderHierarchy() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示1：三层类加载器层级");
        System.out.println("═══════════════════════════════════════════════════");

        // Application ClassLoader
        ClassLoader appLoader = ClassLoaderDemo.class.getClassLoader();
        System.out.printf("\n  ClassLoaderDemo 的加载器: %s%n", appLoader);

        // Extension/Platform ClassLoader
        ClassLoader extLoader = appLoader.getParent();
        System.out.printf("  父加载器: %s%n", extLoader);

        // Bootstrap ClassLoader（C++ 实现，Java 中返回 null）
        ClassLoader bootstrapLoader = extLoader != null ? extLoader.getParent() : null;
        System.out.printf("  祖父加载器: %s（Bootstrap，C++ 实现，Java 中为 null）%n", bootstrapLoader);

        // 各加载器负责的类
        System.out.println("\n  各加载器加载的类：");
        System.out.printf("    String.class 加载器: %s（Bootstrap）%n", String.class.getClassLoader());
        System.out.printf("    ClassLoaderDemo 加载器: %s（Application）%n",
                ClassLoaderDemo.class.getClassLoader());

        // 验证双亲委派
        System.out.println("\n  双亲委派验证：");
        System.out.println("    尝试用 AppClassLoader 加载 java.lang.String：");
        try {
            Class<?> stringClass = appLoader.loadClass("java.lang.String");
            System.out.printf("    加载成功，实际加载器: %s（委托给了 Bootstrap）%n",
                    stringClass.getClassLoader());
        } catch (ClassNotFoundException e) {
            System.out.println("    加载失败: " + e.getMessage());
        }
        System.out.println();
    }

    /** 演示2：自定义 ClassLoader */
    static void demoCustomClassLoader() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示2：自定义 ClassLoader");
        System.out.println("═══════════════════════════════════════════════════");

        // 自定义 ClassLoader：从字节数组加载类
        ClassLoader customLoader = new ClassLoader(ClassLoaderDemo.class.getClassLoader()) {
            @Override
            protected Class<?> findClass(String name) throws ClassNotFoundException {
                if ("com.example.DynamicClass".equals(name)) {
                    // 动态生成一个简单类的字节码（这里用硬编码的最小类字节码）
                    // 实际场景中可以从网络、数据库、加密文件中加载
                    System.out.println("    [自定义 ClassLoader] findClass 被调用，加载: " + name);
                    // 由于无法在运行时生成有效字节码，这里演示流程
                    throw new ClassNotFoundException("演示用：" + name + "（实际场景从文件/网络加载字节码）");
                }
                return super.findClass(name);
            }
        };

        System.out.println("\n  自定义 ClassLoader 加载流程：");
        try {
            customLoader.loadClass("com.example.DynamicClass");
        } catch (ClassNotFoundException e) {
            System.out.println("    " + e.getMessage());
        }

        System.out.println("\n  自定义 ClassLoader 的实际应用：");
        System.out.println("    1. Tomcat — 每个 Web 应用一个 ClassLoader，实现类隔离");
        System.out.println("    2. OSGi — 模块化类加载");
        System.out.println("    3. 热部署 — 重新加载修改后的类");
        System.out.println("    4. 加密保护 — 加载加密的 class 文件");
        System.out.println();
    }

    /** 演示3：类的唯一性 — 不同 ClassLoader 加载的同一个类是不同的 */
    static void demoClassIdentity() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示3：类的唯一性 — ClassLoader + 全限定名");
        System.out.println("═══════════════════════════════════════════════════");

        System.out.println("\n  类的唯一标识 = ClassLoader + 全限定类名");
        System.out.println("  同一个 .class 文件被不同 ClassLoader 加载后，是不同的 Class 对象");

        // 用同一个 ClassLoader 加载两次 → 同一个 Class 对象
        Class<?> c1 = String.class;
        Class<?> c2 = String.class;
        System.out.printf("\n  同一 ClassLoader 加载两次 String: c1 == c2 → %s%n", c1 == c2);

        // 不同 ClassLoader 的影响
        System.out.println("\n  Tomcat 类加载器隔离示例：");
        System.out.println("    WebApp1 ClassLoader 加载 com.example.User → Class@A");
        System.out.println("    WebApp2 ClassLoader 加载 com.example.User → Class@B");
        System.out.println("    Class@A != Class@B（即使是同一个 .class 文件）");
        System.out.println("    → 两个 Web 应用的类互不干扰");
        System.out.println();
    }

    /** 演示4：打破双亲委派 */
    static void demoBreakDelegation() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示4：打破双亲委派的场景");
        System.out.println("═══════════════════════════════════════════════════");

        System.out.println("\n  【场景1：JDBC — 线程上下文类加载器】");
        System.out.println("    问题：DriverManager 在 rt.jar 中（Bootstrap 加载）");
        System.out.println("          但 MySQL Driver 在 classpath 中（App 加载）");
        System.out.println("          Bootstrap 无法加载 classpath 中的类");
        System.out.println("    解决：Thread.currentThread().setContextClassLoader()");
        System.out.println("          DriverManager 用线程上下文类加载器加载 Driver");

        ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
        System.out.printf("    当前线程上下文类加载器: %s%n", contextLoader);

        System.out.println("\n  【场景2：Tomcat — 自定义类加载顺序】");
        System.out.println("    Tomcat 的 WebAppClassLoader 先自己加载，再委托父加载器");
        System.out.println("    目的：Web 应用的类优先于容器的类（如不同版本的 Spring）");
        System.out.println("    加载顺序：WebApp 自己 → 共享库 → 系统类");

        System.out.println("\n  【场景3：SPI — ServiceLoader】");
        System.out.println("    SPI 接口在 rt.jar 中（Bootstrap 加载）");
        System.out.println("    SPI 实现在 classpath 中（App 加载）");
        System.out.println("    通过线程上下文类加载器打破双亲委派");

        System.out.println("\n  打破双亲委派的方法：");
        System.out.println("    1. 重写 loadClass() 方法（改变委托顺序）");
        System.out.println("    2. 使用线程上下文类加载器（Thread Context ClassLoader）");
        System.out.println();
    }

    // ==================== 主方法 ====================

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║  Java 类加载器演示 — 双亲委派 + 自定义 ClassLoader     ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();

        demoClassLoaderHierarchy();
        demoCustomClassLoader();
        demoClassIdentity();
        demoBreakDelegation();
    }
}
