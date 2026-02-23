# QStory 热加载脚本开发文档

## 1. 脚本概述

本脚本是一个基于QStory平台的Java脚本，提供了以下核心功能：

- **热加载Java代码**：支持动态加载、编辑和保存Java文件
- **持久化加载列表**：可将常用脚本加入持久化列表，自动加载
- **动态管理员白名单**：支持添加/移除管理员权限
- **全局方法机制**：通过main.java提供的全局方法，实现脚本间的协作
- **外部库集成**：支持加载外部JAR文件，扩展脚本功能
- **模块化设计**：采用模块化架构，便于功能扩展和维护

## 2. 目录结构

```
├── main.java          # 主脚本文件
├── text.java          # 示例脚本文件
├── scripts/           # 脚本目录
│   └── text.java      # scripts目录中的示例脚本
├── EventLibrary.java  # 事件分发库源文件
├── MyLibrary.java     # 基础工具库
├── AdvancedLibrary.java # 高级功能库
├── EventLibrary.jar   # 编译后的事件分发库
├── MyLibrary.jar      # 编译后的基础工具库
├── AdvancedLibrary.jar # 编译后的高级功能库
├── API.md             # QStory API文档
├── DEVELOPMENT.md     # 开发文档
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

### 3.3 全局方法机制

- **避免冲突**：使用main.java提供的全局方法，避免多个脚本的onMsg方法冲突
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

## 5. 全局方法机制

### 5.1 原理

全局方法机制通过以下步骤实现：

1. **主脚本提供全局方法**：main.java提供一系列全局方法供其他脚本调用
2. **脚本注册处理器**：其他脚本通过全局方法注册消息处理器
3. **主脚本分发事件**：main.java的onMsg方法接收消息后，调用所有注册的处理器
4. **处理器处理**：注册的处理器接收并处理感兴趣的事件

### 5.2 支持的全局方法

main.java提供以下全局方法：

- **registerScriptMessageHandler(Object handler)**：注册消息处理器
- **registerScript(String scriptName, Object scriptObject)**：注册脚本
- **sendGlobalMessage(String groupUin, String userUin, String content)**：发送消息
- **logGlobal(String message)**：记录日志
- **errorGlobal(Exception e)**：处理错误
- **isGlobalAdmin(String qq)**：检查是否为管理员

### 5.3 使用方法

#### 5.3.1 注册消息处理器

```java
// text.java示例

// 文本消息处理器类
class TextMessageHandler {
    public void onMessage(Object msg) {
        try {
            log("Text handler received message: " + (msg.MessageContent != null ? msg.MessageContent : "null"));    

            // 检查消息内容是否为"text"
            if (msg.MessageContent != null && msg.MessageContent.trim().equals("text")) {
                log("Text handler processing text message");
                // 群聊时发到群，私聊时发回给个人
                if (msg.IsGroup) {
                    sendMsg(msg.GroupUin, "", "hello world from text");
                } else {
                    sendMsg("", msg.UserUin, "hello world from text");
                }
            }
        } catch (Exception e) {
            error(e);
            log("Error in text handler: " + e.getMessage());
        }
    }
}

// 创建处理器实例
TextMessageHandler textHandler = new TextMessageHandler();

// 注册到main.java的消息处理器列表
registerScriptMessageHandler(textHandler);

// 注册脚本
registerScript("text", textHandler);

