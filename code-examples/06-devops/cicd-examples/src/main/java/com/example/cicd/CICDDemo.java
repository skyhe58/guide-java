package com.example.cicd;

/**
 * CI/CD 流水线演示 — 用 ProcessBuilder 模拟流水线步骤执行
 *
 * <p>本示例用纯 Java 模拟 CI/CD 流水线的核心概念：
 * <ul>
 *   <li>流水线阶段：编译 → 测试 → 打包 → 部署</li>
 *   <li>步骤执行与状态管理</li>
 *   <li>并行步骤与依赖关系</li>
 *   <li>失败处理与回滚</li>
 *   <li>GitHub Actions / Jenkins 配置对比</li>
 * </ul>
 *
 * <h3>CI/CD 流水线模型：</h3>
 * <pre>
 *  代码提交 → CI（持续集成）                    → CD（持续部署）
 *            ├── 编译（Compile）                ├── 构建镜像（Docker Build）
 *            ├── 单元测试（Unit Test）           ├── 推送镜像（Docker Push）
 *            ├── 代码检查（Lint/SonarQube）      ├── 部署到测试环境
 *            └── 集成测试（Integration Test）     └── 部署到生产环境
 * </pre>
 */
public class CICDDemo {

    // ==================== 流水线模型 ====================

    /** 流水线步骤 */
    static class PipelineStep {
        final String name;
        final java.util.function.Supplier<Boolean> action;
        final long estimatedMs;
        String status = "PENDING"; // PENDING → RUNNING → SUCCESS / FAILED
        long durationMs;

        PipelineStep(String name, java.util.function.Supplier<Boolean> action, long estimatedMs) {
            this.name = name;
            this.action = action;
            this.estimatedMs = estimatedMs;
        }

        boolean execute() {
            status = "RUNNING";
            long start = System.currentTimeMillis();
            System.out.printf("    ▶ [%s] 执行中...%n", name);
            try {
                Thread.sleep(estimatedMs); // 模拟执行耗时
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            boolean success = action.get();
            durationMs = System.currentTimeMillis() - start;
            status = success ? "SUCCESS" : "FAILED";
            String icon = success ? "✓" : "✗";
            System.out.printf("    %s [%s] %s (%dms)%n", icon, name, status, durationMs);
            return success;
        }
    }

    /** 流水线阶段（包含多个步骤） */
    static class PipelineStage {
        final String name;
        final java.util.List<PipelineStep> steps;
        final boolean parallel; // 是否并行执行

        PipelineStage(String name, java.util.List<PipelineStep> steps, boolean parallel) {
            this.name = name;
            this.steps = steps;
            this.parallel = parallel;
        }

        boolean execute() {
            System.out.printf("\n  ═══ Stage: %s %s ═══%n", name, parallel ? "(并行)" : "(串行)");

            if (parallel) {
                // 并行执行
                java.util.concurrent.atomic.AtomicBoolean allSuccess = new java.util.concurrent.atomic.AtomicBoolean(true);
                java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(steps.size());
                for (PipelineStep step : steps) {
                    new Thread(() -> {
                        if (!step.execute()) allSuccess.set(false);
                        latch.countDown();
                    }).start();
                }
                try { latch.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                return allSuccess.get();
            } else {
                // 串行执行
                for (PipelineStep step : steps) {
                    if (!step.execute()) return false;
                }
                return true;
            }
        }
    }

    /** 流水线 */
    static class Pipeline {
        final String name;
        final java.util.List<PipelineStage> stages = new java.util.ArrayList<>();
        long totalDurationMs;

        Pipeline(String name) { this.name = name; }

        void addStage(PipelineStage stage) { stages.add(stage); }

        boolean run() {
            System.out.printf("╔══════════════════════════════════════╗%n");
            System.out.printf("║  Pipeline: %-26s ║%n", name);
            System.out.printf("╚══════════════════════════════════════╝%n");

            long start = System.currentTimeMillis();
            for (PipelineStage stage : stages) {
                if (!stage.execute()) {
                    totalDurationMs = System.currentTimeMillis() - start;
                    System.out.printf("\n  ✗ Pipeline FAILED at stage [%s] (总耗时 %dms)%n", stage.name, totalDurationMs);
                    return false;
                }
            }
            totalDurationMs = System.currentTimeMillis() - start;
            System.out.printf("\n  ✓ Pipeline SUCCESS (总耗时 %dms)%n", totalDurationMs);
            return true;
        }
    }

