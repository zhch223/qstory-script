// yiyan.java
// 功能：响应/一言命令，调用API获取随机一言并发送到群里@使用者

// 一言消息处理器类
class YiyanMessageHandler {
    public void onMessage(Object msg) {
        try {
            log("Yiyan handler received message: " + (msg.MessageContent != null ? msg.MessageContent : "null"));

            // 检查消息内容是否为"/一言"
            if (msg.MessageContent != null && msg.MessageContent.trim().equals("/一言")) {
                log("Yiyan handler processing yiyan command");
                
                // 调用API获取一言
                String apiUrl = "https://api.xunhuisi.store/API/Ranyen/Ranyen.php?type=json";
                String response = httpGet(apiUrl);
                log("API response: " + response);
                
                // 解析JSON，提取quote字段
                String quote = parseQuoteFromJson(response);
                
                // 构建消息，包含@使用者
                String message = "@" + msg.UserUin + "\n" + quote;
                
                // 群聊时发到群，私聊时发回给个人
                if (msg.IsGroup) {
                    sendMsg(msg.GroupUin, "", message);
                } else {
                    sendMsg("", msg.UserUin, quote);
                }
            }
        } catch (Exception e) {
            error(e);
            log("Error in yiyan handler: " + e.getMessage());
        }
    }
    
    // 解析JSON，提取quote字段
    private String parseQuoteFromJson(String json) {
        try {
            // 简单的JSON解析，提取quote字段
            int quoteStart = json.indexOf('"quote":"') + 8;
            int quoteEnd = json.indexOf('"', quoteStart);
            if (quoteStart > 8 && quoteEnd > quoteStart) {
                return json.substring(quoteStart, quoteEnd);
            }
            return "获取一言失败，请稍后再试";
        } catch (Exception e) {
            error(e);
            log("Error parsing JSON: " + e.getMessage());
            return "解析一言失败，请稍后再试";
        }
    }
}

// 创建处理器实例
YiyanMessageHandler yiyanHandler = new YiyanMessageHandler();

// 注册到main.java的消息处理器列表
registerScriptMessageHandler(yiyanHandler);

// 注册脚本
registerScript("yiyan", yiyanHandler);

log("yiyan.java loaded successfully - using global methods");

// 测试全局方法
logGlobal("Yiyan script initialized");