log("text.java loaded successfully - using global methods");
```

#### 5.3.2 处理器注册流程

1. **创建处理器类**：创建一个包含onMessage方法的处理器类
2. **创建处理器实例**：实例化处理器类
3. **注册处理器**：调用registerScriptMessageHandler方法注册处理器
4. **注册脚本**：调用registerScript方法注册脚本信息
5. **事件分发**：main.java接收到消息后会自动调用所有注册的处理器

### 5.4 优势

- **避免冲突**：多个脚本可同时运行，不会因全局回调方法冲突而失效
- **模块化**：每个脚本专注于自己的功能，通过处理器响应事件
- **错误隔离**：一个处理器的错误不会影响其他处理器
- **灵活性**：可根据需要动态注册和移除处理器
- **简单易用**：使用简单的方法调用，不需要复杂的接口实现
- **直接集成**：不需要依赖外部库，直接使用main.java提供的方法

## 6. 外部库集成

### 6.1 库加载

主脚本会在加载时自动加载以下外部库：

```java
void loadExternalLibrary() {
    try {
        log("Starting to load external libraries");
        
        // 加载核心库 - EventLibrary.jar
        String eventLibraryPath = appPath + "/EventLibrary.jar";
        log("Checking EventLibrary.jar at: " + eventLibraryPath);
        if (fileExists(eventLibraryPath)) {
            try {
                log("Attempting to load EventLibrary.jar...");
                loadJar(eventLibraryPath);
                log("✓ EventLibrary.jar loaded successfully");
            } catch (Exception e) {
                error(e);
                log("✗ Failed to load EventLibrary.jar: " + e.getMessage());
            }
        } else {
            log("✗ EventLibrary.jar not found at: " + eventLibraryPath);
        }
        
        // 加载其他库
        String myLibraryPath = appPath + "/MyLibrary.jar";
        log("Checking MyLibrary.jar at: " + myLibraryPath);
        if (fileExists(myLibraryPath)) {
            try {
                log("Attempting to load MyLibrary.jar...");
                loadJar(myLibraryPath);
                log("✓ MyLibrary.jar loaded successfully");
            } catch (Exception e) {
                error(e);
                log("✗ Failed to load MyLibrary.jar: " + e.getMessage());
            }
        } else {
            log("✗ MyLibrary.jar not found at: " + myLibraryPath);
        }
        
        String advancedLibraryPath = appPath + "/AdvancedLibrary.jar";
        log("Checking AdvancedLibrary.jar at: " + advancedLibraryPath);
        if (fileExists(advancedLibraryPath)) {
            try {
                log("Attempting to load AdvancedLibrary.jar...");
                loadJar(advancedLibraryPath);
                log("✓ AdvancedLibrary.jar loaded successfully");
            } catch (Exception e) {
                error(e);
                log("✗ Failed to load AdvancedLibrary.jar: " + e.getMessage());
            }
        } else {
            log("✗ AdvancedLibrary.jar not found at: " + advancedLibraryPath);
        }
        
        log("External libraries loading completed");
    } catch (Exception e) {
        error(e);
        log("Failed to load external library: " + e.getMessage());
    }
}
```

### 6.2 库功能

#### 6.2.1 MyLibrary

- **processMessage(String message)**：处理消息文本
- **getMessageLength(String message)**：获取消息长度
- **reverseMessage(String message)**：反转消息文本
- **isTestMessage(String message)**：判断是否为测试消息

#### 6.2.2 AdvancedLibrary

- **createMessage(String content, String sender)**：创建消息对象
- **formatMessage(Object message)**：格式化消息
- **isEmptyMessage(Object message)**：判断消息是否为空
- **MessageService.getInstance()**：获取消息服务单例

#### 6.2.3 EventLibrary（可选）

虽然现在主要使用全局方法机制，但EventLibrary仍然可以作为可选的事件分发解决方案。

## 7. 使用指南

### 7.1 基础使用

1. **启动脚本**：QStory加载脚本后会自动执行onLoad()方法
2. **查看帮助**：发送`/列表`命令查看持久化列表
3. **保存脚本**：发送`/保存 文件名.java`，然后发送脚本内容
4. **加载脚本**：发送`/加载 文件名.java`加载脚本
5. **测试功能**：发送`测试`消息测试脚本响应

### 7.2 高级使用

#### 7.2.1 创建消息处理器

```java
// text.java示例

// 文本消息处理器类
class TextMessageHandler {
    public void onMessage(Object msg) {
        try {
            log("Text handler received message: " + (msg.MessageContent != null ? msg.MessageContent : "null"));    

            // 检查消息内容是否为"text"
            if (msg.MessageContent != null && msg.MessageContent.trim().equals("text")) {
                log("Text handler processing text message");
                // 群聊时发到群，私聊时发回给个人
                if (msg.IsGroup) {
                    sendMsg(msg.GroupUin, "", "hello world from text");
                } else {
                    sendMsg("", msg.UserUin, "hello world from text");
                }
            }
        } catch (Exception e) {
            error(e);
            log("Error in text handler: " + e.getMessage());
        }
    }
}

// 创建处理器实例
TextMessageHandler textHandler = new TextMessageHandler();

// 注册到main.java的消息处理器列表
registerScriptMessageHandler(textHandler);

// 注册脚本
registerScript("text", textHandler);

log("text.java loaded successfully - using global methods");

// 测试全局方法
logGlobal("Text script initialized");
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

// 全局方法集合，供其他脚本调用
// 注册消息处理器
void registerMessageHandler(String name, Object handler) {
    // 这里可以实现简单的处理器注册逻辑
    log("Registered message handler: " + name);
}

// 发送消息的全局方法
void sendGlobalMessage(String groupUin, String userUin, String content) {
    sendMsg(groupUin, userUin, content);
}

// 日志记录的全局方法
void logGlobal(String message) {
    log(message);
}

// 错误处理的全局方法
void errorGlobal(Exception e) {
    error(e);
}

// 检查是否为管理员的全局方法
boolean isGlobalAdmin(String qq) {
    return isAdmin(qq);
}

// 脚本注册方法，供其他脚本在加载时调用
void registerScript(String scriptName, Object scriptObject) {
    log("Script registered: " + scriptName);
}

// 脚本消息处理器映射
ArrayList<Object> scriptMessageHandlers = new ArrayList<>();

// 注册脚本消息处理器
void registerScriptMessageHandler(Object handler) {
    if (handler != null) {
        scriptMessageHandlers.add(handler);
        log("Registered script message handler: " + handler.getClass().getName());
    }
}

