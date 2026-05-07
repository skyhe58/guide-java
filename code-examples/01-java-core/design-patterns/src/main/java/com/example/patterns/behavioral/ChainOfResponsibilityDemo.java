package com.example.patterns.behavioral;

import java.util.ArrayList;
import java.util.List;

/**
 * 责任链模式演示 — 模拟 Filter 链和审批流程
 * <p>
 * 演示两种责任链实现：
 * 1. 审批流程：组长→经理→总监→VP（经典链表方式）
 * 2. 请求过滤器链：模拟 Servlet Filter 链（数组方式）
 * </p>
 */
public class ChainOfResponsibilityDemo {

    public static void main(String[] args) {
        System.out.println("========== 责任链模式演示 ==========\n");

        demonstrateApprovalChain();
        System.out.println();
        demonstrateFilterChain();
    }

    // ==================== 1. 审批流程（链表方式） ====================

    /** 审批请求 */
    record ApprovalRequest(String applicant, double amount, String reason) {}

    /** 审批处理者（抽象） */
    static abstract class Approver {
        protected Approver next;
        protected final String name;
        protected final double limit;

        Approver(String name, double limit) {
            this.name = name;
            this.limit = limit;
        }

        public Approver setNext(Approver next) {
            this.next = next;
            return next; // 支持链式设置
        }

        public void handle(ApprovalRequest request) {
            if (request.amount() <= limit) {
                approve(request);
            } else if (next != null) {
                System.out.printf("  [%s] 金额 %.0f 超出权限(%.0f)，转交上级%n",
                        name, request.amount(), limit);
                next.handle(request);
            } else {
                System.out.printf("  [%s] 金额 %.0f 超出所有审批权限，审批拒绝%n",
                        name, request.amount());
            }
        }

        protected void approve(ApprovalRequest request) {
            System.out.printf("  [%s] 审批通过：%s 申请 %.0f 元（%s）%n",
                    name, request.applicant(), request.amount(), request.reason());
        }
    }

    static class TeamLeader extends Approver {
        TeamLeader() { super("组长", 1000); }
    }

    static class Manager extends Approver {
        Manager() { super("经理", 5000); }
    }

    static class Director extends Approver {
        Director() { super("总监", 20000); }
    }

    static class VP extends Approver {
        VP() { super("VP", 100000); }
    }

    private static void demonstrateApprovalChain() {
        System.out.println("--- 1. 审批流程（链表方式） ---");

        // 构建责任链：组长 → 经理 → 总监 → VP
        Approver leader = new TeamLeader();
        leader.setNext(new Manager())
              .setNext(new Director())
              .setNext(new VP());

        // 不同金额的审批请求
        leader.handle(new ApprovalRequest("张三", 500, "购买办公用品"));
        System.out.println();
        leader.handle(new ApprovalRequest("李四", 3000, "团建费用"));
        System.out.println();
        leader.handle(new ApprovalRequest("王五", 15000, "服务器采购"));
        System.out.println();
        leader.handle(new ApprovalRequest("赵六", 80000, "年度培训预算"));
        System.out.println();
        leader.handle(new ApprovalRequest("钱七", 200000, "超大额申请"));
    }

    // ==================== 2. 过滤器链（数组方式，模拟 Servlet Filter） ====================

    /** HTTP 请求（简化） */
    static class HttpRequest {
        private final String path;
        private final String token;
        private final String ip;

        HttpRequest(String path, String token, String ip) {
            this.path = path;
            this.token = token;
            this.ip = ip;
        }

        public String getPath() { return path; }
        public String getToken() { return token; }
        public String getIp() { return ip; }
    }

    /** HTTP 响应（简化） */
    static class HttpResponse {
        private int status = 200;
        private String body = "OK";

        public void setStatus(int status) { this.status = status; }
        public void setBody(String body) { this.body = body; }
        public int getStatus() { return status; }
        public String getBody() { return body; }
    }

