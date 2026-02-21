// main.java
// 功能：热加载Java代码 + 持久化加载列表 + 动态管理员白名单 + 静默保存
// 命令（仅管理员可用）：
//   /加权 QQ号          - 将指定QQ加入管理员白名单
//   /去权 QQ号          - 将指定QQ从管理员白名单移除
//   /保存 文件名.java    - 静默等待下一条消息作为代码保存（保存后仅回复"已保存"）
//   /存储 文件名.java    - 同上（兼容旧命令）
//   /编辑 文件名.java    - 显示当前内容，等待新内容覆盖
//   /加载 文件名.java    - 立即加载
//   /保持 文件名.java    - 加入持久化列表并加载
//   /取消保持 文件名.java - 从持久化列表移除
//   /取消              - 放弃当前等待状态
//   /列表              - 查看持久化列表

import android.content.Context;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.List;

// 全局变量（由框架提供）
// String myUin;
// Context context;
// String appPath;
// ClassLoader loader;
// String pluginID;

// 存储等待状态的键前缀
String STATE_PREFIX = "hotload_state_";
String FILE_PREFIX = "hotload_file_";
// 持久化列表的配置名和键名
String PERSIST_CONFIG = "hotload_persist";
String PERSIST_KEY = "files";
// 管理员列表配置
String ADMIN_CONFIG = "admin_list";
String ADMIN_KEY = "admins";

// 状态常量
int STATE_NONE = 0;
int STATE_WAIT_CREATE = 1;
int STATE_WAIT_EDIT = 2;

// ==================== 初始化与权限管理 ====================
void onLoad() {
    ensureAdmin(myUin);
    loadExternalLibrary();
    loadPersistedFiles();
}

void loadExternalLibrary() {
    try {
        log("Starting to load external libraries");
        
        // 加载核心库
        loadJar(appPath + "/EventLibrary.jar");
        log("EventLibrary.jar loaded");
        
        // 加载其他库
        loadJar(appPath + "/MyLibrary.jar");
        log("MyLibrary.jar loaded");
        
        loadJar(appPath + "/AdvancedLibrary.jar");
        log("AdvancedLibrary.jar loaded");
        
        log("External libraries loaded successfully");
    } catch (Exception e) {
        error(e);
        log("Failed to load external library: " + e.getMessage());
    }
}

void ensureAdmin(String qq) {
    Set<String> admins = getAdminSet();
    if (!admins.contains(qq)) {
        admins.add(qq);
        saveAdminSet(admins);
        log("已将 " + qq + " 添加为默认管理员");
    }
}

Set<String> getAdminSet() {
    String str = getString(ADMIN_CONFIG, ADMIN_KEY, "");
    Set<String> set = new HashSet<>();
    if (!str.isEmpty()) {
        String[] arr = str.split(",");
        for (String s : arr) {
            if (!s.trim().isEmpty()) set.add(s.trim());
        }
    }
    return set;
}

void saveAdminSet(Set<String> set) {
    String str = String.join(",", set);
    putString(ADMIN_CONFIG, ADMIN_KEY, str);
}

boolean isAdmin(String qq) {
    return getAdminSet().contains(qq);
}

// ==================== 持久化管理 ====================
void loadPersistedFiles() {
    String filesStr = getString(PERSIST_CONFIG, PERSIST_KEY, "");
    if (filesStr.isEmpty()) return;
    String[] files = filesStr.split(",");
    for (String file : files) {
        if (file.trim().isEmpty()) continue;
        String filePath = appPath + "/" + file.trim();
        try {
            load(filePath);
            log("自动加载持久化文件: " + file);
        } catch (Exception e) {
            error(e);
            log("自动加载失败: " + file + " - " + e.getMessage());
        }
    }
}

