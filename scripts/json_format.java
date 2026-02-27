// json_format.java
// 功能：处理/json格式化命令，格式化JSON文本并返回

// JSON格式化消息处理器类
class JsonFormatMessageHandler {
    public void onMessage(Object msg) {
        try {
            log("JsonFormat handler received message: " + (msg.MessageContent != null ? msg.MessageContent : "null"));    

            // 检查消息内容是否为"/json格式化"命令
            if (msg.MessageContent != null && msg.MessageContent.trim().startsWith("/json格式化")) {
                log("JsonFormat handler processing json format command");
                
                // 解析命令参数，提取JSON文本
                String content = msg.MessageContent.trim();
                if (content.length() > 5) {
                    String jsonText = content.substring(5).trim();
                    if (!jsonText.isEmpty()) {
                        log("Received JSON text: " + jsonText.substring(0, Math.min(100, jsonText.length())) + "...");
                        
                        // 格式化JSON
                        String formattedJson = formatJson(jsonText);
                        
                        // 构建消息，包含@使用者
                        String atUser = "@" + msg.UserUin;
                        String message = atUser + "\n" + formattedJson;
                        
                        // 发送消息
                        if (msg.IsGroup) {
                            sendMsg(msg.GroupUin, "", message);
                        } else {
                            sendMsg("", msg.UserUin, formattedJson);
                        }
                    } else {
                        log("No JSON text provided");
                        String errorMsg = "@" + msg.UserUin + "\n请提供要格式化的JSON文本，格式：/json格式化 {\"key\": \"value\"}";
                        if (msg.IsGroup) {
                            sendMsg(msg.GroupUin, "", errorMsg);
                        } else {
                            sendMsg("", msg.UserUin, "请提供要格式化的JSON文本，格式：/json格式化 {\"key\": \"value\"}");
                        }
                    }
                } else {
                    log("No JSON text provided");
                    String errorMsg = "@" + msg.UserUin + "\n请提供要格式化的JSON文本，格式：/json格式化 {\"key\": \"value\"}";
                    if (msg.IsGroup) {
                        sendMsg(msg.GroupUin, "", errorMsg);
                    } else {
                        sendMsg("", msg.UserUin, "请提供要格式化的JSON文本，格式：/json格式化 {\"key\": \"value\"}");
                    }
                }
            }
        } catch (Exception e) {
            error(e);
            log("Error in json format handler: " + e.getMessage());
        }
    }
    
    // 格式化JSON文本
    private String formatJson(String json) {
        try {
            log("Starting to format JSON");
            StringBuilder result = new StringBuilder();
            int indent = 0;
            boolean inQuotes = false;
            boolean escapeNext = false;
            
            for (int i = 0; i < json.length(); i++) {
                char c = json.charAt(i);
                
                if (escapeNext) {
                    result.append(c);
                    escapeNext = false;
                    continue;
                }
                
                if (c == '\\') {
                    result.append(c);
                    escapeNext = true;
                    continue;
                }
                
                if (c == '"') {
                    inQuotes = !inQuotes;
                    result.append(c);
                } else if (!inQuotes) {
                    switch (c) {
                        case '{':
                        case '[':
                            result.append(c);
                            result.append('\n');
                            indent++;
                            addIndent(result, indent);
                            break;
                        case '}':
                        case ']':
                            result.append('\n');
                            indent--;
                            addIndent(result, indent);
                            result.append(c);
                            break;
                        case ',':
                            result.append(c);
                            result.append('\n');
                            addIndent(result, indent);
                            break;
                        case ':':
                            result.append(c);
                            result.append(' ');
                            break;
                        case ' ': 
                        case '\t':
                        case '\n':
                        case '\r':
                            // 忽略空白字符，由我们自己控制格式
                            break;
                        default:
                            result.append(c);
                    }
                } else {
                    result.append(c);
                }
            }
            
            log("JSON formatting completed");
            return result.toString();
        } catch (Exception e) {
            error(e);
            log("Error formatting JSON: " + e.getMessage());
            return "❌ JSON格式化失败：" + e.getMessage();
        }
    }
    
    // 添加缩进
    private void addIndent(StringBuilder sb, int indent) {
        for (int i = 0; i < indent; i++) {
            sb.append("    "); // 4个空格作为缩进
        }
    }
}

// 创建处理器实例
JsonFormatMessageHandler jsonFormatHandler = new JsonFormatMessageHandler();

// 注册到main.java的消息处理器列表
registerScriptMessageHandler(jsonFormatHandler);

// 注册脚本
registerScript("json_format", jsonFormatHandler);

log("json_format.java loaded successfully - using global methods");

// 测试全局方法
logGlobal("JsonFormat script initialized");
