// fangzhan.java
// 功能：处理/仿站抓取命令，调用API进行网站抓取并返回结果

// 仿站抓取消息处理器类
class FangzhanMessageHandler {
    public void onMessage(Object msg) {
        try {
            log("Fangzhan handler received message: " + (msg.MessageContent != null ? msg.MessageContent : "null"));    

            // 检查消息内容是否为"/仿站抓取"命令
            if (msg.MessageContent != null && msg.MessageContent.trim().startsWith("/仿站抓取")) {
                log("Fangzhan handler processing fangzhan command");
                
                // 解析命令参数，提取URL
                String content = msg.MessageContent.trim();
                if (content.length() > 5) {
                    String url = content.substring(5).trim();
                    if (!url.isEmpty()) {
                        // 调用API进行网站抓取
                        String apiUrl = "https://api.lolimi.cn/API/baz/api?url=" + url;
                        log("Calling API: " + apiUrl);
                        String response = httpGet(apiUrl);
                        
                        if (response != null && !response.isEmpty()) {
                            // 解析JSON响应
                            String result = parseFangzhanResult(response);
                            
                            // 构建消息，包含@使用者
                            String atUser = "@" + msg.UserUin;
                            String message = atUser + "\n" + result;
                            
                            // 发送消息
                            if (msg.IsGroup) {
                                sendMsg(msg.GroupUin, "", message);
                            } else {
                                sendMsg("", msg.UserUin, result);
                            }
                        } else {
                            log("Failed to get response from API");
                            String errorMsg = "@" + msg.UserUin + "\n获取仿站数据失败，请稍后再试";
                            if (msg.IsGroup) {
                                sendMsg(msg.GroupUin, "", errorMsg);
                            } else {
                                sendMsg("", msg.UserUin, "获取仿站数据失败，请稍后再试");
                            }
                        }
                    } else {
                        log("No URL provided");
                        String errorMsg = "@" + msg.UserUin + "\n请提供要抓取的网址，格式：/仿站抓取 https://example.com";
                        if (msg.IsGroup) {
                            sendMsg(msg.GroupUin, "", errorMsg);
                        } else {
                            sendMsg("", msg.UserUin, "请提供要抓取的网址，格式：/仿站抓取 https://example.com");
                        }
                    }
                } else {
                    log("No URL provided");
                    String errorMsg = "@" + msg.UserUin + "\n请提供要抓取的网址，格式：/仿站抓取 https://example.com";
                    if (msg.IsGroup) {
                        sendMsg(msg.GroupUin, "", errorMsg);
                    } else {
                        sendMsg("", msg.UserUin, "请提供要抓取的网址，格式：/仿站抓取 https://example.com");
                    }
                }
            }
        } catch (Exception e) {
            error(e);
            log("Error in fangzhan handler: " + e.getMessage());
        }
    }
    
    // 解析仿站抓取结果 - 自动格式化JSON
    private String parseFangzhanResult(String json) {
        try {
            // 自动格式化JSON
            String formattedJson = formatJson(json);
            return formattedJson;
        } catch (Exception e) {
            error(e);
            log("Error parsing JSON: " + e.getMessage());
            return "{\"code\": 500, \"success\": false, \"msg\": \"解析结果失败，请稍后再试\"}";
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
            return json; // 格式化失败时返回原始JSON
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
FangzhanMessageHandler fangzhanHandler = new FangzhanMessageHandler();

// 注册到main.java的消息处理器列表
registerScriptMessageHandler(fangzhanHandler);

// 注册脚本
registerScript("fangzhan", fangzhanHandler);

log("fangzhan.java loaded successfully - using global methods");

// 测试全局方法
logGlobal("Fangzhan script initialized");