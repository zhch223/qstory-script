// EventLibrary.java
// Event dispatcher library to solve QStory script onMsg conflicts

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

public class EventLibrary {
    // Message handler interface (internal for better compatibility with bsh 1.4)
    public interface MessageHandler {
        void handle(Object msg);
    }
    
    // Forbidden event handler interface
    public interface ForbiddenEventHandler {
        void onForbiddenEvent(String GroupUin, String UserUin, String OPUin, long time);
    }
    
    // Troop event handler interface
    public interface TroopEventHandler {
        void onTroopEvent(String GroupUin, String UserUin, int type);
    }
    
    // Floating window click handler interface
    public interface FloatingWindowClickHandler {
        void onClickFloatingWindow(int type, String uin);
    }
    
    // Message sending handler interface
    public interface MessageSendingHandler {
        String getMsg(String msg, String targetUin, int type);
    }
    
    // Menu creation handler interface
    public interface MenuCreationHandler {
        void onCreateMenu(Object msg);
    }
    
    // Raw message handler interface
    public interface RawMessageHandler {
        void callbackOnRawMsg(Object msg);
    }
    
    // Load handler interface
    public interface LoadHandler {
        void onLoad();
    }
    
    // Unload handler interface
    public interface UnloadHandler {
        void onUnLoad();
    }
    
    // Message type enum
    public enum MessageType {
        TEXT(1, "text"),
        CARD(2, "card"),
        IMAGE_TEXT(3, "image_text"),
        VOICE(4, "voice"),
        FILE(5, "file"),
        REPLY(6, "reply"),
        UNKNOWN(0, "unknown");
        
        private final int code;
        private final String name;
        
        MessageType(int code, String name) {
            this.code = code;
            this.name = name;
        }
        
        public int getCode() {
            return code;
        }
        
        public String getName() {
            return name;
        }
        
        public static MessageType fromCode(int code) {
            for (MessageType type : values()) {
                if (type.code == code) {
                    return type;
                }
            }
            return UNKNOWN;
        }
        
        public static MessageType fromName(String name) {
            for (MessageType type : values()) {
                if (type.name.equals(name)) {
                    return type;
                }
            }
            return UNKNOWN;
        }
    }
    
    // Priority enum
    public enum Priority {
        LOW(0, "low"),
        NORMAL(1, "normal"),
        HIGH(2, "high");
        
        private final int value;
        private final String name;
        
        Priority(int value, String name) {
            this.value = value;
            this.name = name;
        }
        
        public int getValue() {
            return value;
        }
        
        public String getName() {
            return name;
        }
        
        public static Priority fromValue(int value) {
            for (Priority priority : values()) {
                if (priority.value == value) {
                    return priority;
                }
            }
            return NORMAL;
        }
        
        public static Priority fromName(String name) {
            for (Priority priority : values()) {
                if (priority.name.equals(name)) {
                    return priority;
                }
            }
            return NORMAL;
        }
    }
    
    // Priority levels for handlers (backward compatibility)
    public static final int PRIORITY_LOW = Priority.LOW.getValue();
    public static final int PRIORITY_NORMAL = Priority.NORMAL.getValue();
    public static final int PRIORITY_HIGH = Priority.HIGH.getValue();
    
    // Handler with priority
    private static class PriorityHandler {
        final MessageHandler handler;
        final int priority;
        
        PriorityHandler(MessageHandler handler, int priority) {
            this.handler = handler;
            this.priority = priority;
        }
        
        PriorityHandler(MessageHandler handler, Priority priority) {
            this(handler, priority.getValue());
        }
    }
    
