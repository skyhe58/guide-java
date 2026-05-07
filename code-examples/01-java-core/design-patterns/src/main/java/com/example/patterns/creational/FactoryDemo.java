package com.example.patterns.creational;

/**
 * 工厂模式演示 — 简单工厂 / 工厂方法 / 抽象工厂
 * <p>
 * 业务场景：数据库连接创建
 * - 简单工厂：根据类型参数创建不同数据库连接
 * - 工厂方法：每种数据库有自己的连接工厂
 * - 抽象工厂：创建一族数据库相关对象（连接 + 命令）
 * </p>
 */
public class FactoryDemo {

    public static void main(String[] args) {
        System.out.println("========== 工厂模式演示 ==========\n");

        demonstrateSimpleFactory();
        System.out.println();
        demonstrateFactoryMethod();
        System.out.println();
        demonstrateAbstractFactory();
    }

    // ==================== 1. 简单工厂 ====================

    /** 数据库连接接口 */
    interface DbConnection {
        void connect();
        String getType();
    }

    static class MySQLConnection implements DbConnection {
        @Override public void connect() { System.out.println("  连接到 MySQL 数据库"); }
        @Override public String getType() { return "MySQL"; }
    }

    static class PostgreSQLConnection implements DbConnection {
        @Override public void connect() { System.out.println("  连接到 PostgreSQL 数据库"); }
        @Override public String getType() { return "PostgreSQL"; }
    }

    /** 简单工厂：通过参数决定创建哪种连接 */
    static class SimpleConnectionFactory {
        public static DbConnection create(String type) {
            return switch (type.toLowerCase()) {
                case "mysql" -> new MySQLConnection();
                case "postgresql" -> new PostgreSQLConnection();
                default -> throw new IllegalArgumentException("不支持的数据库类型: " + type);
            };
        }
    }

    private static void demonstrateSimpleFactory() {
        System.out.println("--- 1. 简单工厂 ---");
        DbConnection mysql = SimpleConnectionFactory.create("mysql");
        mysql.connect();
        DbConnection pg = SimpleConnectionFactory.create("postgresql");
        pg.connect();
        System.out.println("  缺点：新增数据库类型需要修改工厂代码，违反开闭原则");
    }

    // ==================== 2. 工厂方法 ====================

    /** 工厂接口：每种数据库有自己的工厂 */
    interface ConnectionFactory {
        DbConnection createConnection();
    }

    static class MySQLConnectionFactory implements ConnectionFactory {
        @Override
        public DbConnection createConnection() { return new MySQLConnection(); }
    }

    static class PostgreSQLConnectionFactory implements ConnectionFactory {
        @Override
        public DbConnection createConnection() { return new PostgreSQLConnection(); }
    }

    private static void demonstrateFactoryMethod() {
        System.out.println("--- 2. 工厂方法 ---");
        ConnectionFactory factory1 = new MySQLConnectionFactory();
        DbConnection conn1 = factory1.createConnection();
        conn1.connect();

        ConnectionFactory factory2 = new PostgreSQLConnectionFactory();
        DbConnection conn2 = factory2.createConnection();
        conn2.connect();
        System.out.println("  优点：新增数据库只需新增工厂类，符合开闭原则");
    }

    // ==================== 3. 抽象工厂 ====================

    /** 数据库命令接口 */
    interface DbCommand {
        void execute(String sql);
    }

    static class MySQLCommand implements DbCommand {
        @Override
        public void execute(String sql) { System.out.println("  MySQL 执行: " + sql); }
    }

    static class PostgreSQLCommand implements DbCommand {
        @Override
        public void execute(String sql) { System.out.println("  PostgreSQL 执行: " + sql); }
    }

    /** 抽象工厂：创建一族相关产品（连接 + 命令） */
    interface DatabaseFactory {
        DbConnection createConnection();
        DbCommand createCommand();
    }

    static class MySQLDatabaseFactory implements DatabaseFactory {
        @Override public DbConnection createConnection() { return new MySQLConnection(); }
        @Override public DbCommand createCommand() { return new MySQLCommand(); }
    }

    static class PostgreSQLDatabaseFactory implements DatabaseFactory {
        @Override public DbConnection createConnection() { return new PostgreSQLConnection(); }
        @Override public DbCommand createCommand() { return new PostgreSQLCommand(); }
    }

    private static void demonstrateAbstractFactory() {
        System.out.println("--- 3. 抽象工厂 ---");
        DatabaseFactory factory = new MySQLDatabaseFactory();
        DbConnection conn = factory.createConnection();
        DbCommand cmd = factory.createCommand();
        conn.connect();
        cmd.execute("SELECT * FROM users");
        System.out.println("  优点：保证同一族产品的一致性（MySQL 连接配 MySQL 命令）");
    }
}