void addToPersistList(String fileName, Object msg) {
    String filesStr = getString(PERSIST_CONFIG, PERSIST_KEY, "");
    ArrayList<String> list = new ArrayList<>();
    if (!filesStr.isEmpty()) {
        String[] arr = filesStr.split(",");
        for (String s : arr) {
            if (!s.trim().isEmpty()) list.add(s.trim());
        }
    }
    if (list.contains(fileName)) {
        sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "文件 " + fileName + " 已在持久化列表中。");
        return;
    }
    String filePath = appPath + "/" + fileName;
    if (!fileExists(filePath)) {
        sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "文件 " + fileName + " 不存在，请先创建。");
        return;
    }
    list.add(fileName);
    String newStr = String.join(",", list);
    putString(PERSIST_CONFIG, PERSIST_KEY, newStr);
    try {
        load(filePath);
        sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "文件 " + fileName + " 已加入持久化列表并已加载。");
    } catch (Exception e) {
        error(e);
        sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "文件 " + fileName + " 加入持久化列表，但加载失败：" + e.getMessage());
    }
}

void removeFromPersistList(String fileName, Object msg) {
    String filesStr = getString(PERSIST_CONFIG, PERSIST_KEY, "");
    if (filesStr.isEmpty()) {
        sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "持久化列表为空。");
        return;
    }
    String[] arr = filesStr.split(",");
    ArrayList<String> list = new ArrayList<>();
    boolean found = false;
    for (String s : arr) {
        if (s.trim().equals(fileName)) {
            found = true;
        } else if (!s.trim().isEmpty()) {
            list.add(s.trim());
        }
    }
    if (!found) {
        sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "文件 " + fileName + " 不在持久化列表中。");
        return;
    }
    String newStr = String.join(",", list);
    if (newStr.isEmpty()) {
        putString(PERSIST_CONFIG, PERSIST_KEY, "");
    } else {
        putString(PERSIST_CONFIG, PERSIST_KEY, newStr);
    }
    sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "文件 " + fileName + " 已从持久化列表中移除。");
}

void showPersistList(Object msg) {
    String filesStr = getString(PERSIST_CONFIG, PERSIST_KEY, "");
    if (filesStr.isEmpty()) {
        sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "持久化加载列表为空。");
    } else {
        String[] files = filesStr.split(",");
        StringBuilder sb = new StringBuilder("当前持久化加载列表：\n");
        for (String f : files) {
            if (!f.trim().isEmpty()) sb.append("- " + f.trim() + "\n");
        }
        sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, sb.toString());
    }
}

void showScriptList(Object msg) {
    try {
        String scriptsDir = appPath + "/scripts";
        java.io.File dir = new java.io.File(scriptsDir);
        java.io.File[] files = dir.listFiles();
        
        if (files == null || files.length == 0) {
            sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "scripts目录为空，暂无脚本。");
            return;
        }
        
        StringBuilder sb = new StringBuilder("当前scripts目录下的脚本：\n");
        int count = 0;
        for (java.io.File file : files) {
            if (file.isFile() && file.getName().endsWith(".java")) {
                sb.append("- " + file.getName() + "\n");
                count++;
            }
        }
        
        if (count == 0) {
            sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "scripts目录下暂无Java脚本文件。");
        } else {
            sb.append("\n共 " + count + " 个脚本文件");
            sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, sb.toString());
        }
    } catch (Exception e) {
        error(e);
        log("Error showing script list: " + e.getMessage());
        sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "获取脚本列表失败：" + e.getMessage());
    }
}

void stopScript(String fileName, Object msg) {
    try {
        // 从持久化列表中移除
        String filesStr = getString(PERSIST_CONFIG, PERSIST_KEY, "");
        if (!filesStr.isEmpty()) {
            String[] arr = filesStr.split(",");
            ArrayList<String> list = new ArrayList<>();
            boolean found = false;
            for (String s : arr) {
                if (s.trim().equals(fileName)) {
                    found = true;
                } else if (!s.trim().isEmpty()) {
                    list.add(s.trim());
                }
            }
            if (found) {
                String newStr = String.join(",", list);
                if (newStr.isEmpty()) {
                    putString(PERSIST_CONFIG, PERSIST_KEY, "");
                } else {
                    putString(PERSIST_CONFIG, PERSIST_KEY, newStr);
                }
            }
        }
        
        // 这里可以添加额外的停止逻辑，比如从EventLibrary中移除对应的处理器
        // 由于我们的事件分发机制是基于注册的处理器，停止脚本可能需要更复杂的逻辑
        // 目前主要是从持久化列表中移除，确保下次不会自动加载
        
        sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "脚本 " + fileName + " 已停止。");
        log("Stopped script: " + fileName);
    } catch (Exception e) {
        error(e);
        log("Error stopping script: " + e.getMessage());
        sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "停止脚本失败：" + e.getMessage());
    }
}