    // Event dispatcher
    public static class EventDispatcher {
        // Message handlers
        private static List<PriorityHandler> messageHandlers = new ArrayList<>();
        // Forbidden event handlers
        private static List<ForbiddenEventHandler> forbiddenEventHandlers = new ArrayList<>();
        // Troop event handlers
        private static List<TroopEventHandler> troopEventHandlers = new ArrayList<>();
        // Floating window click handlers
        private static List<FloatingWindowClickHandler> floatingWindowClickHandlers = new ArrayList<>();
        // Message sending handlers
        private static List<MessageSendingHandler> messageSendingHandlers = new ArrayList<>();
        // Menu creation handlers
        private static List<MenuCreationHandler> menuCreationHandlers = new ArrayList<>();
        // Raw message handlers
        private static List<RawMessageHandler> rawMessageHandlers = new ArrayList<>();
        // Load handlers
        private static List<LoadHandler> loadHandlers = new ArrayList<>();
        // Unload handlers
        private static List<UnloadHandler> unloadHandlers = new ArrayList<>();
        
        private static boolean initialized = false;
        
        // Initialize dispatcher
        private static void initialize() {
            if (!initialized) {
                initialized = true;
                // Any initialization code here
            }
        }
        
        // ==================== Message handlers ====================
        // Register handler with default priority
        public static void register(MessageHandler handler) {
            register(handler, Priority.NORMAL);
        }
        
        // Register handler with specified priority (int for backward compatibility)
        public static void register(MessageHandler handler, int priority) {
            register(handler, Priority.fromValue(priority));
        }
        
        // Register handler with specified priority (enum for better type safety)
        public static void register(MessageHandler handler, Priority priority) {
            initialize();
            if (handler != null) {
                // Check if handler already exists
                for (PriorityHandler ph : messageHandlers) {
                    if (ph.handler == handler) {
                        return; // Handler already registered
                    }
                }
                messageHandlers.add(new PriorityHandler(handler, priority));
                // Sort handlers by priority (highest first)
                Collections.sort(messageHandlers, (a, b) -> b.priority - a.priority);
            }
        }
        
        // Unregister message handler
        public static void unregister(MessageHandler handler) {
            initialize();
            if (handler != null) {
                messageHandlers.removeIf(ph -> ph.handler == handler);
            }
        }
        