    /** 过滤器接口（类似 javax.servlet.Filter） */
    interface Filter {
        void doFilter(HttpRequest request, HttpResponse response, FilterChain chain);
    }

    /** 过滤器链（类似 javax.servlet.FilterChain） */
    static class FilterChain {
        private final List<Filter> filters = new ArrayList<>();
        private int index = 0;

        public FilterChain addFilter(Filter filter) {
            filters.add(filter);
            return this;
        }

        public void doFilter(HttpRequest request, HttpResponse response) {
            if (index < filters.size()) {
                Filter filter = filters.get(index++);
                filter.doFilter(request, response, this);
            } else {
                // 所有过滤器通过，执行业务逻辑
                System.out.println("  [Servlet] 处理请求: " + request.getPath());
            }
        }

        /** 重置索引（用于下一次请求） */
        public void reset() { index = 0; }
    }

    /** IP 黑名单过滤器 */
    static class IpFilter implements Filter {
        @Override
        public void doFilter(HttpRequest request, HttpResponse response, FilterChain chain) {
            System.out.println("  [IP过滤器] 检查 IP: " + request.getIp());
            if ("192.168.1.100".equals(request.getIp())) {
                response.setStatus(403);
                response.setBody("IP 被封禁");
                System.out.println("  [IP过滤器] ❌ IP 被封禁，请求终止");
                return; // 不调用 chain.doFilter()，中断链
            }
            System.out.println("  [IP过滤器] ✓ IP 通过");
            chain.doFilter(request, response); // 传递给下一个过滤器
        }
    }

    /** 认证过滤器 */
    static class AuthFilter implements Filter {
        @Override
        public void doFilter(HttpRequest request, HttpResponse response, FilterChain chain) {
            System.out.println("  [认证过滤器] 检查 Token: " + request.getToken());
            if (request.getToken() == null || request.getToken().isEmpty()) {
                response.setStatus(401);
                response.setBody("未认证");
                System.out.println("  [认证过滤器] ❌ 未认证，请求终止");
                return;
            }
            System.out.println("  [认证过滤器] ✓ 认证通过");
            chain.doFilter(request, response);
        }
    }

    /** 日志过滤器 */
    static class LogFilter implements Filter {
        @Override
        public void doFilter(HttpRequest request, HttpResponse response, FilterChain chain) {
            long start = System.currentTimeMillis();
            System.out.println("  [日志过滤器] >>> 请求开始: " + request.getPath());
            chain.doFilter(request, response);
            long cost = System.currentTimeMillis() - start;
            System.out.println("  [日志过滤器] <<< 请求结束: 状态=" + response.getStatus()
                    + ", 耗时=" + cost + "ms");
        }
    }

    private static void demonstrateFilterChain() {
        System.out.println("--- 2. 过滤器链（模拟 Servlet Filter） ---");

        FilterChain chain = new FilterChain()
                .addFilter(new LogFilter())
                .addFilter(new IpFilter())
                .addFilter(new AuthFilter());

        // 正常请求
        System.out.println("\n请求 1：正常请求");
        HttpRequest req1 = new HttpRequest("/api/users", "Bearer token123", "10.0.0.1");
        HttpResponse resp1 = new HttpResponse();
        chain.doFilter(req1, resp1);

        // IP 被封禁的请求
        System.out.println("\n请求 2：IP 被封禁");
        chain.reset();
        HttpRequest req2 = new HttpRequest("/api/users", "Bearer token123", "192.168.1.100");
        HttpResponse resp2 = new HttpResponse();
        chain.doFilter(req2, resp2);

        // 未认证的请求
        System.out.println("\n请求 3：未认证");
        chain.reset();
        HttpRequest req3 = new HttpRequest("/api/users", "", "10.0.0.2");
        HttpResponse resp3 = new HttpResponse();
        chain.doFilter(req3, resp3);
    }
}
