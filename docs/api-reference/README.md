# NeoForge API 参考文档

本目录包含 Minecraft NeoForge 1.21.1 官方文档，方便离线查阅。

## 目录结构

```
docs/api-reference/
├── README.md              # 本文档
├── 1.21.1/                # 稳定版文档 (匹配项目版本)
│   ├── gettingstarted/    # 新手入门
│   ├── concepts/          # 核心概念 (Events, Registries, Sides)
│   ├── blocks/            # 方块开发
│   ├── items/             # 物品开发
│   ├── entities/          # 实体开发
│   ├── gui/               # GUI/屏幕
│   ├── networking/        # 网络通信
│   ├── resources/         # 资源系统
│   ├── worldgen/          # 世界生成
│   └── misc/              # 其他功能
└── latest/               # 最新文档 (可能包含新 API)
```

## 版本对应

| 项目 NeoForge 版本 | 文档版本 |
|--------------------|----------|
| NeoForge 21.1.228 | 1.21.1 |

## 关键文档索引 (1.21.1)

### 入门
- [gettingstarted/index.md](1.21.1/gettingstarted/index.md) - 入门指南
- [concepts/sides.md](1.21.1/concepts/sides.md) - 客户端/服务端区分

### 核心概念
- [concepts/events.md](1.21.1/concepts/events.md) - 事件系统
- [concepts/registries.md](1.21.1/concepts/registries.md) - 注册表

### 客户端渲染
- [gui/screens.md](1.21.1/gui/screens.md) - 屏幕/UI 开发
- [resources/client/textures.md](1.21.1/resources/client/textures.md) - 纹理
- [resources/client/particles.md](1.21.1/resources/client/particles.md) - 粒子系统

### 配置与输入
- [misc/config.md](1.21.1/misc/config.md) - 配置文件
- [misc/keymappings.md](1.21.1/misc/keymappings.md) - 按键映射

### 网络
- [networking/index.md](1.21.1/networking/index.md) - 网络通信

## 在线文档

- 官方文档: https://docs.neoforged.net/
- GitHub 仓库: https://github.com/neoforged/documentation

## 更新文档

```bash
cd docs/neoforge
git pull origin main
# 然后重新复制文件到 docs/api-reference/
```

## 相关资源

- [NeoForge Maven](https://maven.neoforged.net/)
- [Parchment 映射](https://parchmentmc.org/)
- [Mixin 文档](https://github.com/SpongePowered/Mixin)
