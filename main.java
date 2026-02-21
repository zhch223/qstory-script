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
// 
// 模块化改造：
//   - 命令处理逻辑封装
//   - 事件分发机制
//   - 支持多模块协作

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
private static final String STATE_PREFIX = "hotload_state_";
private static final String FILE_PREFIX = "hotload_file_";
// 持久化列表的配置名和键名
private static final String PERSIST_CONFIG = "hotload_persist";
private static final String PERSIST_KEY = "files";
// 管理员列表配置
private static final String ADMIN_CONFIG = "admin_list";
private static final String ADMIN_KEY = "admins";

// 状态常量
private static final int STATE_NONE = 0;
private static final int STATE_WAIT_CREATE = 1; // 等待保存代码
private static final int STATE_WAIT_EDIT = 2;   // 等待编辑代码

// ==================== 初始化与权限管理 ====================
void onLoad() {
    // 确保 myUin 在管理员列表中
    AdminManager.ensureAdmin(myUin);
    // 加载持久化文件
    PersistManager.loadPersistedFiles();
}

// ==================== 消息处理 ====================
void onMsg(Object msg) {
    String content = msg.MessageContent;
    if (content == null || content.isEmpty()) return;

    String sessionId;
    if (msg.IsGroup) {
        sessionId = "g" + msg.GroupUin + "_" + msg.UserUin;
    } else {
        sessionId = "p" + msg.UserUin;
    }

    // 1. 处理等待状态
    if (CommandManager.handleWaitingState(msg, sessionId)) {
        return;
    }

    // 2. 处理命令
    if (CommandManager.handleCommand(msg, sessionId)) {
        return;
    }

    // 3. 分发事件给其他模块
    EventDispatcher.dispatchMessage(msg);
}

// ==================== 管理员管理 ====================
class AdminManager {
    // 确保指定QQ在管理员列表中
    public static void ensureAdmin(String qq) {
        Set<String> admins = getAdminSet();
        if (!admins.contains(qq)) {
            admins.add(qq);
            saveAdminSet(admins);
            log("已将 " + qq + " 添加为默认管理员");
        }
    }

