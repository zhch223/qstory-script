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


String STATE_PREFIX = "hotload_state_";
String FILE_PREFIX = "hotload_file_";
String PERSIST_CONFIG = "hotload_persist";
String PERSIST_KEY = "files";
String ADMIN_CONFIG = "admin_list";
String ADMIN_KEY = "admins";

int STATE_NONE = 0;
int STATE_WAIT_CREATE = 1;
int STATE_WAIT_EDIT = 2;

// 全局方法集合，供其他脚本调用
// 注册消息处理器
void registerMessageHandler(String name, Object handler) {
    // 这里可以实现简单的处理器注册逻辑
    log("Registered message handler: " + name);
}

// 发送消息的全局方法
void sendGlobalMessage(String groupUin, String userUin, String content) {
    sendMsg(groupUin, userUin, content);
}

// 日志记录的全局方法
void logGlobal(String message) {
    log(message);
}

// 错误处理的全局方法
void errorGlobal(Exception e) {
    error(e);
}

// 检查是否为管理员的全局方法
boolean isGlobalAdmin(String qq) {
    return isAdmin(qq);
}

// 脚本注册方法，供其他脚本在加载时调用
void registerScript(String scriptName, Object scriptObject) {
    log("Script registered: " + scriptName);
}

void onLoad() {
    ensureAdmin(myUin);
    
    // 优先加载外部库，确保所有JAR文件在脚本加载前准备就绪
    try {
        log("=== Loading external libraries ===");
        loadExternalLibrary();
        log("=== External libraries loaded successfully ===");
    } catch (Exception e) {
        error(e);
        log("Error loading external libraries: " + e.getMessage());
    }
    
    // 延迟一秒，确保JAR文件完全加载
    try {
        Thread.sleep(1000);
        log("Waiting for JAR files to initialize...");
    } catch (Exception e) {
        // 忽略睡眠异常
    }
    
    // 加载持久化脚本 - 放在EventLibrary验证之前，确保脚本优先加载
    try {
        log("=== Loading persisted scripts ===");
        loadPersistedFiles();
        log("=== Persisted scripts loaded successfully ===");
    } catch (Exception e) {
        error(e);
        log("Error loading persisted scripts: " + e.getMessage());
    }
    
    // 验证EventLibrary是否加载成功
    try {
        log("=== Verifying EventLibrary ===");
        int handlerCount = EventLibrary.getTotalHandlerCount();
        log("EventLibrary loaded successfully, total handlers: " + handlerCount);
    } catch (Error e) {
        error(e);
        log("EventLibrary not available (Error): " + e.getMessage());
        log("Continuing without EventLibrary...");
    } catch (Exception e) {
        error(e);
        log("EventLibrary not available (Exception): " + e.getMessage());
        log("Continuing without EventLibrary...");
    }
    
    // 分发加载事件
    try {
        log("Dispatching load event to registered handlers");
        EventLibrary.dispatchLoad();
    } catch (Exception e) {
        error(e);
        log("Error dispatching load event: " + e.getMessage());
    }
}

