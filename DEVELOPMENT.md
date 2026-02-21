# QStory 热加载脚本开发文档

## 1. 脚本概述

本脚本是一个基于QStory平台的Java脚本，提供了以下核心功能：

- **热加载Java代码**：支持动态加载、编辑和保存Java文件
- **持久化加载列表**：可将常用脚本加入持久化列表，自动加载
- **动态管理员白名单**：支持添加/移除管理员权限
- **事件分发机制**：通过外部库实现消息事件分发，避免onMsg方法冲突
- **外部库集成**：支持加载外部JAR文件，扩展脚本功能
- **模块化设计**：采用模块化架构，便于功能扩展和维护

## 2. 目录结构

```
├── main.java          # 主脚本文件
├── text.java          # 示例脚本文件
├── EventLibrary.java  # 事件分发库源文件
├── SimpleHandler.java # 事件处理器接口
├── MyLibrary.java     # 基础工具库
├── AdvancedLibrary.java # 高级功能库
├── EventLibrary.jar   # 编译后的事件分发库
├── MyLibrary.jar      # 编译后的基础工具库
├── AdvancedLibrary.jar # 编译后的高级功能库
├── API.md             # QStory API文档
├── desc.txt           # 脚本描述文件
├── info.prop          # 脚本信息文件
└── debug.log          # 调试日志文件
```

## 3. 核心功能

### 3.1 热加载功能

- **保存脚本**：使用`/保存 文件名.java`命令保存新脚本
- **编辑脚本**：使用`/编辑 文件名.java`命令编辑现有脚本
- **加载脚本**：使用`/加载 文件名.java`命令加载脚本
- **持久化**：使用`/保持 文件名.java`命令将脚本加入持久化列表
- **取消持久化**：使用`/取消保持 文件名.java`命令从持久化列表移除

### 3.2 管理员管理

- **添加管理员**：使用`/加权 QQ号`命令添加管理员
- **移除管理员**：使用`/去权 QQ号`命令移除管理员
- **查看列表**：使用`/列表`命令查看持久化加载列表

### 3.3 事件分发机制

- **避免冲突**：使用EventLibrary实现消息事件分发，避免多个脚本的onMsg方法冲突
- **模块化处理**：每个脚本可独立注册消息处理器，专注于自己的功能
- **错误隔离**：一个处理器的错误不会影响其他处理器的运行

### 3.4 外部库集成

- **基础工具库**：提供消息处理的基本功能
- **高级功能库**：实现复杂的面向对象特性，如内部类、接口、单例模式等
- **事件分发库**：提供消息事件的注册和分发功能

## 4. API使用说明

### 4.1 全局变量

脚本环境提供以下全局变量，可直接使用：

- `String myUin`：当前用户的QQ号
- `Context context`：QQ全局上下文对象
- `String appPath`：脚本运行时的相对目录
- `ClassLoader loader`：QQ的类加载器
- `String pluginID`：当前脚本ID

### 4.2 回调方法

- `void onLoad()`：脚本完成加载时调用
- `void onMsg(Object msg)`：收到消息时调用
- `void onUnLoad()`：取消加载脚本时调用
- `void onTroopEvent(String GroupUin, String UserUin, int type)`：发生进群和退群时调用
- `void onForbiddenEvent(String GroupUin, String UserUin, String OPUin, long time)`：成员被禁言时调用

### 4.3 消息对象结构

`onMsg`方法中的`msg`对象包含以下字段：

- `String MessageContent`：消息内容
- `String GroupUin`：群号（仅在群消息时有效）
- `String UserUin`：发送者QQ号
- `boolean IsGroup`：是否群组消息
- `String SenderNickName`：发送者昵称
- `long MessageTime`：消息时间戳
- `ArrayList<String> mAtList`：艾特列表

### 4.4 核心API方法

#### 4.4.1 发送消息

- `sendMsg(String GroupUin, String UserUin, String msg)`：发送文本、图片或图文消息
- `sendPic(String GroupUin, String UserUin, String Path)`：发送单张图片
- `sendCard(String GroupUin, String UserUin, String card)`：发送卡片消息

#### 4.4.2 数据存储

- `putString(String ConfigName, String key, String value)`：存储文本数据
- `getString(String ConfigName, String key, String def)`：读取文本数据
- `putInt(String ConfigName, String key, int value)`：存储整数数据
- `getInt(String ConfigName, String key, int def)`：读取整数数据

#### 4.4.3 文件操作

- `readFileText(String path)`：读取文件文本
- `writeTextToFile(String path, String text)`：写入文本到文件
- `load(String path)`：加载Java文件
- `loadJar(String jarPath)`：加载JAR文件

#### 4.4.4 其他方法

- `log(Object content)`：输出日志到脚本目录
- `error(Throwable throwable)`：打印异常到脚本目录
- `toast(Object content)`：弹出toast提示
- `httpGet(String url)`：发送HTTP GET请求
- `httpPost(String url, Map<String, String> data)`：发送HTTP POST请求