        // Dispatch message to all handlers
        public static void dispatch(Object msg) {
            initialize();
            if (msg == null) {
                return;
            }
            
            // Create a copy to avoid concurrent modification
            List<PriorityHandler> handlersCopy = new ArrayList<>(messageHandlers);
            
            for (PriorityHandler ph : handlersCopy) {
                try {
                    ph.handler.handle(msg);
                } catch (Exception e) {
                    // Log error but continue processing other handlers
                    System.err.println("Error in message handler: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        
        // ==================== Forbidden event handlers ====================
        public static void register(ForbiddenEventHandler handler) {
            initialize();
            if (handler != null && !forbiddenEventHandlers.contains(handler)) {
                forbiddenEventHandlers.add(handler);
            }
        }
        
        public static void unregister(ForbiddenEventHandler handler) {
            initialize();
            if (handler != null) {
                forbiddenEventHandlers.remove(handler);
            }
        }
        
        public static void dispatchForbiddenEvent(String GroupUin, String UserUin, String OPUin, long time) {
            initialize();
            List<ForbiddenEventHandler> handlersCopy = new ArrayList<>(forbiddenEventHandlers);
            for (ForbiddenEventHandler handler : handlersCopy) {
                try {
                    handler.onForbiddenEvent(GroupUin, UserUin, OPUin, time);
                } catch (Exception e) {
                    System.err.println("Error in forbidden event handler: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        
        // ==================== Troop event handlers ====================
        public static void register(TroopEventHandler handler) {
            initialize();
            if (handler != null && !troopEventHandlers.contains(handler)) {
                troopEventHandlers.add(handler);
            }
        }
        
        public static void unregister(TroopEventHandler handler) {
            initialize();
            if (handler != null) {
                troopEventHandlers.remove(handler);
            }
        }
        
        public static void dispatchTroopEvent(String GroupUin, String UserUin, int type) {
            initialize();
            List<TroopEventHandler> handlersCopy = new ArrayList<>(troopEventHandlers);
            for (TroopEventHandler handler : handlersCopy) {
                try {
                    handler.onTroopEvent(GroupUin, UserUin, type);
                } catch (Exception e) {
                    System.err.println("Error in troop event handler: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        
        // ==================== Floating window click handlers ====================
        public static void register(FloatingWindowClickHandler handler) {
            initialize();
            if (handler != null && !floatingWindowClickHandlers.contains(handler)) {
                floatingWindowClickHandlers.add(handler);
            }
        }
        
        public static void unregister(FloatingWindowClickHandler handler) {
            initialize();
            if (handler != null) {
                floatingWindowClickHandlers.remove(handler);
            }
        }
        
        public static void dispatchFloatingWindowClick(int type, String uin) {
            initialize();
            List<FloatingWindowClickHandler> handlersCopy = new ArrayList<>(floatingWindowClickHandlers);
            for (FloatingWindowClickHandler handler : handlersCopy) {
                try {
                    handler.onClickFloatingWindow(type, uin);
                } catch (Exception e) {
                    System.err.println("Error in floating window click handler: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        
        // ==================== Message sending handlers ====================
        public static void register(MessageSendingHandler handler) {
            initialize();
            if (handler != null && !messageSendingHandlers.contains(handler)) {
                messageSendingHandlers.add(handler);
            }
        }
        
        public static void unregister(MessageSendingHandler handler) {
            initialize();
            if (handler != null) {
                messageSendingHandlers.remove(handler);
            }
        }
        
        public static String dispatchMessageSending(String msg, String targetUin, int type) {
            initialize();
            String result = msg;
            List<MessageSendingHandler> handlersCopy = new ArrayList<>(messageSendingHandlers);
            for (MessageSendingHandler handler : handlersCopy) {
                try {
                    String handlerResult = handler.getMsg(result, targetUin, type);
                    if (handlerResult != null) {
                        result = handlerResult;
                    }
                } catch (Exception e) {
                    System.err.println("Error in message sending handler: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            return result;
        }
        
        // ==================== Menu creation handlers ====================
        public static void register(MenuCreationHandler handler) {
            initialize();
            if (handler != null && !menuCreationHandlers.contains(handler)) {
                menuCreationHandlers.add(handler);
            }
        }
        
        public static void unregister(MenuCreationHandler handler) {
            initialize();
            if (handler != null) {
                menuCreationHandlers.remove(handler);
            }
        }
        
        public static void dispatchMenuCreation(Object msg) {
            initialize();
            List<MenuCreationHandler> handlersCopy = new ArrayList<>(menuCreationHandlers);
            for (MenuCreationHandler handler : handlersCopy) {
                try {
                    handler.onCreateMenu(msg);
                } catch (Exception e) {
                    System.err.println("Error in menu creation handler: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        
        // ==================== Raw message handlers ====================
        public static void register(RawMessageHandler handler) {
            initialize();
            if (handler != null && !rawMessageHandlers.contains(handler)) {
                rawMessageHandlers.add(handler);
            }
        }
        
        public static void unregister(RawMessageHandler handler) {
            initialize();
            if (handler != null) {
                rawMessageHandlers.remove(handler);
            }
        }
        
        public static void dispatchRawMessage(Object msg) {
            initialize();
            List<RawMessageHandler> handlersCopy = new ArrayList<>(rawMessageHandlers);
            for (RawMessageHandler handler : handlersCopy) {
                try {
                    handler.callbackOnRawMsg(msg);
                } catch (Exception e) {
                    System.err.println("Error in raw message handler: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        
        // ==================== Load handlers ====================
        public static void register(LoadHandler handler) {
            initialize();
            if (handler != null && !loadHandlers.contains(handler)) {
                loadHandlers.add(handler);
            }
        }
        
        public static void unregister(LoadHandler handler) {
            initialize();
            if (handler != null) {
                loadHandlers.remove(handler);
            }
        }
        
        public static void dispatchLoad() {
            initialize();
            List<LoadHandler> handlersCopy = new ArrayList<>(loadHandlers);
            for (LoadHandler handler : handlersCopy) {
                try {
                    handler.onLoad();
                } catch (Exception e) {
                    System.err.println("Error in load handler: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        
        // ==================== Unload handlers ====================
        public static void register(UnloadHandler handler) {
            initialize();
            if (handler != null && !unloadHandlers.contains(handler)) {
                unloadHandlers.add(handler);
            }
        }
        
        public static void unregister(UnloadHandler handler) {
            initialize();
            if (handler != null) {
                unloadHandlers.remove(handler);
            }
        }
        
        public static void dispatchUnload() {
            initialize();
            List<UnloadHandler> handlersCopy = new ArrayList<>(unloadHandlers);
            for (UnloadHandler handler : handlersCopy) {
                try {
                    handler.onUnLoad();
                } catch (Exception e) {
                    System.err.println("Error in unload handler: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        
        // Get message handler count
        public static int getMessageHandlerCount() {
            initialize();
            return messageHandlers.size();
        }
        
        // Get total handler count
        public static int getTotalHandlerCount() {
            initialize();
            return messageHandlers.size() + forbiddenEventHandlers.size() + 
                   troopEventHandlers.size() + floatingWindowClickHandlers.size() + 
                   messageSendingHandlers.size() + menuCreationHandlers.size() + 
                   rawMessageHandlers.size() + loadHandlers.size() + 
                   unloadHandlers.size();
        }
        
        // Clear all handlers
        public static void clear() {
            initialize();
            messageHandlers.clear();
            forbiddenEventHandlers.clear();
            troopEventHandlers.clear();
            floatingWindowClickHandlers.clear();
            messageSendingHandlers.clear();
            menuCreationHandlers.clear();
            rawMessageHandlers.clear();
            loadHandlers.clear();
            unloadHandlers.clear();
        }
    }
    
    // Convenience methods
    // ==================== Message handlers ====================
    public static void registerHandler(MessageHandler handler) {
        EventDispatcher.register(handler);
    }
    
    public static void registerHandler(MessageHandler handler, int priority) {
        EventDispatcher.register(handler, priority);
    }
    
    public static void registerHandler(MessageHandler handler, Priority priority) {
        EventDispatcher.register(handler, priority);
    }
    
    public static void unregisterHandler(MessageHandler handler) {
        EventDispatcher.unregister(handler);
    }
    
    public static void dispatchMessage(Object msg) {
        EventDispatcher.dispatch(msg);
    }
    
    // ==================== Forbidden event handlers ====================
    public static void registerForbiddenEventHandler(ForbiddenEventHandler handler) {
        EventDispatcher.register(handler);
    }
    
    public static void unregisterForbiddenEventHandler(ForbiddenEventHandler handler) {
        EventDispatcher.unregister(handler);
    }
    
    public static void dispatchForbiddenEvent(String GroupUin, String UserUin, String OPUin, long time) {
        EventDispatcher.dispatchForbiddenEvent(GroupUin, UserUin, OPUin, time);
    }
    
    // ==================== Troop event handlers ====================
    public static void registerTroopEventHandler(TroopEventHandler handler) {
        EventDispatcher.register(handler);
    }
    
    public static void unregisterTroopEventHandler(TroopEventHandler handler) {
        EventDispatcher.unregister(handler);
    }
    
    public static void dispatchTroopEvent(String GroupUin, String UserUin, int type) {
        EventDispatcher.dispatchTroopEvent(GroupUin, UserUin, type);
    }
    
    // ==================== Floating window click handlers ====================
    public static void registerFloatingWindowClickHandler(FloatingWindowClickHandler handler) {
        EventDispatcher.register(handler);
    }
    
    public static void unregisterFloatingWindowClickHandler(FloatingWindowClickHandler handler) {
        EventDispatcher.unregister(handler);
    }
    
    public static void dispatchFloatingWindowClick(int type, String uin) {
        EventDispatcher.dispatchFloatingWindowClick(type, uin);
    }
    
    // ==================== Message sending handlers ====================
    public static void registerMessageSendingHandler(MessageSendingHandler handler) {
        EventDispatcher.register(handler);
    }
    
    public static void unregisterMessageSendingHandler(MessageSendingHandler handler) {
        EventDispatcher.unregister(handler);
    }
    
    public static String dispatchMessageSending(String msg, String targetUin, int type) {
        return EventDispatcher.dispatchMessageSending(msg, targetUin, type);
    }
    
    // ==================== Menu creation handlers ====================
    public static void registerMenuCreationHandler(MenuCreationHandler handler) {
        EventDispatcher.register(handler);
    }
    
    public static void unregisterMenuCreationHandler(MenuCreationHandler handler) {
        EventDispatcher.unregister(handler);
    }
    
    public static void dispatchMenuCreation(Object msg) {
        EventDispatcher.dispatchMenuCreation(msg);
    }
    
    // ==================== Raw message handlers ====================
    public static void registerRawMessageHandler(RawMessageHandler handler) {
        EventDispatcher.register(handler);
    }
    
    public static void unregisterRawMessageHandler(RawMessageHandler handler) {
        EventDispatcher.unregister(handler);
    }
    
    public static void dispatchRawMessage(Object msg) {
        EventDispatcher.dispatchRawMessage(msg);
    }
    
    // ==================== Load handlers ====================
    public static void registerLoadHandler(LoadHandler handler) {
        EventDispatcher.register(handler);
    }
    
    public static void unregisterLoadHandler(LoadHandler handler) {
        EventDispatcher.unregister(handler);
    }
    
    public static void dispatchLoad() {
        EventDispatcher.dispatchLoad();
    }
    
    // ==================== Unload handlers ====================
    public static void registerUnloadHandler(UnloadHandler handler) {
        EventDispatcher.register(handler);
    }
    
    public static void unregisterUnloadHandler(UnloadHandler handler) {
        EventDispatcher.unregister(handler);
    }
    
    public static void dispatchUnload() {
        EventDispatcher.dispatchUnload();
    }
    
    // ==================== Utility methods ====================
    public static int getMessageHandlerCount() {
        return EventDispatcher.getMessageHandlerCount();
    }
    
    public static int getTotalHandlerCount() {
        return EventDispatcher.getTotalHandlerCount();
    }
    
    public static void clearHandlers() {
        EventDispatcher.clear();
    }
    
    // Helper method to get message type from message object
    public static MessageType getMessageType(Object msg) {
        try {
            if (msg != null) {
                // Use reflection to get MessageType field
                java.lang.reflect.Field field = msg.getClass().getField("MessageType");
                if (field != null) {
                    Object value = field.get(msg);
                    if (value instanceof Integer) {
                        return MessageType.fromCode((Integer) value);
                    }
                }
            }
        } catch (Exception e) {
            // Ignore reflection errors
        }
        return MessageType.UNKNOWN;
    }
    
    // Helper method to create a handler from a bsh closure (for better bsh compatibility)
    public static MessageHandler createHandler(final Object bshClosure) {
        return new MessageHandler() {
            public void handle(Object msg) {
                try {
                    // This is a placeholder for bsh closure invocation
                    // In practice, this would use bsh's API to invoke the closure
                    System.out.println("Handler invoked with message: " + msg);
                } catch (Exception e) {
                    System.err.println("Error invoking handler: " + e.getMessage());
                }
            }
        };
    }
    
    // Helper method to create a handler that only handles specific message types
    public static MessageHandler createTypeSpecificHandler(final MessageHandler handler, final MessageType... types) {
        return new MessageHandler() {
            public void handle(Object msg) {
                MessageType type = getMessageType(msg);
                for (MessageType t : types) {
                    if (t == type) {
                        handler.handle(msg);
                        break;
                    }
                }
            }
        };
    }
}
