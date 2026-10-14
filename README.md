# AI 识石（StoneAI）

一款面向自然探索与科普学习的 Android 岩石、矿物图片识别应用。用户可以拍照或从相册选择照片，通过自行配置的 OpenAI 兼容多模态服务获取结构化识别结果，并在本地保存记录、生成分享卡片。

> AI 识别结果仅供学习和科普参考，不能替代专业地质鉴定、珠宝检测或其他具有法律效力的检测结论。

## 功能特性

- 拍照识别或从系统文件选择照片
- 相机、预览、识别结果、历史记录及分享卡片统一采用 1:1 照片展示比例
- 支持 OpenAI 兼容接口，可自定义服务地址、API Key 和视觉模型
- 返回名称、分类、置信度、视觉特征、组成、形成环境、用途及辅助测试建议等结构化信息
- 在用户授权且照片包含相关信息时读取拍摄时间和地点
- 识别历史与照片保存在设备本地，可分别控制是否保存
- 生成包含照片、识别摘要、拍摄时间与地点的自适应高度分享卡片
- 支持简体中文、英文及跟随系统的识别结果语言设置
- API Key 使用 Android Keystore 与 AES/GCM 加密后保存在设备端

## 使用说明

1. 打开应用，在“设置 → AI 服务”中添加服务配置。
2. 填写 OpenAI 兼容接口地址、API Key，并选择或填写支持图片输入的模型。
3. 测试连接并激活该配置。
4. 返回首页，通过“拍照识别”或“从相册选择”开始识别。

默认接口地址为 `https://api.openai.com/v1`，应用也支持使用兼容 `/models` 与 `/chat/completions` 接口的其他服务。具体可用模型、费用、数据处理方式及服务条款由相应服务提供方决定。

## 隐私与数据

- 项目源码不包含 API Key，使用者需要自行配置。
- API Key 经 Android Keystore 保护，不会写入普通配置文件，也被排除在 Android 备份之外。
- 发起识别时，经过缩放和压缩的照片、识别提示词及所选模型信息会发送到用户配置的 AI 服务。
- 识别历史保存在应用本地数据库中；用户可以关闭历史或照片保存，也可以在应用内清除数据。
- 相机定位、相册媒体位置权限均为可选。未授权时仍可使用不依赖位置的功能。
- 相册照片的位置和时间仅从其可访问的 EXIF 元数据读取；应用拍摄照片的位置仅在用户授权后获取。
- 分享卡片由设备本地生成，只有用户主动调用系统分享面板后才会交给所选的目标应用。

请勿将真实 API Key、签名文件、`local.properties`、服务账号文件或其他凭据提交到仓库。若发现安全问题，请参阅 [SECURITY.md](SECURITY.md)。

## 系统要求

- Android 8.0（API 26）及以上
- Android SDK 37（编译）
- JDK 17
- Android Studio 或支持 Gradle 9.6 的命令行环境
- 一个支持图片输入的 OpenAI 兼容模型及有效 API Key

## 本地构建

克隆仓库后，用 Android Studio 打开项目并等待 Gradle 同步，或者在项目根目录执行：

```bash
# Windows
gradlew.bat assembleDebug

# macOS / Linux
./gradlew assembleDebug
```

Debug APK 默认生成在 `app/build/outputs/apk/debug/`。

运行测试和静态检查：

```bash
# Windows
gradlew.bat testDebugUnitTest lintDebug

# macOS / Linux
./gradlew testDebugUnitTest lintDebug
```

Release 构建不会回退使用 debug 证书。未配置正式证书时会生成带 `_unsigned` 后缀的未签名产物；需要签名时，将 `keystore.properties.example` 复制为被 Git 忽略的 `keystore.properties`，填写本机证书信息后再构建。CI 也可以使用 `STONEAI_KEYSTORE_FILE`、`STONEAI_STORE_PASSWORD`、`STONEAI_KEY_ALIAS` 和 `STONEAI_KEY_PASSWORD` 环境变量。仓库不会包含任何真实签名证书或密码。

## 技术栈

- Kotlin、Jetpack Compose、Material 3
- CameraX
- Navigation Compose
- Room、DataStore
- Android Keystore
- OkHttp
- Coil
- ExifInterface

## 项目结构

```text
app/src/main/java/cn/xzbim/stoneai/
├── ai/          # OpenAI 兼容服务实现
├── data/        # 本地存储、数据库与 Repository
├── model/       # 业务数据模型
├── navigation/  # Compose 导航
├── network/     # 网络客户端与错误映射
├── prompt/      # 识别提示词
├── share/       # 分享卡片与系统分享
├── ui/          # Compose 页面与组件
├── util/        # 图片、EXIF、解析等工具
└── viewmodel/   # 页面状态和业务流程
```

## 参与贡献

欢迎提交问题和改进建议。开始贡献前请阅读 [CONTRIBUTING.md](CONTRIBUTING.md)。

计划发布地址：[github.com/charce526/StoneAI](https://github.com/charce526/StoneAI)

## 许可证

本项目采用 [Apache License 2.0](LICENSE) 开源许可证。

## 致谢

AI 练习作品。
