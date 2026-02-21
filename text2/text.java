// text.java
// 功能：收到消息"测试"时回复"hello world"

// 注册消息监听器
class TestModule implements EventDispatcher.MessageListener {
    static {
        // 注册监听器
        EventDispatcher.registerListener(new TestModule());
    }

    @Override
    public void onMessage(Object msg) {
        // 检查消息内容是否为"测试"
        if (msg.MessageContent != null && msg.MessageContent.trim().equals("测试")) {
            // 群聊时发到群，私聊时发回给个人
            if (msg.IsGroup) {
                sendMsg(msg.GroupUin, "", "hello world");
            } else {
                sendMsg("", msg.UserUin, "hello world");
            }
        }
    }
}

// 初始化模块
new TestModule();