    // ==================== 演示方法 ====================

    static void demoBasicPipeline() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示1：基本 CI/CD 流水线");
        System.out.println("═══════════════════════════════════════════════════\n");

        Pipeline pipeline = new Pipeline("Java 项目构建");

        // Stage 1: 编译
        pipeline.addStage(new PipelineStage("编译",
                java.util.Arrays.asList(
                        new PipelineStep("mvn compile", () -> true, 100)),
                false));

        // Stage 2: 测试（并行执行单元测试和代码检查）
        pipeline.addStage(new PipelineStage("测试",
                java.util.Arrays.asList(
                        new PipelineStep("mvn test", () -> true, 150),
                        new PipelineStep("sonar-scanner", () -> true, 120)),
                true));

        // Stage 3: 打包
        pipeline.addStage(new PipelineStage("打包",
                java.util.Arrays.asList(
                        new PipelineStep("mvn package -DskipTests", () -> true, 80),
                        new PipelineStep("docker build", () -> true, 100)),
                false));

        // Stage 4: 部署
        pipeline.addStage(new PipelineStage("部署",
                java.util.Arrays.asList(
                        new PipelineStep("docker push", () -> true, 60),
                        new PipelineStep("kubectl apply", () -> true, 50)),
                false));

        pipeline.run();
        System.out.println();
    }

    static void demoFailedPipeline() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示2：流水线失败 — 测试阶段失败");
        System.out.println("═══════════════════════════════════════════════════\n");

        Pipeline pipeline = new Pipeline("失败示例");

        pipeline.addStage(new PipelineStage("编译",
                java.util.Arrays.asList(new PipelineStep("mvn compile", () -> true, 50)),
                false));

        // 测试失败
        pipeline.addStage(new PipelineStage("测试",
                java.util.Arrays.asList(
                        new PipelineStep("mvn test", () -> false, 100), // 测试失败
                        new PipelineStep("sonar-scanner", () -> true, 80)),
                false));

        pipeline.addStage(new PipelineStage("部署",
                java.util.Arrays.asList(new PipelineStep("kubectl apply", () -> true, 50)),
                false));

        pipeline.run();
        System.out.println("  → 测试失败后，后续阶段不会执行");
        System.out.println();
    }

    static void demoGitHubActionsConfig() {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("  演示3：GitHub Actions 配置示例");
        System.out.println("═══════════════════════════════════════════════════");

        System.out.println("\n  .github/workflows/ci.yml：");
        System.out.println("  name: Java CI/CD");
        System.out.println("  on:");
        System.out.println("    push:");
        System.out.println("      branches: [main]");
        System.out.println("    pull_request:");
        System.out.println("      branches: [main]");
        System.out.println("  jobs:");
        System.out.println("    build:");
        System.out.println("      runs-on: ubuntu-latest");
        System.out.println("      steps:");
        System.out.println("        - uses: actions/checkout@v4");
        System.out.println("        - uses: actions/setup-java@v4");
        System.out.println("          with:");
        System.out.println("            java-version: '21'");
        System.out.println("            distribution: 'temurin'");
        System.out.println("        - name: Build & Test");
        System.out.println("          run: mvn clean verify");
        System.out.println("        - name: Docker Build & Push");
        System.out.println("          run: |");
        System.out.println("            docker build -t myapp:${{ github.sha }} .");
        System.out.println("            docker push myapp:${{ github.sha }}");

        System.out.println("\n  CI/CD 最佳实践：");
        System.out.println("    1. 每次提交自动触发 CI（编译+测试）");
        System.out.println("    2. PR 合并前必须通过所有检查");
        System.out.println("    3. 主分支合并后自动部署到测试环境");
        System.out.println("    4. 生产部署需要手动审批");
        System.out.println("    5. 使用缓存加速构建（Maven/Docker Layer）");
        System.out.println();
    }

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║  CI/CD 流水线演示（纯内存模拟）                        ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
        System.out.println();

        demoBasicPipeline();
        demoFailedPipeline();
        demoGitHubActionsConfig();
    }
}
