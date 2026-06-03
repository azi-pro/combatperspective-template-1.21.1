# CombatPerspective 模组

战斗视角模组，为 Minecraft 1.21.1 提供独特的第三人称战斗视角系统。

基于 **NeoForge 2.0.141** for Minecraft 1.21.1。

---

## 功能特性

### 核心功能

| 功能 | 说明 |
|------|------|
| **固定视角相机** | 第三人称后视角时，相机位置由配置参数决定，脱离原版跟随逻辑 |
| **鼠标指向控制** | 玩家朝向跟随鼠标射线命中点，而非鼠标拖拽 |
| **WASD 移动重映射** | 禁用左右移动（W/S 前进后退保持原样），W = 远离相机方向 |
| **疾跑逻辑修改** | 改为「按键方向与视线夹角 < 45°」触发疾跑 |

### 视觉辅助

| 元素 | 说明 |
|------|------|
| **白色十字** | 标记相机射线与世界的交点 |
| **选中框** | 命中方块时显示黄色/红色边框（范围内=黄，超出范围=红） |
| **实体边框** | 命中实体时显示黄色/红色 AABB 线框 |

### 摄像机调整（运行时）

| 按键 | 效果 |
|------|------|
| `LeftAlt` + 鼠标移动 | 调整摄像机角度（yaw/pitch） |
| `LeftAlt` + 滚轮 | 调整摄像机距离 |

### 视角切换

```
Windows/Linux:  F5
Mac:            Fn + F5
```

---

## 安装

1. 直接克隆此仓库作为模板，参考 [GitHub 官方教程](https://docs.github.com/en/repositories/creating-and-managing-repositories/creating-a-repository-from-template)
2. 使用 `git clone` 克隆到本地
3. 用 IntelliJ IDEA 或 Eclipse 打开项目

### 常见问题

```bash
./gradlew --refresh-dependencies  # 刷新依赖库
./gradlew clean                   # 重置构建（不清除代码）
```

## 使用

### 生成 IDE 配置

```bash
./gradlew genIntellijRuns   # 生成 IDEA 运行配置
./gradlew idea              # 生成 IDEA 项目文件
```

### 运行测试

```bash
./gradlew runClient         # 运行客户端测试
./gradlew runServer         # 运行服务端测试
./gradlew runData           # 运行数据生成
```

### 构建发布

```bash
./gradlew build             # 构建 JAR 文件
# 输出位置：build/libs/
```

---

## 配置项

配置文件位置：`config/combatperspective-common.toml`

### 摄像机配置

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `cameraDeltaX` | 6.0 | 摄像机在玩家右方的偏移（负值=左方） |
| `cameraDeltaY` | 6.0 | 摄像机在玩家上方的高度 |
| `cameraDeltaZ` | 6.0 | 摄像机在玩家后方的距离 |

### 触及相机配置

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `enableReachCamera` | false | 启用触及相机（摄像机朝向最远触及点） |
| `reachDistance` | 6.0 | 最远触及距离 (1-20) |

### 模板遗留配置（可忽略）

| 配置项 | 说明 |
|--------|------|
| `logDirtBlock` | 调试用，是否在加载时打印泥土方块信息 |
| `magicNumber` | 模板遗留 |
| `magicNumberIntroduction` | 模板遗留 |
| `items` | 模板遗留 |

---

## 技术架构

### 项目结构

```
src/main/java/com/aaa/combatperspective/
├── CombatPerspective.java          # 模组主类（服务端入口）
├── CombatPerspectiveClient.java     # 客户端入口 + 渲染系统
├── Config.java                     # 配置项定义
├── data/
│   └── CursorStore.java           # Mixin 间数据共享（命中点、摄像机参数）
├── item/
│   ├── ModItems.java               # 物品注册
│   ├── CPTier.java                 # 自定义剑等级（高耐久/效率）
│   └── CPSword.java                # 支持高附魔值的剑实现
└── mixin/                          # Mixin 注入点
    ├── CameraMixin.java             # 相机位置/旋转控制
    ├── LocalPlayerMixin.java        # 玩家朝向 + 疾跑逻辑
    ├── MouseHandlerMixin.java       # 鼠标拦截 + 摄像机调整
    ├── GameRendererMixin.java       # 预留
    ├── LevelRendererMixin.java       # 预留
    ├── GuiMixin.java                # 预留
    └── ...
```

### 核心 Mixin 说明

| Mixin | 注入方法 | 作用 |
|-------|----------|------|
| `CameraMixin` | `Camera.setup()` | 拦截相机设置，使用配置的 delta 值定位相机 |
| `LocalPlayerMixin` | `LocalPlayer.tick()` | 鼠标位置 → 射线 → 玩家看向命中点 |
| `LocalPlayerMixin` | `LocalPlayer.aiStep()` | 覆盖疾跑判定逻辑 |
| `MouseHandlerMixin` | `MouseHandler.turnPlayer()` | 锁定第三人称视角时的鼠标旋转 |
| `MouseHandlerMixin` | `MouseHandler.onMove/onScroll()` | 拦截 LeftAlt+鼠标/滚轮用于摄像机调整 |

### 数据流向

```
鼠标位置 (MouseHandler)
    ↓
LocalPlayerMixin.mouseLook()
    ↓ 射线检测
命中点 (BlockHitResult / EntityHitResult)
    ↓
CursorStore (共享数据)
    ├→ LocalPlayer.lookAt()  (玩家看向目标)
    ├→ CameraMixin           (更新相机朝向)
    └→ CombatPerspectiveClient (渲染选中框)
```

---

## 内置物品

| 物品 ID | 类型 | 说明 |
|---------|------|------|
| `combatperspective:weapon/iron_longsword` | 剑 | 自定义长剑，支持高附魔值 |

---

## 映射说明

默认使用 Mojang 官方映射名。方法/字段命名需遵守 [Mojang License](https://github.com/NeoForged/NeoForm/blob/main/Mojang.md)。

---

## 资源链接

- 社区文档：https://docs.neoforged.net/
- NeoForged Discord：https://discord.neoforged.net/

---

## 许可证

MIT License（参考 `TEMPLATE_LICENSE.txt`）