void loadExternalLibrary() {
    try {
        log("Starting to load external libraries");
        
        // 加载核心库 - EventLibrary.jar
        String eventLibraryPath = appPath + "/EventLibrary.jar";
        log("Checking EventLibrary.jar at: " + eventLibraryPath);
        if (fileExists(eventLibraryPath)) {
            try {
                log("Attempting to load EventLibrary.jar...");
                loadJar(eventLibraryPath);
                log("✓ EventLibrary.jar loaded successfully");
            } catch (Exception e) {
                error(e);
                log("✗ Failed to load EventLibrary.jar: " + e.getMessage());
            }
        } else {
            log("✗ EventLibrary.jar not found at: " + eventLibraryPath);
        }
        
        // 加载其他库
        String myLibraryPath = appPath + "/MyLibrary.jar";
        log("Checking MyLibrary.jar at: " + myLibraryPath);
        if (fileExists(myLibraryPath)) {
            try {
                log("Attempting to load MyLibrary.jar...");
                loadJar(myLibraryPath);
                log("✓ MyLibrary.jar loaded successfully");
            } catch (Exception e) {
                error(e);
                log("✗ Failed to load MyLibrary.jar: " + e.getMessage());
            }
        } else {
            log("✗ MyLibrary.jar not found at: " + myLibraryPath);
        }
        
        String advancedLibraryPath = appPath + "/AdvancedLibrary.jar";
        log("Checking AdvancedLibrary.jar at: " + advancedLibraryPath);
        if (fileExists(advancedLibraryPath)) {
            try {
                log("Attempting to load AdvancedLibrary.jar...");
                loadJar(advancedLibraryPath);
                log("✓ AdvancedLibrary.jar loaded successfully");
            } catch (Exception e) {
                error(e);
                log("✗ Failed to load AdvancedLibrary.jar: " + e.getMessage());
            }
        } else {
            log("✗ AdvancedLibrary.jar not found at: " + advancedLibraryPath);
        }
        
        log("External libraries loading completed");
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

void loadPersistedFiles() {
    log("开始加载持久化脚本");
    
    // 首先确保scripts目录存在
    String scriptsDir = getScriptsDir();
    log("Scripts目录路径: " + scriptsDir);
    
    try {
        // 尝试读取scripts目录，检查是否存在
        log("检查scripts目录是否存在");
        String content = readFileText(scriptsDir);
        log("读取scripts目录结果: " + (content != null ? "成功" : "失败"));
        if (content == null) {
            log("scripts目录不存在，创建scripts目录");
            // 这里我们无法直接创建目录，但可以通过创建一个临时文件来间接创建目录
            String tempFile = scriptsDir + "/temp.txt";
            log("尝试创建临时文件: " + tempFile);
            writeTextToFile(tempFile, "");
            log("已创建scripts目录");
        }
    } catch (Exception e) {
        error(e);
        log("检查scripts目录失败: " + e.getMessage());
    }
    
    // 从脚本目录加载列表文件
    String loadListPath = scriptsDir + "/load_list.txt";
    log("加载列表文件路径: " + loadListPath);
    
    try {
        log("检查加载列表文件是否存在");
        boolean exists = fileExists(loadListPath);
        log("加载列表文件存在: " + exists);
        
        if (exists) {
            String loadListContent = readFileText(loadListPath);
            log("加载列表文件内容: " + (loadListContent != null ? loadListContent : "null"));
            if (loadListContent != null && !loadListContent.isEmpty()) {
                String[] files = loadListContent.split("\\n");
                log("加载列表文件中的文件数量: " + files.length);
                for (String file : files) {
                    String fileName = file.trim();
                    if (fileName.isEmpty()) continue;
                    String filePath = scriptsDir + "/" + fileName;
                    try {
                        // 检查文件是否存在
                        if (fileExists(filePath)) {
                            log("加载文件: " + fileName);
                            load(filePath);
                            log("从加载列表加载文件: " + fileName);
                        } else {
                            log("跳过不存在的文件: " + fileName);
                        }
                    } catch (Exception e) {
                        error(e);
                        log("加载文件失败: " + fileName + " - " + e.getMessage());
                    }
                }
            }
        } else {
            log("加载列表文件不存在，创建默认加载列表");
            // 创建默认加载列表
            log("尝试创建默认加载列表文件: " + loadListPath);
            writeTextToFile(loadListPath, "text.java\nyiyan.java");
            log("已创建默认加载列表文件");
        }
    } catch (Exception e) {
        error(e);
        log("读取加载列表失败: " + e.getMessage());
    }
    
    log("持久化脚本加载完成");
    
    // 自动加载scripts目录中的其他java文件（不在加载列表中的文件）
    try {
        String content = readFileText(scriptsDir);
        if (content != null) {
            String[] lines = content.split("\\n");
            for (String line : lines) {
                String fileName = line.trim();
                if (fileName.endsWith(".java")) {
                    // 检查是否在加载列表中
                    boolean inLoadList = false;
                    try {
                        String loadListContent = readFileText(loadListPath);
                        if (loadListContent != null) {
                            inLoadList = loadListContent.contains(fileName);
                        }
                    } catch (Exception e) {
                        // 忽略读取加载列表的错误
                    }
                    
                    if (!inLoadList) {
                        String filePath = scriptsDir + "/" + fileName;
                        try {
                            if (fileExists(filePath)) {
                                load(filePath);
                                log("自动加载scripts目录中的文件: " + fileName);
                            }
                        } catch (Exception e) {
                            error(e);
                            log("加载文件失败: " + fileName + " - " + e.getMessage());
                        }
                    }
                }
            }
        }
    } catch (Exception e) {
        error(e);
        log("读取scripts目录失败: " + e.getMessage());
    }
}

void addToPersistList(String fileName, Object msg) {
    // 更新内存中的持久化列表
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
    String filePath = getScriptsDir() + "/" + fileName;
    if (!fileExists(filePath)) {
        sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "文件 " + fileName + " 不存在，请先创建。");
        return;
    }
    list.add(fileName);
    String newStr = String.join(",", list);
    putString(PERSIST_CONFIG, PERSIST_KEY, newStr);
    
    // 更新脚本目录中的加载列表文件
    try {
        String loadListPath = getScriptsDir() + "/load_list.txt";
        String loadListContent = "";
        if (fileExists(loadListPath)) {
            loadListContent = readFileText(loadListPath);
        }
        if (!loadListContent.contains(fileName)) {
            if (!loadListContent.isEmpty()) {
                loadListContent += "\n";
            }
            loadListContent += fileName;
            writeTextToFile(loadListPath, loadListContent);
            log("已将 " + fileName + " 添加到加载列表文件");
        }
    } catch (Exception e) {
        error(e);
        log("更新加载列表文件失败: " + e.getMessage());
    }
    
    try {
        load(filePath);
        sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "文件 " + fileName + " 已加入持久化列表并已加载。");
    } catch (Exception e) {
        error(e);
        sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "文件 " + fileName + " 加入持久化列表，但加载失败：" + e.getMessage());
    }
}