## 5. 事件分发机制

### 5.1 原理

事件分发机制通过以下步骤实现：

1. **主脚本接收消息**：main.java的onMsg方法接收所有消息
2. **消息预处理**：主脚本处理命令和基础逻辑
3. **事件分发**：通过EventLibrary.dispatchMessage()方法分发消息
4. **处理器处理**：注册的处理器接收并处理感兴趣的消息

### 5.2 使用方法

#### 5.2.1 注册事件处理器

```java
// 创建并注册消息处理器
EventLibrary.registerHandler(new SimpleHandler() {
    public void handle(Object msg) {
        // 处理消息逻辑
        if (msg.MessageContent != null && msg.MessageContent.trim().equals("测试")) {
            if (msg.IsGroup) {
                sendMsg(msg.GroupUin, "", "hello world");
            } else {
                sendMsg("", msg.UserUin, "hello world");
            }
        }
    }
});
```

#### 5.2.2 处理器注册流程

1. **创建处理器**：实现SimpleHandler接口的handle方法
2. **注册处理器**：调用EventLibrary.registerHandler()方法注册
3. **消息分发**：主脚本接收到消息后会自动分发
4. **处理器执行**：所有注册的处理器都会收到消息并执行各自的逻辑

### 5.3 优势

- **避免冲突**：多个脚本可同时运行，不会因onMsg方法冲突而失效
- **模块化**：每个脚本专注于自己的功能，通过事件处理器响应消息
- **错误隔离**：一个处理器的错误不会影响其他处理器
- **灵活性**：可根据需要动态注册和移除处理器

## 6. 外部库集成

### 6.1 库加载

主脚本会在加载时自动加载以下外部库：

```java
void loadExternalLibrary() {
    try {
        loadJar(appPath + "/EventLibrary.jar");
        loadJar(appPath + "/MyLibrary.jar");
        loadJar(appPath + "/AdvancedLibrary.jar");
        log("External libraries loaded successfully");
    } catch (Exception e) {
        error(e);
        log("Failed to load external library: " + e.getMessage());
    }
}
```

### 6.2 库功能

#### 6.2.1 EventLibrary

- **registerHandler(SimpleHandler handler)**：注册消息处理器
- **dispatchMessage(Object msg)**：分发消息给所有注册的处理器
- **getHandlerCount()**：获取注册的处理器数量

#### 6.2.2 MyLibrary

- **processMessage(String message)**：处理消息文本
- **getMessageLength(String message)**：获取消息长度
- **reverseMessage(String message)**：反转消息文本
- **isTestMessage(String message)**：判断是否为测试消息

#### 6.2.3 AdvancedLibrary

- **createMessage(String content, String sender)**：创建消息对象
- **formatMessage(Object message)**：格式化消息
- **isEmptyMessage(Object message)**：判断消息是否为空
- **MessageService.getInstance()**：获取消息服务单例

## 7. 使用指南

### 7.1 基础使用

1. **启动脚本**：QStory加载脚本后会自动执行onLoad()方法
2. **查看帮助**：发送`/列表`命令查看持久化列表
3. **保存脚本**：发送`/保存 文件名.java`，然后发送脚本内容
4. **加载脚本**：发送`/加载 文件名.java`加载脚本
5. **测试功能**：发送`测试`消息测试脚本响应

### 7.2 高级使用

#### 7.2.1 创建事件处理器

```java
// text.java示例
void registerTestHandler() {
    try {
        // 创建并注册消息处理器
        SimpleHandler handler = new SimpleHandler() {
            public void handle(Object msg) {
                // 处理消息逻辑
                if (msg.MessageContent != null && msg.MessageContent.trim().equals("测试")) {
                    if (msg.IsGroup) {
                        sendMsg(msg.GroupUin, "", "hello world from text");
                    } else {
                        sendMsg("", msg.UserUin, "hello world from text");
                    }
                }
            }
        };
        
        // 注册处理器
        EventLibrary.registerHandler(handler);
        log("Test message handler registered successfully");
        
    } catch (Exception e) {
        error(e);
        log("Failed to register test handler: " + e.getMessage());
    }
}

// 调用注册方法
registerTestHandler();
```

#### 7.2.2 使用外部库

```java
// 测试基础库
String processed = MyLibrary.processMessage(content);
int length = MyLibrary.getMessageLength(content);
String reversed = MyLibrary.reverseMessage(content);
boolean isTest = MyLibrary.isTestMessage(content);

// 测试高级库
Object messageObj = AdvancedLibrary.createMessage(content, msg.UserUin);
String formattedMsg = AdvancedLibrary.formatMessage(messageObj);
boolean isEmpty = AdvancedLibrary.isEmptyMessage(messageObj);
```

## 8. 开发注意事项

### 8.1 QStory脚本限制

