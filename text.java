// text.java
// 功能：收到消息"测试"时回复"hello world"
// 使用EventLibrary避免onMsg冲突

// 注册测试消息处理
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