void onLoad() {
    ensureAdmin(myUin);
    
    // 优先加载外部库，确保所有JAR文件在脚本加载前准备就绪
    try {
        log("=== Loading external libraries ===");
        loadExternalLibrary();
        log("=== External libraries loaded successfully ===");
    } catch (Exception e) {
        error(e);
        log("Error loading external libraries: " + e.getMessage());
    }
    
    // 延迟一秒，确保JAR文件完全加载
    try {
        Thread.sleep(1000);
        log("Waiting for JAR files to initialize...");
    } catch (Exception e) {
        // 忽略睡眠异常
    }
    
    // 验证EventLibrary是否加载成功
    try {
        log("=== Verifying EventLibrary ===");
        int handlerCount = EventLibrary.getTotalHandlerCount();
        log("EventLibrary loaded successfully, total handlers: " + handlerCount);
    } catch (Exception e) {
        error(e);
        log("EventLibrary not available: " + e.getMessage());
        log("Continuing without EventLibrary...");
    }
    
    // 加载持久化脚本
    try {
        log("=== Loading persisted scripts ===");
        loadPersistedFiles();
        log("=== Persisted scripts loaded successfully ===");
    } catch (Exception e) {
        error(e);
        log("Error loading persisted scripts: " + e.getMessage());
    }
    
    // 分发加载事件
    try {
        log("Dispatching load event to registered handlers");
        EventLibrary.dispatchLoad();
    } catch (Exception e) {
        error(e);
        log("Error dispatching load event: " + e.getMessage());
    }
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
        
        // 调用脚本消息处理器
        try {
            log("Calling script message handlers");
            for (Object handler : scriptMessageHandlers) {
                try {
                    // 尝试调用handler的onMessage方法
                    java.lang.reflect.Method method = handler.getClass().getMethod("onMessage", Object.class);
                    method.invoke(handler, msg);
                } catch (NoSuchMethodException e) {
                    // 忽略没有onMessage方法的处理器
                } catch (Exception e) {
                    error(e);
                    log("Error calling script message handler: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            error(e);
            log("Error calling script message handlers: " + e.getMessage());
        }
        
        // 尝试直接调用text脚本的处理方法（兼容旧版本）
        try {
            if (content.trim().equals("text")) {
                log("Handling text command");
                if (msg.IsGroup) {
                    sendMsg(msg.GroupUin, "", "hello world from text");
                } else {
                    sendMsg("", msg.UserUin, "hello world from text");
                }
            }
        } catch (Exception e) {
            error(e);
            log("Error handling text command: " + e.getMessage());
        }
        
        log("Main script onMsg completed successfully");
    } catch (Exception e) {
        error(e);
        log("Critical error in main onMsg: " + e.getMessage());
    }
}
```

### 9.2 消息处理器示例

```java
// text.java

// 文本消息处理器类
class TextMessageHandler {
    public void onMessage(Object msg) {
        try {
            log("Text handler received message: " + (msg.MessageContent != null ? msg.MessageContent : "null"));    

            // 检查消息内容是否为"text"
            if (msg.MessageContent != null && msg.MessageContent.trim().equals("text")) {
                log("Text handler processing text message");
                // 群聊时发到群，私聊时发回给个人
                if (msg.IsGroup) {
                    sendMsg(msg.GroupUin, "", "hello world from text");
                } else {
                    sendMsg("", msg.UserUin, "hello world from text");
                }
            }
        } catch (Exception e) {
            error(e);
            log("Error in text handler: " + e.getMessage());
        }
    }
}

// 创建处理器实例
TextMessageHandler textHandler = new TextMessageHandler();

// 注册到main.java的消息处理器列表
registerScriptMessageHandler(textHandler);

// 注册脚本
registerScript("text", textHandler);

log("text.java loaded successfully - using global methods");

// 测试全局方法
logGlobal("Text script initialized");
```

## 10. 总结

本脚本通过模块化设计和全局方法机制，解决了QStory脚本环境的诸多限制，为开发者提供了一个灵活、强大的开发框架。

### 核心优势

- **热加载功能**：支持动态加载、编辑和保存Java文件，实现实时开发和测试
- **全局方法机制**：通过main.java提供的全局方法，实现脚本间的协作，避免全局回调方法冲突
- **错误隔离**：一个处理器的错误不会影响其他处理器的运行
- **外部库集成**：通过加载JAR文件，实现复杂的面向对象特性
- **模块化设计**：每个脚本可以专注于自己的功能，通过处理器响应事件
- **简单易用**：使用简单的方法调用，不需要复杂的接口实现
- **直接集成**：不需要依赖外部库，直接使用main.java提供的方法

### 技术创新

- **全局方法系统**：main.java提供一系列全局方法，方便其他脚本调用
- **动态处理器注册**：支持脚本动态注册消息处理器
- **反射机制**：使用反射调用处理器的方法，提高灵活性
- **向后兼容**：保持了与原有API的兼容性，同时扩展了新功能
- **错误处理**：完善的错误处理机制，确保脚本稳定运行

这种设计不仅提高了开发效率，也增强了脚本的可维护性和扩展性，为QStory脚本开发开辟了新的可能性。开发者可以通过全局方法机制，创建更加模块化、可维护的脚本，同时避免了全局方法冲突的问题。

---

**文档更新日期**：2026-02-23
**作者**：氢氧根
**版本**：2.0.0