- **无注解支持**：脚本环境不支持注解，使用注解会导致加载失败
- **无访问修饰符**：不支持private、public、protected等修饰符
- **无内部类**：不支持内部类定义，需使用外部库实现
- **无接口定义**：不支持接口定义，需使用外部库实现
- **JDK9环境**：脚本运行在JDK9环境，不支持较新的API

### 8.2 最佳实践

- **模块化设计**：将功能拆分为多个小脚本，通过事件分发机制协作
- **错误处理**：添加完善的异常处理，避免脚本崩溃
- **日志记录**：使用log()方法记录关键操作，便于调试
- **权限检查**：对敏感操作进行管理员权限检查
- **路径处理**：使用appPath构建文件路径，避免路径错误

### 8.3 常见问题

#### 8.3.1 脚本加载失败

- **检查语法**：确保脚本语法符合QStory要求，无注解、无修饰符
- **检查依赖**：确保所有依赖的外部库已正确加载
- **查看日志**：查看debug.log文件，了解具体错误信息

#### 8.3.2 事件处理器不生效

- **检查注册**：确保处理器已正确注册到EventLibrary
- **检查逻辑**：确保处理器的handle方法逻辑正确
- **查看日志**：查看debug.log文件，了解处理器注册和执行情况

#### 8.3.3 主脚本无响应

- **检查冲突**：确保没有其他脚本覆盖了onMsg方法
- **检查日志**：查看debug.log文件，了解主脚本执行情况
- **检查权限**：确保发送命令的用户有管理员权限

## 9. 示例代码

### 9.1 主脚本示例

```java
// main.java

void onLoad() {
    ensureAdmin(myUin);
    loadExternalLibrary();
    loadPersistedFiles();
}

void onMsg(Object msg) {
    try {
        log("Main script onMsg triggered: " + (msg.MessageContent != null ? msg.MessageContent : "null"));
        
        String content = msg.MessageContent;
        if (content == null || content.isEmpty()) {
            log("Empty message, returning");
            return;
        }

        String sessionId;
        if (msg.IsGroup) {
            sessionId = "g" + msg.GroupUin + "_" + msg.UserUin;
        } else {
            sessionId = "p" + msg.UserUin;
        }

        if (handleWaitingState(msg, sessionId)) {
            log("Handled waiting state, returning");
            return;
        }

        if (handleCommand(msg, sessionId)) {
            log("Handled command, returning");
            return;
        }

        if (content.trim().equals("测试")) {
            log("Handling test message");
            if (msg.IsGroup) {
                sendMsg(msg.GroupUin, "", "hello world from main");
            } else {
                sendMsg("", msg.UserUin, "hello world from main");
            }
        }
        
        // 分发消息给注册的处理器
        try {
            log("Dispatching message to registered handlers");
            EventLibrary.dispatchMessage(msg);
        } catch (Exception e) {
            error(e);
            log("Error dispatching message: " + e.getMessage());
        }
        
        log("Main script onMsg completed successfully");
    } catch (Exception e) {
        error(e);
        log("Critical error in main onMsg: " + e.getMessage());
    }
}
```

### 9.2 事件处理器示例

```java
// text.java

void registerTestHandler() {
    try {
        log("Starting to register test handler");
        
        // 创建并注册消息处理器
        SimpleHandler handler = new SimpleHandler() {
            public void handle(Object msg) {
                try {
                    log("Test handler received message: " + (msg.MessageContent != null ? msg.MessageContent : "null"));
                    
                    // 检查消息内容是否为"测试"
                    if (msg.MessageContent != null && msg.MessageContent.trim().equals("测试")) {
                        log("Test handler processing test message");
                        // 群聊时发到群，私聊时发回给个人
                        if (msg.IsGroup) {
                            sendMsg(msg.GroupUin, "", "hello world from text");
                        } else {
                            sendMsg("", msg.UserUin, "hello world from text");
                        }
                    }
                } catch (Exception e) {
                    error(e);
                    log("Error in test handler: " + e.getMessage());
                }
            }
        };
        
        // 注册处理器
        EventLibrary.registerHandler(handler);
        log("Test message handler registered successfully");
        
        // 记录当前处理器数量
        int count = EventLibrary.getHandlerCount();
        log("Total handlers registered: " + count);
        
    } catch (Exception e) {
        error(e);
        log("Failed to register test handler: " + e.getMessage());
    }
}

// 调用注册方法
registerTestHandler();

log("text.java loaded successfully");
```

## 10. 总结

本脚本通过模块化设计和事件分发机制，解决了QStory脚本环境的诸多限制，为开发者提供了一个灵活、强大的开发框架。通过热加载功能，开发者可以实时编写和测试代码；通过事件分发机制，多个脚本可以和谐共存，避免方法冲突；通过外部库集成，实现了复杂的面向对象特性。

这种设计不仅提高了开发效率，也增强了脚本的可维护性和扩展性，为QStory脚本开发开辟了新的可能性。

---

**文档更新日期**：2026-02-21
**作者**氢氧根
**版本**：1.0.0
