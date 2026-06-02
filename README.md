# CombatPerspective 模组

战斗视角模组模板，支持玩家在第三人称视角下，相机自动朝向最远触及点。

基于 **NeoForge 2.0.141** for Minecraft 1.21.1。

---

## 模组的使用方式

该模组添加了独特的第三人称视角，
能够使方向键直接对应对应方向，
通过鼠标移动来控制人物朝向控制。


想要切换人称视角：

```
windows/Linux:  F5

mac:            Fn+F5
```


添加额外的碰撞体显示，能根据颜色确定能不能交互

## 安装

1. 直接克隆此仓库作为模板，参考 [GitHub 官方教程](https://docs.github.com/en/repositories/creating-and-managing-repositories/creating-a-repository-from-a-template)
2. 使用 `git clone` 克隆到你本地
3. 用 IntelliJ IDEA 或 Eclipse 打开项目

常见问题处理：
```bash
./gradlew --refresh-dependencies  # 刷新依赖库
./gradlew clean                    # 重置构建（不清除代码）
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

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `enableReachCamera` | false | 启用触及相机 |
| `reachDistance` | 6.0 | 最远触及距离 (1-20) |
| `cameraDistance` | 2.0 | 相机与玩家距离 (0-10) |
| `cameraHeight` | 0.7 | 相机高度 (-5 to 5) |
| `cameraSide` | 0.0 | 相机左右偏移 (-5 to 5) |

---

## 映射说明

默认使用 Mojang 官方映射名。方法/字段命名需遵守 [Mojang License](https://github.com/NeoForged/NeoForm/blob/main/Mojang.md)。

---

## 资源链接

- 社区文档：https://docs.neoforged.net/
- NeoForged Discord：https://discord.neoforged.net/