# Git 提交规范

## 1. 分支建议

```text
main         正式稳定分支，不直接开发，禁止直接 push
feature/*    功能分支
bugfix/*     常规修复分支
hotfix/*     线上或演示前紧急修复分支
docs/*       文档变更分支
```

团队合作时，不直接 push 到 `main`。`main` 是唯一长期稳定分支，应启用分支保护。每次开发前从 `main` 新建短生命周期分支，开发完成后通过 Pull Request 直接合并回 `main`。

日常开发流程：

```text
main -> feature/*、bugfix/*、hotfix/* 或 docs/* -> PR 到 main
```

## 2. 推荐开发流程

```bash
# 1. 从主分支拉最新代码
git checkout main
git pull origin main

# 2. 新建功能分支
git checkout -b feature/login-page

# 3. 开发、提交
git add .
git commit -m "feat: add login page"

# 4. 推送自己的分支
git push origin feature/login-page
```

然后在 GitHub 上创建指向 `main` 的 Pull Request，等待队友 review 和检查通过。确认没问题后，再合并到 `main`。

## 3. 什么时候可以直接 push

直接 push 只适合个人项目或临时实验仓库。本项目的 `main` 分支应保持保护状态，不允许直接 push；即使是 README typo 这类极小文档修改，也建议走短分支和 PR。

如果仓库中仍存在历史 `dev` 分支，应停止作为集成分支使用，并在确认无未合并提交后删除。

## 4. 提交格式

```text
<type>: <description>
```

## 5. type 类型

| 类型     | 说明           |
| -------- | -------------- |
| feat     | 新功能         |
| fix      | 修复 Bug       |
| docs     | 文档           |
| style    | 样式调整       |
| refactor | 重构           |
| test     | 测试           |
| chore    | 构建或工具配置 |

## 6. 示例

```text
feat: 新增航班查询接口
feat: 实现 AI 智能购票助手页面
fix: 修复座位并发锁定问题
docs: 更新部署指南
refactor: 重构订单服务
```
