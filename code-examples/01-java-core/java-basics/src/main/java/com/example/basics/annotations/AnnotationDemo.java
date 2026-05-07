package com.example.basics.annotations;

import java.lang.annotation.*;
import java.lang.reflect.Method;

/**
 * 自定义注解与注解处理器示例
 *
 * 核心知识点：
 * 1. 元注解：@Target、@Retention、@Documented、@Inherited、@Repeatable
 * 2. 自定义注解的定义与使用
 * 3. 运行时注解处理（通过反射）
 * 4. 注解在框架中的应用模式
 *
 * 对应文档：docs/java-basics/annotations.md
 */
public class AnnotationDemo {

    // ==================== 自定义注解 ====================

    /**
     * 自定义注解：标记需要权限检查的方法
     *
     * @Target: 注解可以用在哪里（METHOD = 方法上）
     * @Retention: 注解保留到什么阶段（RUNTIME = 运行时可通过反射获取）
     * @Documented: 注解会出现在 Javadoc 中
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @interface RequiresPermission {
        String value();                    // 必需属性
        String[] roles() default {};       // 可选属性，默认空数组
        boolean adminOnly() default false; // 可选属性，默认 false
    }

    /**
     * 自定义注解：方法执行计时
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Timed {
        String description() default "";
    }

    /**
     * 可重复注解（JDK 8+）
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Repeatable(Schedules.class) // 指定容器注解
    @interface Schedule {
        String cron();
    }

    /** 容器注解 */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Schedules {
        Schedule[] value();
    }

    // ==================== 使用注解的类 ====================

    static class UserService {

        @RequiresPermission(value = "user:read", roles = {"admin", "user"})
        @Timed(description = "查询用户")
        public String getUser(int id) {
            return "User-" + id;
        }

        @RequiresPermission(value = "user:delete", adminOnly = true)
        @Timed(description = "删除用户")
        public void deleteUser(int id) {
            System.out.println("  删除用户: " + id);
        }

        @Schedule(cron = "0 0 * * *")
        @Schedule(cron = "0 12 * * *") // 可重复注解
        public void syncData() {
            System.out.println("  同步数据");
        }

        public void publicMethod() {
            System.out.println("  无注解的公共方法");
        }
    }

    // ==================== 注解处理器（运行时） ====================

    /**
     * 简单的注解处理器：模拟 Spring AOP 的权限检查
     */
    static class AnnotationProcessor {

        /** 检查方法上的权限注解 */
        static boolean checkPermission(Method method, String currentRole) {
            if (!method.isAnnotationPresent(RequiresPermission.class)) {
                return true; // 没有注解，不需要权限
            }

            RequiresPermission perm = method.getAnnotation(RequiresPermission.class);

            // 检查是否需要 admin 权限
            if (perm.adminOnly() && !"admin".equals(currentRole)) {
                System.out.println("  ❌ 权限不足: 需要 admin 权限，当前角色: " + currentRole);
                return false;
            }

            // 检查角色列表
            if (perm.roles().length > 0) {
                for (String role : perm.roles()) {
                    if (role.equals(currentRole)) {
                        System.out.println("  ✅ 权限通过: " + perm.value() + " (角色: " + currentRole + ")");
                        return true;
                    }
                }
                System.out.println("  ❌ 权限不足: 需要角色 " + java.util.Arrays.toString(perm.roles()));
                return false;
            }

            return true;
        }

        /** 扫描类中所有带 @Timed 注解的方法 */
        static void scanTimedMethods(Class<?> clazz) {
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Timed.class)) {
                    Timed timed = method.getAnnotation(Timed.class);
                    System.out.println("  发现计时方法: " + method.getName()
                            + " (" + timed.description() + ")");
                }
            }
        }

        /** 处理可重复注解 */
        static void scanSchedules(Class<?> clazz) {
            for (Method method : clazz.getDeclaredMethods()) {
                Schedule[] schedules = method.getAnnotationsByType(Schedule.class);
                if (schedules.length > 0) {
                    System.out.println("  方法 " + method.getName() + " 的调度计划:");
                    for (Schedule s : schedules) {
                        System.out.println("    cron: " + s.cron());
                    }
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("===== 1. 注解信息读取 =====");
        demonstrateAnnotationReading();

        System.out.println("\n===== 2. 权限检查处理器 =====");
        demonstratePermissionCheck();

        System.out.println("\n===== 3. 扫描计时注解 =====");
        AnnotationProcessor.scanTimedMethods(UserService.class);

        System.out.println("\n===== 4. 可重复注解 =====");
        AnnotationProcessor.scanSchedules(UserService.class);

        System.out.println("\n===== 5. 元注解说明 =====");
        demonstrateMetaAnnotations();
    }

    static void demonstrateAnnotationReading() throws Exception {
        Method getUser = UserService.class.getMethod("getUser", int.class);

        // 获取所有注解
        Annotation[] annotations = getUser.getAnnotations();
        System.out.println("getUser() 上的注解数量: " + annotations.length);

        // 获取特定注解
        RequiresPermission perm = getUser.getAnnotation(RequiresPermission.class);
        System.out.println("权限: " + perm.value());
        System.out.println("角色: " + java.util.Arrays.toString(perm.roles()));
        System.out.println("仅管理员: " + perm.adminOnly());
    }

    static void demonstratePermissionCheck() throws Exception {
        Method getUser = UserService.class.getMethod("getUser", int.class);
        Method deleteUser = UserService.class.getMethod("deleteUser", int.class);

        System.out.println("--- user 角色访问 getUser ---");
        AnnotationProcessor.checkPermission(getUser, "user");

        System.out.println("--- user 角色访问 deleteUser ---");
        AnnotationProcessor.checkPermission(deleteUser, "user");

        System.out.println("--- admin 角色访问 deleteUser ---");
        AnnotationProcessor.checkPermission(deleteUser, "admin");
    }

    static void demonstrateMetaAnnotations() {
        System.out.println("@Target: 指定注解可以用在哪里（TYPE/METHOD/FIELD/PARAMETER 等）");
        System.out.println("@Retention: 指定注解保留阶段（SOURCE/CLASS/RUNTIME）");
        System.out.println("  SOURCE: 仅源码中，编译后丢弃（如 @Override）");
        System.out.println("  CLASS: 保留到 class 文件，运行时不可获取（默认）");
        System.out.println("  RUNTIME: 运行时可通过反射获取（框架常用）");
        System.out.println("@Documented: 注解出现在 Javadoc 中");
        System.out.println("@Inherited: 子类继承父类的注解（仅对类注解有效）");
        System.out.println("@Repeatable: 允许同一位置重复使用注解（JDK 8+）");
    }
}
