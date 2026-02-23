# QStory 热加载脚本框架

一个基于QStory平台的Java脚本框架，提供热加载、事件分发、模块化设计等功能，解决脚本冲突问题。

## 功能特性

- **热加载Java代码**：支持动态加载、编辑和保存Java文件
- **持久化加载列表**：可将常用脚本加入持久化列表，自动加载
- **动态管理员白名单**：支持添加/移除管理员权限
- **完整的事件分发机制**：支持9种不同类型的事件，避免方法冲突
- **外部库集成**：支持加载外部JAR文件，扩展脚本功能
- **优先级系统**：处理器支持优先级设置，确保重要处理器先执行
- **枚举类型支持**：提供消息类型和优先级的枚举，提高代码可读性
- **错误隔离**：一个处理器的错误不会影响其他处理器的运行

## 目录结构

```
├── main.java          # 主脚本文件
├── text.java          # 示例脚本文件
├── EventLibrary.java  # 事件分发库源文件
├── MyLibrary.java     # 基础工具库
├── AdvancedLibrary.java # 高级功能库
├── EventLibrary.jar   # 编译后的事件分发库
├── MyLibrary.jar      # 编译后的基础工具库
├── AdvancedLibrary.jar # 编译后的高级功能库
├── API.md             # QStory API文档
├── DEVELOPMENT.md     # 开发文档
├── README.md          # 项目说明文档
├── desc.txt           # 脚本描述文件
├── info.prop          # 脚本信息文件
└── debug.log          # 调试日志文件
```

## 快速开始

### 1. 安装

1. 克隆本仓库到本地：
   ```bash
   git clone https://github.com/zhch223/qstory-script.git
   ```

2. 将文件夹复制到QStory脚本目录中

3. 在QStory中加载 `main.java`

### 2. 基本命令

**管理员命令**：
- `/加权 QQ号` - 将指定QQ加入管理员白名单
- `/去权 QQ号` - 将指定QQ从管理员白名单移除
- `/保存 文件名.java` - 静默等待下一条消息作为代码保存
- `/编辑 文件名.java` - 显示当前内容，等待新内容覆盖
- `/加载 文件名.java` - 立即加载脚本
- `/保持 文件名.java` - 加入持久化列表并加载
- `/取消保持 文件名.java` - 从持久化列表移除
- `/取消` - 放弃当前等待状态
- `/列表` - 查看持久化加载列表
- `/脚本列表` - 查看所有脚本文件
- `/stop 文件名.java` - 停止指定脚本

### 3. 创建事件处理器

```java
// 注册消息处理器
EventLibrary.registerHandler(new EventLibrary.MessageHandler() {
    public void handle(Object msg) {
        if (msg.MessageContent != null && msg.MessageContent.trim().equals("测试")) {
            if (msg.IsGroup) {
                sendMsg(msg.GroupUin, "", "hello world from handler");
            } else {
                sendMsg("", msg.UserUin, "hello world from handler");
            }
        }
    }
}, EventLibrary.Priority.HIGH);

// 注册禁言事件处理器
EventLibrary.registerForbiddenEventHandler(new EventLibrary.ForbiddenEventHandler() {
    public void onForbiddenEvent(String GroupUin, String UserUin, String OPUin, long time) {
        sendMsg(GroupUin, "", "用户 " + UserUin + " 被禁言 " + time + " 秒");
    }
});

// 注册进群/退群事件处理器
EventLibrary.registerTroopEventHandler(new EventLibrary.TroopEventHandler() {
    public void onTroopEvent(String GroupUin, String UserUin, int type) {
        if (type == 2) {
            sendMsg(GroupUin, "", "欢迎新成员加入群聊！");
        } else if (type == 1) {
            sendMsg(GroupUin, "", "成员已退出群聊");
        }
    }
});
```

## 事件类型

EventLibrary支持以下事件类型：

1. **消息事件** (`MessageHandler`) - 收到消息时触发
2. **禁言事件** (`ForbiddenEventHandler`) - 成员被禁言时触发
3. **进群/退群事件** (`TroopEventHandler`) - 有成员进群或退群时触发
4. **悬浮窗点击事件** (`FloatingWindowClickHandler`) - 点击脚本悬浮窗时触发
5. **消息发送事件** (`MessageSendingHandler`) - 点击发送按钮发送消息时触发
6. **菜单创建事件** (`MenuCreationHandler`) - 长按消息创建菜单时触发
7. **原始消息事件** (`RawMessageHandler`) - 收到未解析的原始消息时触发
8. **脚本加载事件** (`LoadHandler`) - 脚本完成加载时触发
9. **脚本卸载事件** (`UnloadHandler`) - 取消加载脚本时触发

## 开发指南

### 1. 创建新脚本

1. 使用 `/保存 文件名.java` 命令创建新脚本
2. 编写脚本代码，注册必要的事件处理器
3. 使用 `/加载 文件名.java` 命令加载脚本
4. 可选：使用 `/保持 文件名.java` 命令将脚本加入持久化列表

### 2. 事件处理器最佳实践

- **模块化设计**：将不同功能拆分为多个小脚本
- **错误处理**：在处理器中添加异常处理，避免影响其他处理器
- **日志记录**：使用 `log()` 方法记录关键操作
- **优先级设置**：根据处理器的重要性设置合适的优先级
- **类型安全**：使用枚举类型提高代码可读性和类型安全性

### 3. 外部库使用

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

## 示例脚本

### 消息响应脚本

```java
// text.java
void registerTestHandler() {
    try {
        EventLibrary.MessageHandler handler = new EventLibrary.MessageHandler() {
            public void handle(Object msg) {
                try {
                    if (msg.MessageContent != null && msg.MessageContent.trim().equals("测试")) {
                        if (msg.IsGroup) {
                            sendMsg(msg.GroupUin, "", "hello world from text");
                        } else {
                            sendMsg("", msg.UserUin, "hello world from text");
                        }
                    }
                } catch (Exception e) {
                    error(e);
                }
            }
        };
        
        EventLibrary.registerHandler(handler, EventLibrary.Priority.HIGH);
        log("Test message handler registered successfully");
    } catch (Exception e) {
        error(e);
    }
}

registerTestHandler();
log("text.java loaded successfully");
```

## 常见问题

### 1. 脚本加载失败

- **检查语法**：确保脚本语法符合QStory要求，无注解、无修饰符
- **检查依赖**：确保所有依赖的外部库已正确加载
- **查看日志**：查看debug.log文件，了解具体错误信息

### 2. 事件处理器不生效

- **检查注册**：确保处理器已正确注册到EventLibrary
- **检查逻辑**：确保处理器的逻辑正确
- **查看日志**：查看debug.log文件，了解处理器注册和执行情况

### 3. 主脚本无响应

- **检查冲突**：确保没有其他脚本覆盖了回调方法
- **检查日志**：查看debug.log文件，了解主脚本执行情况
- **检查权限**：确保发送命令的用户有管理员权限

## 技术栈

- **开发语言**：Java
- **运行环境**：QStory脚本环境（JDK 9）
- **核心库**：EventLibrary（事件分发）
- **工具库**：MyLibrary、AdvancedLibrary

## 贡献

欢迎提交Issue和Pull Request，帮助改进这个项目！

## 许可证

MIT License

## 更新日志

- **2026-02-23**：完成事件分发机制的全面实现，支持所有QStory事件类型
- **2026-02-21**：初始版本，实现基础的热加载和事件分发功能

---

**作者**：氢氧根
**项目地址**：https://github.com/zhch223/qstory-script
**文档**：DEVELOPMENT.md（详细开发文档）