// ==================== 文件管理 ====================
boolean fileExists(String path) {
    try {
        String content = readFileText(path);
        return content != null;
    } catch (Exception e) {
        return false;
    }
}

boolean isValidFileName(String name) {
    if (name == null || name.isEmpty()) return false;
    return name.matches("^[a-zA-Z0-9_.]+\\.java$") && !name.startsWith(".") && !name.contains("..");
}

// ==================== 命令处理 ====================
boolean handleWaitingState(Object msg, String sessionId) {
    int state = getInt(STATE_PREFIX, sessionId, STATE_NONE);
    String fileName = getString(FILE_PREFIX, sessionId, "");

    if (state == STATE_NONE) {
        return false;
    }

    if (fileName.isEmpty()) {
        putInt(STATE_PREFIX, sessionId, STATE_NONE);
        sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "内部错误：文件名为空，已取消操作。");
        return true;
    }

    String filePath;
    if (state == STATE_WAIT_CREATE) {
        // 新建文件时保存到scripts目录
        filePath = appPath + "/scripts/" + fileName;
    } else {
        // 编辑文件时，先尝试在scripts目录查找，再尝试根目录
        filePath = appPath + "/scripts/" + fileName;
        if (!fileExists(filePath)) {
            filePath = appPath + "/" + fileName;
        }
    }

    if (state == STATE_WAIT_CREATE) {
        try {
            writeTextToFile(filePath, msg.MessageContent);
            sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, fileName + " 已保存到scripts目录。");
        } catch (Exception e) {
            error(e);
            sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "保存失败：" + e.getMessage());
        }
        putInt(STATE_PREFIX, sessionId, STATE_NONE);
        putString(FILE_PREFIX, sessionId, "");
    } else if (state == STATE_WAIT_EDIT) {
        if (msg.MessageContent.startsWith("当前文件内容：")) {
            sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "检测到误发送，请直接发送代码内容。若想放弃请发送 /取消");
            return true;
        }
        try {
            writeTextToFile(filePath, msg.MessageContent);
            sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "文件 " + fileName + " 已更新。");
        } catch (Exception e) {
            error(e);
            sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "保存失败：" + e.getMessage());
        }
        putInt(STATE_PREFIX, sessionId, STATE_NONE);
        putString(FILE_PREFIX, sessionId, "");
    }
    return true;
}

void handleCancelCommand(Object msg, String sessionId) {
    int state = getInt(STATE_PREFIX, sessionId, STATE_NONE);
    if (state != STATE_NONE) {
        putInt(STATE_PREFIX, sessionId, STATE_NONE);
        putString(FILE_PREFIX, sessionId, "");
        sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "已取消当前操作。");
    } else {
        sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "当前没有等待中的操作。");
    }
}

boolean handlePermissionCommand(Object msg, String cmd, String arg) {
    if (cmd.equals("/加权")) {
        if (arg.isEmpty() || !arg.matches("\\d+")) {
            sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "用法：/加权 QQ号");
            return true;
        }
        Set<String> admins = getAdminSet();
        if (admins.contains(arg)) {
            sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "该QQ已是管理员。");
        } else {
            admins.add(arg);
            saveAdminSet(admins);
            sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "已将 " + arg + " 添加为管理员。");
        }
        return true;
    } else if (cmd.equals("/去权")) {
        if (arg.isEmpty() || !arg.matches("\\d+")) {
            sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "用法：/去权 QQ号");
            return true;
        }
        Set<String> admins = getAdminSet();
        if (!admins.contains(arg)) {
            sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "该QQ不是管理员。");
        } else if (arg.equals(myUin)) {
            sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "不能移除自己（脚本作者）的管理员权限。");
        } else {
            admins.remove(arg);
            saveAdminSet(admins);
            sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "已将 " + arg + " 移除管理员。");
        }
        return true;
    }
    return false;
}