void removeFromPersistList(String fileName, Object msg) {
    // 更新内存中的持久化列表
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
    
    // 更新脚本目录中的加载列表文件
    try {
        String loadListPath = getScriptsDir() + "/load_list.txt";
        if (fileExists(loadListPath)) {
            String loadListContent = readFileText(loadListPath);
            if (loadListContent != null) {
                // 移除包含fileName的行
                String[] lines = loadListContent.split("\\n");
                StringBuilder newLoadListContent = new StringBuilder();
                for (String line : lines) {
                    if (!line.trim().equals(fileName)) {
                        if (newLoadListContent.length() > 0) {
                            newLoadListContent.append("\n");
                        }
                        newLoadListContent.append(line);
                    }
                }
                writeTextToFile(loadListPath, newLoadListContent.toString());
                log("已将 " + fileName + " 从加载列表文件中移除");
            }
        }
    } catch (Exception e) {
        error(e);
        log("更新加载列表文件失败: " + e.getMessage());
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

String getScriptsDir() {
    return appPath + "/scripts";
}

void listScripts(Object msg) {
    try {
        String scriptsDir = getScriptsDir();
        String content = readFileText(scriptsDir);
        if (content == null) {
            sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "脚本目录不存在或无法读取。");
            return;
        }
        
        String[] lines = content.split("\\n");
        ArrayList<String> scripts = new ArrayList<>();
        
        for (String line : lines) {
            if (line.trim().endsWith(".java")) {
                scripts.add(line.trim());
            }
        }
        
        if (scripts.isEmpty()) {
            sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "脚本目录为空。");
        } else {
            StringBuilder sb = new StringBuilder("当前脚本列表：\n");
            for (String script : scripts) {
                sb.append("- " + script + "\n");
            }
            sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, sb.toString());
        }
    } catch (Exception e) {
        error(e);
        sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "列出脚本失败：" + e.getMessage());
    }
}

