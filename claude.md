# Android Prompts 应用

## 项目简介
该 Android app 项目的使用场景：利用 Gemini 的 API，对多个 prompt 进行轮询请求并展示。

## 功能需求

### 1. 设置功能
- 通过顶部右上角的设置按钮展开底部 BottomSheet
- 文本框可填写 Gemini API Key，支持保存和修改
- 新增模型选择功能：
  - gemini-2.5-pro (高性能模型)
  - gemini-2.5-flash (默认选择，平衡性能)
  - gemini-2.5-flash-lite (轻量级模型)
- BottomSheet 上有导入按钮，通过文件选择器导入 JSON 文件
- 读取 prompt 列表并保存到项目中，方便下次使用

### 2. JSON 文件格式
```json
[
    {
        "历史": [
            "=== 通鉴镜 ===\n=== "
        ]
    },
    {
        "读书": [
            "=== 书境探索者 ===\n\n=== "
        ]
    },
    {
        "思考": [
            "=== 知识向导 ===\n\n=== "
        ]
    },
    {
        "小说": [
            "=== 吓自己 ===\n\n=== 扩写100字故事\n"
        ]
    }
]
```

### 3. 侧边栏功能
- 点击顶部左上角的图标展开侧边栏
- 侧边栏布局优化：
  - 上半部分：用户输入文本框，支持多行输入
  - 下半部分：prompt分类选择区域，支持滚动
  - 底部：发送按钮，固定位置
- Prompt分类使用 RadioGroup 展示可单选的大类
- 显示每个分类下的子类数量

### 4. 轮询机制
- 点击发送按钮时，对选中的大类进行轮询
- 大分类下的所有子分类prompt只轮询一遍，完成后自动停止
- 上次请求返回后，倒计时1分钟再次触发API请求
- **网络请求失败时不重试，直接继续下一个prompt**
- 轮询过程中通知栏实时显示：
  - 请求状态：显示"请求中(当前/总数) [prompt名称] X秒"
  - 倒计时状态：显示"等待下一个请求: X秒 | 分类: [分类名]"
  - 请求结果：显示成功/失败状态和具体信息
  - 完成状态：显示"轮询完成 已完成所有X个prompt的轮询"

### 5. API 请求示例
```bash
curl "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent" \
  -H 'Content-Type: application/json' \
  -H 'X-goog-api-key: GEMINI_API_KEY' \
  -X POST \
  -d '{
    "contents": [
      {
        "parts": [
          {
            "text": "$prompt + user question"
          }
        ]
      }
    ]
  }'
```

### 6. 主界面展示
- 主页顶部有设置和历史两个按钮
- 下方是 ViewPager 展示不同 prompt 下的返回文本
- ViewPager中的响应文本字号为18sp，提升可读性

### 7. 通知系统
- 使用一个常驻通知显示应用状态和最新操作信息
- 通知会实时更新，不会产生多条通知

### 8. 历史记录功能
- 每次发送按钮点击后，所有的返回值需要和问题绑定后存在 Room 数据库中
- 设置按钮的左边有一个历史按钮，可以查看之前的历史数据
- 历史记录按时间倒序排列，最新的记录显示在列表最上方
- 点击历史记录项可跳转到详情页面，查看完整的问题和回答内容
- 详情页支持文本选择，方便复制内容
- 支持清空历史记录功能

## 代码要求

### 技术限制
1. 禁止使用 Jetpack Compose 和 ViewBinding 库
2. 使用 Kotlin 编码，类尽可能的短小精悍
3. 网络请求使用 Retrofit + OkHttp
4. 应用的所有页面都是暗色主题

### 技术栈
- **UI**: 传统View系统 + Material Design
- **网络**: Retrofit2 + OkHttp (超时时间: 1分钟)
- **数据库**: Room
- **数据存储**: SharedPreferences
- **异步处理**: Kotlin Coroutines
- **通知**: Android Notification API

## 注意事项

### 常见错误修复方案

#### 1. AAPT错误 - attribute auto:menu not found
- **问题**: NavigationView中使用了不存在的menu属性
- **解决**: 移除`app:menu`属性，改用LinearLayout包含include布局

#### 2. AAPT错误 - attribute auto:popupTheme not found
- **问题**: Toolbar中popupTheme属性找不到
- **解决**: 移除`app:popupTheme="@style/ThemeOverlay.AppCompat.Light"`属性

#### 3. AAPT错误 - attribute auto:headerLayout not found
- **问题**: NavigationView的headerLayout属性错误
- **解决**: 改用LinearLayout包含`<include layout="@layout/nav_header" />`

#### 4. NullPointerException - navView.getHeaderView(0) returns null
- **问题**: NavigationView结构改变后无法获取header视图
- **解决**: 直接使用`findViewById`获取视图，而不是通过`getHeaderView(0)`

#### 5. KAPT插件版本冲突
- **问题**: `Error resolving plugin [id: 'org.jetbrains.kotlin.kapt', version: '2.0.21']`
- **解决**: 使用`kotlin("kapt")`替代`alias(libs.plugins.kotlin.kapt)`

#### 6. 菜单文件命名空间错误
- **问题**: `AAPT: error: attribute auto:showAsAction not found`
- **解决**: 确保使用正确命名空间`xmlns:app="http://schemas.android.com/apk/res-auto"`和`app:showAsAction`

#### 7. 通知权限问题
- **问题**: Android 13+通知不显示
- **解决**: 添加`POST_NOTIFICATIONS`权限，在代码中请求权限，检查权限状态

#### 8. 网络安全策略错误
- **问题**: `cleartext communication to [IP] not permitted by network security policy`
- **解决**: 添加网络安全配置文件`network_security_config.xml`，允许明文HTTP连接
- **配置**: 在AndroidManifest.xml中添加`android:networkSecurityConfig="@xml/network_security_config"`

### 项目结构要点

#### 暗色主题配色方案
- 主背景: `#121212`
- 次要背景: `#1E1E1E`
- 组件背景: `#303030`
- 输入框背景: `#424242`
- 主色调: `#2E2E2E` (深灰色，简洁无彩色设计)
- 文字颜色: `#FFFFFF`
- 次要文字: `#CCCCCC`

#### 核心组件架构
- **侧边栏**: 使用LinearLayout替代NavigationView以避免布局问题
- **通知功能**: 需要权限检查和异常处理
- **数据展示**: ViewPager2用于显示API响应结果
- **数据存储**: 
  - SharedPreferences存储API Key和prompt配置
  - Room数据库存储历史对话记录
- **网络处理**: 失败不重试，直接继续轮询下一个prompt

#### 关键实现细节
- 使用Handler + Coroutines实现1分钟轮询间隔
- 轮询过程实时显示正计时和倒计时状态到通知栏
- 每个分类的prompt只轮询一遍，完成后自动停止
- 侧边栏分类按钮间距优化，提升操作体验
- 历史记录支持点击跳转详情页，完整展示问答内容
- 网络超时设置为1分钟（连接、读取、写入超时）
- 所有API调用结果（成功/失败/异常）都保存到数据库
- 历史记录按时间戳倒序排列，最新记录在顶部
- 常驻通知实时更新应用状态，避免多条通知干扰
- 支持动态添加RadioButton显示prompt分类和数量