boolean handleFileCommand(Object msg, String sessionId, String cmd, String arg) {
    if (cmd.equals("/保存") || cmd.equals("/存储")) {
        if (arg.isEmpty() || !arg.endsWith(".java")) {
            sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "用法：/保存 文件名.java （必须以.java结尾）");
            return true;
        }
        if (!isValidFileName(arg)) {
            sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "文件名包含非法字符，只允许字母、数字、下划线、点。");
            return true;
        }
        putInt(STATE_PREFIX, sessionId, STATE_WAIT_CREATE);
        putString(FILE_PREFIX, sessionId, arg);
        return true;
    } else if (cmd.equals("/编辑")) {
        if (arg.isEmpty() || !arg.endsWith(".java")) {
            sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "用法：/编辑 文件名.java （必须以.java结尾）");
            return true;
        }
        if (!isValidFileName(arg)) {
            sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "文件名包含非法字符。");
            return true;
        }
        // 尝试在scripts目录中查找
        String filePath = appPath + "/scripts/" + arg;
        if (!fileExists(filePath)) {
            // 如果scripts目录中不存在，尝试在根目录查找
            filePath = appPath + "/" + arg;
        }
        String contentStr = readFileText(filePath);
        if (contentStr == null) {
            sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "文件不存在或无法读取：" + arg);
            return true;
        }
        sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "当前文件内容：\n" + contentStr + "\n请发送新的代码内容覆盖（发送 /取消 可放弃）：");
        putInt(STATE_PREFIX, sessionId, STATE_WAIT_EDIT);
        putString(FILE_PREFIX, sessionId, arg);
        return true;
    } else if (cmd.equals("/加载")) {
        if (arg.isEmpty() || !arg.endsWith(".java")) {
            sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "用法：/加载 文件名.java （必须以.java结尾）");
            return true;
        }
        if (!isValidFileName(arg)) {
            sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "文件名包含非法字符。");
            return true;
        }
        // 尝试在scripts目录中查找
        String filePath = appPath + "/scripts/" + arg;
        if (!fileExists(filePath)) {
            // 如果scripts目录中不存在，尝试在根目录查找
            filePath = appPath + "/" + arg;
        }
        try {
            load(filePath);
            sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "文件 " + arg + " 已加载。");
        } catch (Exception e) {
            error(e);
            sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "加载失败：" + e.getMessage());
        }
        return true;
    }
    return false;
}

boolean handlePersistCommand(Object msg, String cmd, String arg) {
    if (cmd.equals("/保持")) {
        if (arg.isEmpty() || !arg.endsWith(".java")) {
            sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "用法：/保持 文件名.java （必须以.java结尾）");
            return true;
        }
        if (!isValidFileName(arg)) {
            sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "文件名包含非法字符。");
            return true;
        }
        addToPersistList(arg, msg);
        return true;
    } else if (cmd.equals("/取消保持")) {
        if (arg.isEmpty() || !arg.endsWith(".java")) {
            sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "用法：/取消保持 文件名.java （必须以.java结尾）");
            return true;
        }
        if (!isValidFileName(arg)) {
            sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "文件名包含非法字符。");
            return true;
        }
        removeFromPersistList(arg, msg);
        return true;
    } else if (cmd.equals("/列表")) {
        showPersistList(msg);
        return true;
    } else if (cmd.equals("/脚本列表")) {
        showScriptList(msg);
        return true;
    } else if (cmd.equals("/stop")) {
        if (arg.isEmpty() || !arg.endsWith(".java")) {
            sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "用法：/stop 文件名.java （必须以.java结尾）");
            return true;
        }
        if (!isValidFileName(arg)) {
            sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "文件名包含非法字符。");
            return true;
        }
        stopScript(arg, msg);
        return true;
    }
    return false;
}

