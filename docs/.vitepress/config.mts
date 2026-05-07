import { defineConfig } from 'vitepress'
import { withMermaid } from 'vitepress-plugin-mermaid'
import { sidebar } from './sidebar.mts'

export default withMermaid(
  defineConfig({
    title: 'Java 知识库',
    description: 'Java 开发者的个人知识库 — 系统学习、面试准备、知识复习',
    lang: 'zh-CN',

    // 修复 dayjs ESM default export 问题
    vite: {
      optimizeDeps: {
        include: ['mermaid', 'dayjs'],
      },
      ssr: {
        noExternal: ['mermaid'],
      },
    },

    // 忽略死链接（知识模块文档尚未创建时避免构建失败，后续内容完善后可改为 'localhostLinks'）
    ignoreDeadLinks: true,

    themeConfig: {
      // 顶部导航
      nav: [
        { text: '指南', link: '/guide/getting-started' },
        {
          text: '语言基础',
          items: [
            { text: 'Java 基础', link: '/1-java-core/1.1-java-basics/01-data-types' },
          ],
        },
        {
          text: '语言深入',
          items: [
            { text: '并发编程', link: '/1-java-core/1.3-concurrent/01-thread-lifecycle' },
            { text: 'JVM', link: '/1-java-core/1.4-jvm/01-memory-model' },
            { text: 'Java 进阶', link: '/1-java-core/1.2-java-advanced/01-collections-source' },
            { text: '设计模式', link: '/1-java-core/1.5-design-patterns/01-creational' },
            { text: '算法', link: '/1-java-core/1.6-algorithm/01-linked-list' },
          ],
        },
        {
          text: '框架应用',
          items: [
            { text: '网络与协议', link: '/2-framework/2.1-network/01-tcp-ip' },
            { text: 'Spring Boot', link: '/2-framework/2.2-springboot/01-ioc-di' },
            { text: 'Spring Cloud', link: '/2-framework/2.3-springcloud/01-registry' },
            { text: '数据库', link: '/3-data-store/3.1-database/01-index-theory' },
          ],
        },
        {
          text: '数据存储',
          items: [
            { text: 'Redis', link: '/3-data-store/3.2-redis/01-data-structures' },
            { text: 'Elasticsearch', link: '/3-data-store/3.3-elasticsearch/01-inverted-index' },
            { text: 'MongoDB', link: '/3-data-store/3.4-mongodb/01-document-model' },
            { text: 'MinIO', link: '/3-data-store/3.5-minio/01-architecture' },
          ],
        },
        {
          text: '中间件',
          items: [
            { text: 'RabbitMQ', link: '/4-middleware/4.1-mq-rabbitmq/01-rabbitmq' },
            { text: 'Kafka', link: '/4-middleware/4.2-mq-kafka/01-kafka' },
            { text: 'MQTT', link: '/4-middleware/4.3-mq-mqtt/01-mqtt-protocol' },
            { text: '配置中心', link: '/4-middleware/4.4-config-center/01-apollo' },
            { text: '注册中心', link: '/4-middleware/4.5-registry/01-principles' },
            { text: 'Nginx', link: '/4-middleware/4.6-nginx/01-architecture' },
          ],
        },
        {
          text: '综合进阶',
          items: [
            { text: '分布式系统', link: '/5-distributed/5.1-distributed/01-cap-base' },
            { text: 'Docker 与 K8s', link: '/6-devops/6.1-docker-k8s/01-docker-basics' },
            { text: 'CI/CD', link: '/6-devops/6.2-cicd/01-jenkins' },
            { text: '监控体系', link: '/6-devops/6.3-monitoring/01-prometheus' },
            { text: 'Linux 运维', link: '/6-devops/6.4-linux/01-commands' },
            { text: 'AI 应用', link: '/7-ai/7.1-ai/01-spring-ai' },
            { text: '架构设计', link: '/8-architecture/01-seckill' },
          ],
        },
        {
          text: '学习路径',
          link: '/learning-paths/beginner',
        },
      ],

      // 侧边栏
      sidebar,

      // 本地搜索
      search: {
        provider: 'local',
        options: {
          translations: {
            button: {
              buttonText: '搜索文档',
              buttonAriaLabel: '搜索文档',
            },
            modal: {
              noResultsText: '无法找到相关结果',
              resetButtonTitle: '清除查询条件',
              footer: {
                selectText: '选择',
                navigateText: '切换',
                closeText: '关闭',
              },
            },
          },
        },
      },

      // 社交链接
      socialLinks: [
        { icon: 'github', link: 'https://github.com' },
      ],

      // 页脚
      footer: {
        message: '基于 VitePress 构建的 Java 知识库',
        copyright: 'Copyright © 2024',
      },

      // 文档页脚导航文字
      docFooter: {
        prev: '上一篇',
        next: '下一篇',
      },

      // 大纲标题
      outlineTitle: '本页目录',

      // 最后更新时间
      lastUpdatedText: '最后更新',

      // 返回顶部
      returnToTopLabel: '返回顶部',

      // 侧边栏菜单标签（移动端）
      sidebarMenuLabel: '菜单',

      // 深色模式切换标签
      darkModeSwitchLabel: '主题',
    },

    // Markdown 配置
    markdown: {
      lineNumbers: true,
    },

    // 最后更新时间
    lastUpdated: true,
  }),
)
