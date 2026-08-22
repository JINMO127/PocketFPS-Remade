# PocketFPS 重制版

**智能性能优化引擎 - 四级降级策略，自动调度**

当 FPS 低于阈值时，自动启用优化，让你在低配电脑上也能流畅游玩。

---

## ✨ 特性

- ⚡ **自动调度**：无需手动操作，根据 FPS 自动升降级
- 🎯 **四级降级策略**：轻度 → 中度 → 重度 → 极限
- 👁️ **HUD 实时显示**：左上角显示当前优化等级和 FPS
- ⚙️ **完全可配置**：`config/pocketfps.json` 调所有阈值
- 🔧 **兼容 Sodium**：与 Sodium 渲染优化协同工作

---

## 📥 下载

[CurseForge]() | [Modrinth]() | [GitHub Releases]()

---

## 🎮 使用说明

1. 安装 Fabric Loader + Fabric API
2. 将 `pocketfps-2.0.0.jar` 放入 `mods/` 文件夹
3. 进游戏，自动生效
4. 按 `F3` 看 FPS，低于阈值时左上角显示优化等级

---

## 📐 四级降级策略

| 等级 | 触发条件 | 优化措施 |
|------|---------|---------|
| 🟢 轻度 | FPS < 40 | 视距调整、红石限制 64 格 |
| 🟡 中度 | FPS < 25 | 实体冻结 32 格、区块节流 3tick |
| 🔴 重度 | FPS < 18 | 帧预测、实体冻结 16 格、区块节流 5tick |

---

## ⚙️ 配置

首次运行后生成 `config/pocketfps.json`：

```json
{
  "lightFpsThreshold": 40,
  "mediumFpsThreshold": 25,
  "heavyFpsThreshold": 18,
  "freezeDistanceLight": 48,
  "freezeDistanceMedium": 32,
  "freezeDistanceHeavy": 16,
  "showHUDIndicator": true
}