boolean handleCommand(Object msg, String sessionId) {
    String content = msg.MessageContent;
    if (content == null || !content.startsWith("/")) {
        return false;
    }

    if (!isAdmin(msg.UserUin)) {
        return true;
    }

    String[] parts = content.split("\\s+", 2);
    String cmd = parts[0].toLowerCase();
    String arg = parts.length > 1 ? parts[1].trim() : "";

    if (cmd.equals("/取消")) {
        handleCancelCommand(msg, sessionId);
        return true;
    }

    if (handlePermissionCommand(msg, cmd, arg)) {
        return true;
    }

    if (handleFileCommand(msg, sessionId, cmd, arg)) {
        return true;
    }

    if (handlePersistCommand(msg, cmd, arg)) {
        return true;
    }

    return false;
}

void testAdvancedLibrary(Object msg, String content) {
    try {
        Object messageObj = AdvancedLibrary.createMessage(content, msg.UserUin);
        Object dispatcher = AdvancedLibrary.createEventDispatcher();
        Object handler = AdvancedLibrary.createDefaultHandler("TestHandler");
        Object messageService = AdvancedLibrary.MessageService.getInstance();
        String formattedMsg = AdvancedLibrary.formatMessage(messageObj);
        boolean isEmpty = AdvancedLibrary.isEmptyMessage(messageObj);
        
        String result = "Advanced Library Test Results:\n";
        result += "Original Content: " + content + "\n";
        result += "Formatted Message: " + formattedMsg + "\n";
        result += "Is Empty Message: " + isEmpty + "\n";
        result += "\n=== Advanced Features Tested ===\n";
        result += "✓ Inner Classes (Message, EventDispatcher)\n";
        result += "✓ Interfaces (MessageHandler)\n";
        result += "✓ Singleton Pattern (MessageService)\n";
        result += "✓ Factory Methods\n";
        result += "✓ Complex OOP Features\n";
        
        if (msg.IsGroup) {
            sendMsg(msg.GroupUin, "", result);
        } else {
            sendMsg("", msg.UserUin, result);
        }
        
        log("Advanced library test completed successfully");
    } catch (Exception e) {
        error(e);
        log("Error testing advanced library: " + e.getMessage());
        if (msg.IsGroup) {
            sendMsg(msg.GroupUin, "", "Advanced library test failed: " + e.getMessage());
        } else {
            sendMsg("", msg.UserUin, "Advanced library test failed: " + e.getMessage());
        }
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
        
        // 分发消息给注册的处理器
        try {
            log("Dispatching message to registered handlers");
            EventLibrary.dispatchMessage(msg);
        } catch (Exception e) {
            error(e);
            log("Error dispatching message: " + e.getMessage());
        }
        
        // 测试外部库
        try {
            String processed = MyLibrary.processMessage(content);
            int length = MyLibrary.getMessageLength(content);
            String reversed = MyLibrary.reverseMessage(content);
            boolean isTest = MyLibrary.isTestMessage(content);
            
            if (content.startsWith("/library")) {
                String result = "Library Test Results:\n";
                result += "Original: " + content + "\n";
                result += "Processed: " + processed + "\n";
                result += "Length: " + length + "\n";
                result += "Reversed: " + reversed + "\n";
                result += "Is Test: " + isTest;
                
                if (msg.IsGroup) {
                    sendMsg(msg.GroupUin, "", result);
                } else {
                    sendMsg("", msg.UserUin, result);
                }
            }
            
            if (content.startsWith("/advanced")) {
                testAdvancedLibrary(msg, content);
            }
        } catch (Exception e) {
            error(e);
            log("Error using external library: " + e.getMessage());
        }
        
        log("Main script onMsg completed successfully");
    } catch (Exception e) {
        error(e);
        log("Critical error in main onMsg: " + e.getMessage());
    }
}
