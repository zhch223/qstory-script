// text.java
// 功能：收到消息"测试"时回复"hello world"
// 使用EventLibrary避免onMsg冲突

// 注册测试消息处理
void registerTestHandler() {
    try {
        log("Starting to register test handler");
        
        // 创建并注册消息处理器
        EventLibrary.MessageHandler handler = new EventLibrary.MessageHandler() {
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
        
        // 注册处理器（使用高优先级）
        EventLibrary.registerHandler(handler, EventLibrary.PRIORITY_HIGH);
        log("Test message handler registered successfully with high priority");
        
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

// 测试处理器优先级
void registerLowPriorityHandler() {
    try {
        EventLibrary.MessageHandler lowPriorityHandler = new EventLibrary.MessageHandler() {
            public void handle(Object msg) {
                try {
                    if (msg.MessageContent != null && msg.MessageContent.trim().equals("测试")) {
                        log("Low priority handler received test message");
                    }
                } catch (Exception e) {
                    error(e);
                }
            }
        };
        
        EventLibrary.registerHandler(lowPriorityHandler, EventLibrary.PRIORITY_LOW);
        log("Low priority handler registered successfully");
    } catch (Exception e) {
        error(e);
        log("Failed to register low priority handler: " + e.getMessage());
    }
}

// 注册低优先级处理器
registerLowPriorityHandler();
