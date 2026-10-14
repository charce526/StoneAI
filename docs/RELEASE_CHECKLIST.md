# GitHub 首次发布检查清单

目标仓库：<https://github.com/charce526/StoneAI>

## 当前已完成

- [x] README、贡献指南、安全政策和更新记录
- [x] 采用 Apache License 2.0
- [x] `.gitignore` 排除本机配置、构建产物、凭据与签名文件
- [x] 作者说明统一为“AI练习作品”
- [x] 当前源码敏感信息扫描
- [x] 单元测试、Android Lint 与 Release 编译验证
- [x] API Key 使用 Android Keystore 加密并排除备份
- [x] 日志不输出原始响应、照片路径、精确坐标或识别对象名称
- [x] 使用现有 `workspace-release.jks` 完成 v1.1.0 正式签名
- [x] 正式 APK 签名、版本号、作者文案与 SHA-256 校验

## 公开仓库前需要确认

- [ ] 确认仓库显示名称、简介与 Topics
- [ ] 确认是否保留当前包名 `cn.xzbim.stoneai`
- [ ] 确认 README 中的功能、隐私说明和免责声明
- [x] 首次发布版本：`versionName 1.1.0`、`versionCode 2`
- [x] 正式签名使用 `workspace_key`；证书和密码不提交到 Git
- [x] 首次 Release 提供 APK，不提供截图
- [x] 首次 GitHub Release 仅提供 APK，不提供 AAB
- [ ] 在真实设备上完成拍照、相册、定位、识别、历史和分享回归测试

## 初始化 Git 前

1. 再次执行敏感信息扫描。
2. 检查 `git status` 中不存在 `local.properties`、`.idea/`、`build/`、APK、AAB、密钥或签名文件。
3. 运行：

   ```bash
   ./gradlew clean testDebugUnitTest lintDebug assembleRelease
   ```

4. 检查 Release 构建所用证书，不使用 debug 证书对外发布。
5. 记录最终 APK 的 SHA-256 校验值。

## GitHub 仓库设置建议

- 默认分支：`main`
- 简介：`一款支持自定义 OpenAI 兼容视觉模型的 Android 岩石与矿物识别应用`
- Topics：`android`、`kotlin`、`jetpack-compose`、`camerax`、`openai-compatible`、`image-recognition`、`geology`
- 开启 Issues 与 Private vulnerability reporting
- 视维护方式决定是否启用 Discussions
- 首次稳定发布建议使用标签 `v1.1.0`

## 发布后验证

- [ ] README 链接、截图与安全报告链接可访问
- [ ] GitHub 自动识别的许可证正确
- [ ] Release 附件可下载，SHA-256 与本地一致
- [ ] 从 Release 下载的 APK 可以在测试设备正常安装和启动
- [ ] 仓库代码搜索中没有凭据、本机路径和个人信息
