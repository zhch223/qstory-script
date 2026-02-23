// text.java
// 功能：收到消息"text"时回复"hello world"
// 使用main.java提供的全局方法

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
