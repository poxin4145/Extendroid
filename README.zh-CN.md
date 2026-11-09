# Extendroid 中文说明

[English](README.md) | **简体中文**

Extendroid 是一个 Android 多任务管理工具：它把任意已安装的应用放进可自由移动、缩放的浮窗里，
并在控制面板中以「实时预览」的方式管理这些窗口——相当于把桌面系统的多窗口体验搬到手机上。

> 本项目是 [legendsayantan/Extendroid](https://github.com/legendsayantan/Extendroid) 的社区分支，
> 在原版基础上加入了**可选中文界面**、**离线可编译**和**自动发布流程**。

---

## 一、这个分支多了什么

| 改进 | 说明 |
| --- | --- |
| 可选中文界面 | 默认跟随系统语言；也可以在「选项 → 语言」中手动切换「跟随系统 / 简体中文 / English」，切换后浮窗菜单立即生效 |
| 应用内使用说明 | 首页「选项 → 使用说明」内置快速上手指南，随语言切换 |
| 无 Firebase 也能编译运行 | 未提供 `google-services.json` 的构建会自动隐藏 Echo（远程访问）入口，不会闪退 |
| 开箱即可编译 | 补齐了上游需要手动创建的资源，克隆后直接 `./gradlew assembleRelease` |
| 自动发布 | 推送 `v*` 标签后，GitHub Actions 自动构建签名 APK 并发布到 Releases |

---

## 二、运行要求

- Android 10（API 29）或更高版本
- [Shizuku](https://shizuku.rikka.app/) 已运行（通过 ADB 或 root 启动）
- 「显示在其他应用上层」权限
- 小米 / Redmi 设备还需要在系统设置中额外开启「在其他应用上层显示」的 MIUI 权限

> Shizuku 的作用是让应用获得「ADB 级」权限，用来创建虚拟屏幕、跨应用启动界面、注入触摸事件。
> 它不会 root 你的手机。

---

## 三、安装

1. 打开 [Releases](../../releases) 页面，下载最新的 `Extendroid-<版本号>.apk`。
2. 点击安装，如果系统提示，请允许「安装未知来源应用」。
3. 从旧版本升级时若提示「签名不一致」，需要先卸载旧版本再安装（应用内设置会清空）。

---

## 四、首次配置

1. **启动 Shizuku**
   - 已 root：在 Shizuku 应用内直接点击「启动」。
   - 未 root：按 Shizuku 的提示，用电脑执行 `adb shell sh /sdcard/Android/data/moe.shizuku.privileged.api/start.sh`，
     或使用支持无线调试的「通过无线调试启动」。
2. **打开 Extendroid**，首页有两张卡片：
   - **配置**：点击「配置 Shizuku」，如果 Shizuku 已运行，右侧图标会变成对勾；
     点击「权限」，在 Shizuku 的授权弹窗中选择「允许」。
   - **选项**：Shizuku 配置完成后才会解锁。
3. **授予悬浮窗权限**：首次打开菜单时，系统会要求「显示在其他应用上层」，请允许。

两项配置都出现对勾后，Extendroid 会在后台启动前台服务（通知栏会出现常驻通知）。

---

## 五、日常使用

### 打开控制面板

两种方式，任选其一：

- **通知栏**：点击「点击打开 Extendroid 菜单」这条通知。
- **悬浮球**：在「选项」中打开「悬浮球」开关，屏幕上会出现一个圆形图标，点击即可打开菜单。

### 控制面板结构

- 顶部三个标签：**☆**（最近 / 常用）、**＋**（全部应用）、**预览**。
- 右上角三个按钮：**⚙ 设置**、**⋮ 更多**、**✕ 关闭**。

### 选择并打开应用

1. 在 **☆** 或 **＋** 标签中选择一个应用。
2. 自动切换到 **预览** 标签，显示该应用的实时画面。
3. **点击预览画面** → 应用以浮窗形式打开。
4. **长按预览画面** → 应用全屏打开。
5. 预览画面右下角的 **◇** 按钮：点击后可以直接在预览里操作应用；长按则关闭该应用。

### 浮窗手势

| 操作 | 效果 |
| --- | --- |
| 拖动顶部把手 | 移动窗口 |
| 点击顶部把手 | 展开 / 收起工具栏：最小化、最大化、返回、关闭 |
| 拖动底部两角 | 自由调整窗口大小与宽高比 |
| 长按窗口 | 全屏 |

### 其它功能

- **最小化**：窗口回到预览标签，应用继续在后台运行，预览画面保持实时更新。
- **关闭屏幕**：在预览标签下打开 **⋮** 菜单 → 「关闭屏幕」，屏幕熄灭但所有应用继续运行。
  再次点亮屏幕即可继续使用。
- **更多菜单**：全部浮窗打开、全部全屏、全部关闭、配置、新建工作区。
- **工作区**：把一组应用窗口保存成一个独立任务，在最近任务中作为一个整体出现。
- **日志**：首页底部的日志按钮可以查看运行日志，支持按天数清理、升序/降序排列。

---

## 六、语言设置

- **跟随系统（默认）**：系统语言为中文时，界面自动显示简体中文。
- 手动切换：首页 →「选项」→「语言」→ 选择「跟随系统 / 简体中文 / English」。
- 切换后主界面会立即重启生效，悬浮菜单会在下次打开时使用新语言。

---

## 七、Echo（远程访问，可选）

Echo 允许你从浏览器远程访问手机上的应用，它依赖 Firebase 和上游的服务器，**默认不可用**：

- 本分支的构建如果没有 `google-services.json`，会自动隐藏 Echo 入口。
- 想启用 Echo，需要：
  1. 放入自己的 `app/google-services.json`（存在该文件时 Gradle 会自动应用 Google Services 插件）；
  2. 用自己的后端地址替换 `app/src/main/res/values/strings_echo_backend.xml`
     （或删除它并新建 `secrets.xml`，提供 `url_backend_prod`、`url_backend_dev`、
     `mappingJsonUrl`、`disclaimerUrl`）。

---

## 八、从源码构建

```bash
git clone https://github.com/poxin4145/Extendroid.git
cd Extendroid
./gradlew :app:assembleDebug     # 调试包
./gradlew :app:assembleRelease   # 发布包（需要签名配置，见下）
```

- 需要 **JDK 17**、**Android SDK 35**，Gradle 8.14 由 wrapper 自动下载。
- 发布签名通过环境变量提供：`EXTENDROID_KEYSTORE`、`EXTENDROID_KEYSTORE_PASSWORD`、
  `EXTENDROID_KEY_ALIAS`、`EXTENDROID_KEY_PASSWORD`；不设置时 `assembleRelease` 产出未签名 APK。
- 仓库内置了 CI 使用的签名密钥，说明见 [.github/signing/README.md](.github/signing/README.md)。

### 自动发布

给提交打上 `v*` 标签并推送即可：

```bash
git tag v1.1.0
git push origin v1.1.0
```

GitHub Actions 会构建 APK、计算 SHA-256 并创建 Release。也可以在 Actions 页面手动触发
`Release` 工作流并指定版本号。

---

## 九、常见问题

**Q：菜单打不开，只提示「请稍候，马上开始」？**
A：说明悬浮窗权限还没授予，或 Shizuku 服务尚未就绪。先确认首页两项配置都是对勾。

**Q：应用窗口能打开但显示黑屏？**
A：部分应用（尤其是带 DRM 或禁止截屏的应用）无法被虚拟屏幕捕获；另外请确认没有开启省电模式限制后台。

**Q：小米设备上浮窗一闪而过？**
A：需要在「设置 → 应用 → Extendroid → 权限」中开启 MIUI 的「在其他应用上层显示」，部分机型还要允许「后台弹出界面」。

**Q：升级安装失败，提示签名不一致？**
A：本仓库的 Release 使用固定签名密钥，正常情况下可以直接覆盖安装；如果你之前装的是原作者版本或自己编译的版本，请先卸载。

**Q：切换语言后浮窗还是旧语言？**
A：浮窗运行在服务中，重新打开一次控制面板即可；若仍不生效，重启应用。

---

## 十、许可

GNU General Public License v3.0，详见 [LICENSE.txt](LICENSE.txt)。

上游附加要求：无论原版还是修改版，分发时必须注明原始源码地址；未经许可不得发布到任何应用商店。