    // 获取管理员集合
    public static Set<String> getAdminSet() {
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

    // 保存管理员集合
    public static void saveAdminSet(Set<String> set) {
        String str = String.join(",", set);
        putString(ADMIN_CONFIG, ADMIN_KEY, str);
    }

    // 检查用户是否为管理员
    public static boolean isAdmin(String qq) {
        return getAdminSet().contains(qq);
    }
}

// ==================== 命令管理 ====================
class CommandManager {
    // 处理等待状态
    public static boolean handleWaitingState(Object msg, String sessionId) {
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

        String filePath = appPath + "/" + fileName;

        if (state == STATE_WAIT_CREATE) {
            // 保存模式：直接将此消息作为代码保存
            try {
                writeTextToFile(filePath, msg.MessageContent);
                sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, fileName + " 已保存。");
            } catch (Exception e) {
                error(e);
                sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "保存失败：" + e.getMessage());
            }
            putInt(STATE_PREFIX, sessionId, STATE_NONE);
            putString(FILE_PREFIX, sessionId, "");
        } else if (state == STATE_WAIT_EDIT) {
            // 编辑模式：保留防误触检查
            if (msg.MessageContent.startsWith("当前文件内容：")) {
                sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, 
                        "检测到误发送，请直接发送代码内容。若想放弃请发送 /取消");
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

    // 处理命令
    public static boolean handleCommand(Object msg, String sessionId) {
        String content = msg.MessageContent;
        if (content == null || !content.startsWith("/")) {
            return false;
        }

        // 非管理员忽略所有命令
        if (!AdminManager.isAdmin(msg.UserUin)) {
            // 可取消注释以提示用户
            // sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "您无权使用命令。");
            return true;
        }

        String[] parts = content.split("\\s+", 2);
        String cmd = parts[0].toLowerCase();
        String arg = parts.length > 1 ? parts[1].trim() : "";

        // 处理取消命令
        if (cmd.equals("/取消")) {
            handleCancelCommand(msg, sessionId);
            return true;
        }

        // 权限管理命令
        if (handlePermissionCommand(msg, cmd, arg)) {
            return true;
        }

        // 文件操作命令
        if (handleFileCommand(msg, sessionId, cmd, arg)) {
            return true;
        }

        // 持久化命令
        if (handlePersistCommand(msg, cmd, arg)) {
            return true;
        }

        return false;
    }

    // 处理取消命令
    private static void handleCancelCommand(Object msg, String sessionId) {
        int state = getInt(STATE_PREFIX, sessionId, STATE_NONE);
        if (state != STATE_NONE) {
            putInt(STATE_PREFIX, sessionId, STATE_NONE);
            putString(FILE_PREFIX, sessionId, "");
            sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "已取消当前操作。");
        } else {
            sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "当前没有等待中的操作。");
        }
    }

    // 处理权限管理命令
    private static boolean handlePermissionCommand(Object msg, String cmd, String arg) {
        if (cmd.equals("/加权")) {
            if (arg.isEmpty() || !arg.matches("\\d+")) {
                sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "用法：/加权 QQ号");
                return true;
            }
            Set<String> admins = AdminManager.getAdminSet();
            if (admins.contains(arg)) {
                sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "该QQ已是管理员。");
            } else {
                admins.add(arg);
                AdminManager.saveAdminSet(admins);
                sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "已将 " + arg + " 添加为管理员。");
            }
            return true;
        } else if (cmd.equals("/去权")) {
            if (arg.isEmpty() || !arg.matches("\\d+")) {
                sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "用法：/去权 QQ号");
                return true;
            }
            Set<String> admins = AdminManager.getAdminSet();
            if (!admins.contains(arg)) {
                sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "该QQ不是管理员。");
            } else if (arg.equals(myUin)) {
                sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "不能移除自己（脚本作者）的管理员权限。");
            } else {
                admins.remove(arg);
                AdminManager.saveAdminSet(admins);
                sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "已将 " + arg + " 移除管理员。");
            }
            return true;
        }
        return false;
    }

    // 处理文件操作命令
    private static boolean handleFileCommand(Object msg, String sessionId, String cmd, String arg) {
        if (cmd.equals("/保存") || cmd.equals("/存储")) {
            if (arg.isEmpty() || !arg.endsWith(".java")) {
                sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "用法：/保存 文件名.java （必须以.java结尾）");
                return true;
            }
            if (!FileManager.isValidFileName(arg)) {
                sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "文件名包含非法字符，只允许字母、数字、下划线、点。");
                return true;
            }
            // 设置等待状态，不发送任何提示
            putInt(STATE_PREFIX, sessionId, STATE_WAIT_CREATE);
            putString(FILE_PREFIX, sessionId, arg);
            return true;
        } else if (cmd.equals("/编辑")) {
            if (arg.isEmpty() || !arg.endsWith(".java")) {
                sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "用法：/编辑 文件名.java （必须以.java结尾）");
                return true;
            }
            if (!FileManager.isValidFileName(arg)) {
                sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "文件名包含非法字符。");
                return true;
            }
            String filePath = appPath + "/" + arg;
            String contentStr = FileManager.readFileText(filePath);
            if (contentStr == null) {
                sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "文件不存在或无法读取：" + arg);
                return true;
            }
            // 发送当前内容并等待新内容
            sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "当前文件内容：\n" + contentStr + "\n请发送新的代码内容覆盖（发送 /取消 可放弃）：");
            putInt(STATE_PREFIX, sessionId, STATE_WAIT_EDIT);
            putString(FILE_PREFIX, sessionId, arg);
            return true;
        } else if (cmd.equals("/加载")) {
            if (arg.isEmpty() || !arg.endsWith(".java")) {
                sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "用法：/加载 文件名.java （必须以.java结尾）");
                return true;
            }
            if (!FileManager.isValidFileName(arg)) {
                sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "文件名包含非法字符。");
                return true;
            }
            String filePath = appPath + "/" + arg;
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

    // 处理持久化命令
    private static boolean handlePersistCommand(Object msg, String cmd, String arg) {
        if (cmd.equals("/保持")) {
            if (arg.isEmpty() || !arg.endsWith(".java")) {
                sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "用法：/保持 文件名.java （必须以.java结尾）");
                return true;
            }
            if (!FileManager.isValidFileName(arg)) {
                sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "文件名包含非法字符。");
                return true;
            }
            PersistManager.addToPersistList(arg, msg);
            return true;
        } else if (cmd.equals("/取消保持")) {
            if (arg.isEmpty() || !arg.endsWith(".java")) {
                sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "用法：/取消保持 文件名.java （必须以.java结尾）");
                return true;
            }
            if (!FileManager.isValidFileName(arg)) {
                sendMsg(msg.IsGroup ? msg.GroupUin : "", msg.IsGroup ? "" : msg.UserUin, "文件名包含非法字符。");
                return true;
            }
            PersistManager.removeFromPersistList(arg, msg);
            return true;
        } else if (cmd.equals("/列表")) {
            PersistManager.showPersistList(msg);
            return true;
        }
        return false;
    }
}

// ==================== 持久化管理 ====================
class PersistManager {
    // 加载持久化文件
    public static void loadPersistedFiles() {
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

    // 添加到持久化列表
    public static void addToPersistList(String fileName, Object msg) {
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
        if (!FileManager.fileExists(filePath)) {
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

    // 从持久化列表移除
    public static void removeFromPersistList(String fileName, Object msg) {
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

    // 显示持久化列表
    public static void showPersistList(Object msg) {
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
}

// ==================== 文件管理 ====================
class FileManager {
    // 检查文件是否存在
    public static boolean fileExists(String path) {
        try {
            String content = readFileText(path);
            return content != null;
        } catch (Exception e) {
            return false;
        }
    }

    // 验证文件名是否合法
    public static boolean isValidFileName(String name) {
        if (name == null || name.isEmpty()) return false;
        return name.matches("^[a-zA-Z0-9_.]+\\.java$") && !name.startsWith(".") && !name.contains("..");
    }

    // 读取文件内容
    public static String readFileText(String path) {
        try {
            return readFileText(path);
        } catch (Exception e) {
            return null;
        }
    }
}

// ==================== 事件分发 ====================
class EventDispatcher {
    // 消息事件监听器接口
    public interface MessageListener {
        void onMessage(Object msg);
    }

    // 监听器列表
    private static List<MessageListener> listeners = new ArrayList<>();

    // 注册消息监听器
    public static void registerListener(MessageListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    // 移除消息监听器
    public static void removeListener(MessageListener listener) {
        listeners.remove(listener);
    }

    // 分发消息事件
    public static void dispatchMessage(Object msg) {
        for (MessageListener listener : listeners) {
            try {
                listener.onMessage(msg);
            } catch (Exception e) {
                error(e);
                log("分发消息时出错：" + e.getMessage());
            }
        }
    }
}