void stopScript(String fileName, Object msg) {
    try {
        removeFromPersistList(fileName, msg);
        
        sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "脚本 " + fileName + " 已停止。");
    } catch (Exception e) {
        error(e);
        sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "停止脚本失败：" + e.getMessage());
    }
}

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

    String filePath = getScriptsDir() + "/" + fileName;

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
        String filePath = getScriptsDir() + "/" + arg;
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
        String filePath = getScriptsDir() + "/" + arg;
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

    if (cmd.equals("/脚本列表")) {
        listScripts(msg);
        return true;
    }

    if (cmd.equals("/stop")) {
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

// 脚本消息处理器映射
ArrayList<Object> scriptMessageHandlers = new ArrayList<>();

// 注册脚本消息处理器
void registerScriptMessageHandler(Object handler) {
    if (handler != null) {
        scriptMessageHandlers.add(handler);
        log("Registered script message handler: " + handler.getClass().getName());
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
        
        // 调用脚本消息处理器
        try {
            log("Calling script message handlers");
            for (Object handler : scriptMessageHandlers) {
                try {
                    // 尝试调用handler的onMessage方法
                    java.lang.reflect.Method method = handler.getClass().getMethod("onMessage", Object.class);
                    method.invoke(handler, msg);
                } catch (NoSuchMethodException e) {
                    // 忽略没有onMessage方法的处理器
                } catch (Exception e) {
                    error(e);
                    log("Error calling script message handler: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            error(e);
            log("Error calling script message handlers: " + e.getMessage());
        }
        
        // 尝试直接调用text脚本的处理方法（兼容旧版本）
        try {
            if (content.trim().equals("text")) {
                log("Handling text command");
                if (msg.IsGroup) {
                    sendMsg(msg.GroupUin, "", "hello world from text");
                } else {
                    sendMsg("", msg.UserUin, "hello world from text");
                }
            }
        } catch (Exception e) {
            error(e);
            log("Error handling text command: " + e.getMessage());
        }
        
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

// 脚本卸载时调用
void onUnLoad() {
    try {
        log("Main script onUnLoad triggered");
        
        // 分发卸载事件
        try {
            log("Dispatching unload event to registered handlers");
            EventLibrary.dispatchUnload();
        } catch (Exception e) {
            error(e);
            log("Error dispatching unload event: " + e.getMessage());
        }
        
        log("Main script onUnLoad completed successfully");
    } catch (Exception e) {
        error(e);
        log("Critical error in main onUnLoad: " + e.getMessage());
    }
}

// 成员被禁言时调用
void onForbiddenEvent(String GroupUin, String UserUin, String OPUin, long time) {
    try {
        log("Main script onForbiddenEvent triggered: Group=" + GroupUin + ", User=" + UserUin + ", Operator=" + OPUin + ", Time=" + time);
        
        // 分发禁言事件
        try {
            log("Dispatching forbidden event to registered handlers");
            EventLibrary.dispatchForbiddenEvent(GroupUin, UserUin, OPUin, time);
        } catch (Exception e) {
            error(e);
            log("Error dispatching forbidden event: " + e.getMessage());
        }
        
        log("Main script onForbiddenEvent completed successfully");
    } catch (Exception e) {
        error(e);
        log("Critical error in main onForbiddenEvent: " + e.getMessage());
    }
}

// 进群/退群事件时调用
void onTroopEvent(String GroupUin, String UserUin, int type) {
    try {
        log("Main script onTroopEvent triggered: Group=" + GroupUin + ", User=" + UserUin + ", Type=" + type);
        
        // 分发进群/退群事件
        try {
            log("Dispatching troop event to registered handlers");
            EventLibrary.dispatchTroopEvent(GroupUin, UserUin, type);
        } catch (Exception e) {
            error(e);
            log("Error dispatching troop event: " + e.getMessage());
        }
        
        log("Main script onTroopEvent completed successfully");
    } catch (Exception e) {
        error(e);
        log("Critical error in main onTroopEvent: " + e.getMessage());
    }
}

// 点击悬浮窗时调用
void onClickFloatingWindow(int type, String uin) {
    try {
        log("Main script onClickFloatingWindow triggered: Type=" + type + ", Uin=" + uin);
        
        // 分发悬浮窗点击事件
        try {
            log("Dispatching floating window click event to registered handlers");
            EventLibrary.dispatchFloatingWindowClick(type, uin);
        } catch (Exception e) {
            error(e);
            log("Error dispatching floating window click event: " + e.getMessage());
        }
        
        log("Main script onClickFloatingWindow completed successfully");
    } catch (Exception e) {
        error(e);
        log("Critical error in main onClickFloatingWindow: " + e.getMessage());
    }
}

// 发送消息时调用
String getMsg(String msg, String targetUin, int type) {
    try {
        log("Main script getMsg triggered: Message=" + msg + ", TargetUin=" + targetUin + ", Type=" + type);
        
        // 分发消息发送事件
        try {
            log("Dispatching message sending event to registered handlers");
            String processedMsg = EventLibrary.dispatchMessageSending(msg, targetUin, type);
            log("Message processed by handlers: " + processedMsg);
            return processedMsg;
        } catch (Exception e) {
            error(e);
            log("Error dispatching message sending event: " + e.getMessage());
            return msg; // 出错时返回原始消息
        }
    } catch (Exception e) {
        error(e);
        log("Critical error in main getMsg: " + e.getMessage());
        return msg; // 出错时返回原始消息
    }
}

// 长按消息创建菜单时调用
void onCreateMenu(Object msg) {
    try {
        log("Main script onCreateMenu triggered");
        
        // 分发菜单创建事件
        try {
            log("Dispatching menu creation event to registered handlers");
            EventLibrary.dispatchMenuCreation(msg);
        } catch (Exception e) {
            error(e);
            log("Error dispatching menu creation event: " + e.getMessage());
        }
        
        log("Main script onCreateMenu completed successfully");
    } catch (Exception e) {
        error(e);
        log("Critical error in main onCreateMenu: " + e.getMessage());
    }
}

// 收到原始消息时调用
void callbackOnRawMsg(Object msg) {
    try {
        log("Main script callbackOnRawMsg triggered");
        
        // 分发原始消息事件
        try {
            log("Dispatching raw message event to registered handlers");
            EventLibrary.dispatchRawMessage(msg);
        } catch (Exception e) {
            error(e);
            log("Error dispatching raw message event: " + e.getMessage());
        }
        
        log("Main script callbackOnRawMsg completed successfully");
    } catch (Exception e) {
        error(e);
        log("Critical error in main callbackOnRawMsg: " + e.getMessage());
    }
}
