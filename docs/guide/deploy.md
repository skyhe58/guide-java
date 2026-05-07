---
title: "部署指南"
---

# 部署指南

## GitHub Pages 自动部署

项目已配置 GitHub Actions，推送代码到 `main` 分支后自动构建并部署到 GitHub Pages。

### 配置步骤

1. 在 GitHub 仓库 Settings → Pages → Source 选择 `GitHub Actions`
2. 推送代码到 `main` 分支
3. GitHub Actions 自动执行 `.github/workflows/deploy.yml`
4. 部署完成后访问 `https://<username>.github.io/<repo-name>/`

### 自定义域名

1. 在 GitHub 仓库 Settings → Pages → Custom domain 填入域名
2. DNS 添加 CNAME 记录指向 `<username>.github.io`
3. 在 `docs/public/` 下创建 `CNAME` 文件，内容为你的域名

### 构建流程

```yaml
# .github/workflows/deploy.yml 核心步骤
- pnpm install          # 安装依赖
- pnpm run build        # 构建 VitePress 站点
- 部署到 GitHub Pages    # 上传 dist 目录
```

## 手动部署到其他平台

### Vercel

1. 导入 GitHub 仓库
2. Framework Preset 选择 `VitePress`
3. Root Directory 设置为 `docs`
4. Build Command: `pnpm run build`
5. Output Directory: `.vitepress/dist`

### Netlify

1. 导入 GitHub 仓库
2. Base directory: `docs`
3. Build command: `pnpm run build`
4. Publish directory: `docs/.vitepress/dist`

### 自建服务器（Nginx）

```bash
# 1. 本地构建
cd docs
pnpm run build

# 2. 上传到服务器
scp -r .vitepress/dist/* user@server:/var/www/java-kb/

# 3. Nginx 配置
server {
    listen 80;
    server_name your-domain.com;
    root /var/www/java-kb;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

## 代码链接说明

文档中的代码示例链接（如 `RegistryController.java`）指向 GitHub 仓库的源码文件：
- **GitHub Pages 上**：点击可跳转到 GitHub 仓库查看源码
- **本地预览时**：链接会 404，这是正常的，直接在 IDE 中打开对应文件即可

## 本地预览

```bash
cd docs
pnpm install
pnpm run dev        # http://localhost:5173
```

构建后预览：

```bash
cd docs
pnpm run build
pnpm run preview    # http://localhost:4173
```
