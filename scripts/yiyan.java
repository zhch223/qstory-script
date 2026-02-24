// yiyan.java
// 功能：处理/一言命令，获取随机一言并发送到群

// 一言消息处理器类
class YiyanMessageHandler {
    public void onMessage(Object msg) {
        try {
            log("Yiyan handler received message: " + (msg.MessageContent != null ? msg.MessageContent : "null"));    

            // 检查消息内容是否为"/一言"
            if (msg.MessageContent != null && msg.MessageContent.trim().equals("/一言")) {
                log("Yiyan handler processing yiyan command");
                
                // 请求API获取一言
                String apiUrl = "https://api.xunhuisi.store/API/Ranyen/Ranyen.php?type=json";
                String response = httpGet(apiUrl);
                
                if (response != null && !response.isEmpty()) {
                    // 解析JSON
                    String quote = parseQuoteFromJson(response);
                    
                    if (quote != null && !quote.isEmpty()) {
                        // 构建消息，包含@使用者
                        String atUser = "@" + msg.UserUin;
                        String message = atUser + "\n" + quote;
                        
                        // 群聊时发到群
                        if (msg.IsGroup) {
                            sendMsg(msg.GroupUin, "", message);
                        } else {
                            // 私聊时直接发送
                            sendMsg("", msg.UserUin, quote);
                        }
                    } else {
                        log("Failed to parse quote from JSON response");
                        if (msg.IsGroup) {
                            sendMsg(msg.GroupUin, "", "获取一言失败，请稍后再试");
                        } else {
                            sendMsg("", msg.UserUin, "获取一言失败，请稍后再试");
                        }
                    }
                } else {
                    log("Failed to get response from API");
                    if (msg.IsGroup) {
                        sendMsg(msg.GroupUin, "", "获取一言失败，请稍后再试");
                    } else {
                        sendMsg("", msg.UserUin, "获取一言失败，请稍后再试");
                    }
                }
            }
        } catch (Exception e) {
            error(e);
            log("Error in yiyan handler: " + e.getMessage());
        }
    }
    
    // 从JSON中解析一言内容
    private String parseQuoteFromJson(String json) {
        try {
            // 简单的JSON解析，提取quote字段
            int quoteStart = json.indexOf("quote") + 7;
            int quoteEnd = json.indexOf("}", quoteStart);
            if (quoteStart > 6 && quoteEnd > quoteStart) {
                String quote = json.substring(quoteStart, quoteEnd - 1);
                // 去除引号
                if (quote.startsWith("\"")) {
                    quote = quote.substring(1);
                }
                if (quote.endsWith("\"")) {
                    quote = quote.substring(0, quote.length() - 1);
                }
                return quote;
            }
        } catch (Exception e) {
            error(e);
            log("Error parsing JSON: " + e.getMessage());
        }
        return null;
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
