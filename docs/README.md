# 项目文档参考

本目录包含开发所需的官方文档参考。

## 目录结构

```
docs/
├── README.md                    # 本文档
├── api-reference/             # NeoForge API 参考
│   ├── README.md              # API 索引
│   ├── 1.21.1/               # 稳定版 (匹配项目版本)
│   │   ├── concepts/          # 核心概念
│   │   ├── gui/               # GUI/屏幕
│   │   ├── resources/         # 资源系统
│   │   └── misc/             # 配置、按键等
│   └── latest/               # 最新文档
├── minecraft-wiki/            # Minecraft 官方 Wiki
│   ├── README.md             # Wiki 索引
│   ├── Java_Edition/         # Java Edition 文档 (39 页)
│   └── Related/             # 相关页面
└── neoforge/                 # NeoForge 文档仓库 (Git)
```

## 文档说明

### NeoForge API 参考 (1.21.1)
- **来源**: https://github.com/neoforged/documentation
- **内容**: NeoForge 框架 API、事件、注册表、渲染等
- **版本**: 匹配项目 NeoForge 21.1.228

### Minecraft 官方 Wiki
- **来源**: https://minecraft.wiki
- **内容**: Java Edition 数据格式、命令、数据包、配方等
- **格式**: HTML (可直接浏览器打开)

## 快速链接

### NeoForge 文档
| 类别 | 路径 |
|------|------|
| 入门 | [1.21.1/gettingstarted/](api-reference/1.21.1/gettingstarted/) |
| 事件系统 | [1.21.1/concepts/events.md](api-reference/1.21.1/concepts/events.md) |
| 屏幕/GUI | [1.21.1/gui/screens.md](api-reference/1.21.1/gui/screens.md) |
| 资源配置 | [1.21.1/resources/client/textures.md](api-reference/1.21.1/resources/client/textures.md) |
| 粒子系统 | [1.21.1/resources/client/particles.md](api-reference/1.21.1/resources/client/particles.md) |

### Minecraft Wiki
| 类别 | 路径 |
|------|------|
| 数据包 | [minecraft-wiki/Java_Edition/Data_pack.html](minecraft-wiki/Java_Edition/Data_pack.html) |
| 资源包 | [minecraft-wiki/Java_Edition/Resource_pack.html](minecraft-wiki/Java_Edition/Resource_pack.html) |
| 命令参考 | [minecraft-wiki/Java_Edition/Commands.html](minecraft-wiki/Java_Edition/Commands.html) |
| 实体格式 | [minecraft-wiki/Java_Edition/Entity_format.html](minecraft-wiki/Java_Edition/Entity_format.html) |
| 战利品表 | [minecraft-wiki/Java_Edition/Loot_table.html](minecraft-wiki/Java_Edition/Loot_table.html) |

## 更新文档

```bash
# 更新 NeoForge 文档
cd docs/neoforge
git pull
# 然后重新复制到 api-reference/

# Minecraft Wiki 需要重新下载页面
# 暂无自动更新脚本
```
