// AdvancedLibrary.java
// Advanced external library implementation with inner classes, interfaces, and complex OOP features

import java.util.ArrayList;
import java.util.List;

public class AdvancedLibrary {
    // Message handler interface
    public interface MessageHandler {
        void handleMessage(Message message);
    }
    
    // Message class
    public static class Message {
        private String content;
        private String sender;
        private long timestamp;
        
        public Message(String content, String sender) {
            this.content = content;
            this.sender = sender;
            this.timestamp = System.currentTimeMillis();
        }
        
        public String getContent() {
            return content;
        }
        
        public String getSender() {
            return sender;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        @Override
        public String toString() {
            return "Message{content='" + content + "', sender='" + sender + "', timestamp=" + timestamp + "}";
        }
    }
    
    // Event dispatcher (inner class)
    public static class EventDispatcher {
        private List<MessageHandler> handlers;
        
        public EventDispatcher() {
            this.handlers = new ArrayList<>();
        }
        
        public void registerHandler(MessageHandler handler) {
            if (handler != null && !handlers.contains(handler)) {
                handlers.add(handler);
            }
        }
        
        public void unregisterHandler(MessageHandler handler) {
            if (handler != null) {
                handlers.remove(handler);
            }
        }
        
        public void dispatchMessage(Message message) {
            for (MessageHandler handler : handlers) {
                try {
                    handler.handleMessage(message);
                } catch (Exception e) {
                    System.err.println("Error handling message: " + e.getMessage());
                }
            }
        }
        
        public int getHandlerCount() {
            return handlers.size();
        }
    }
    
    // Default message handler (inner class)
    public static class DefaultMessageHandler implements MessageHandler {
        private String name;
        
        public DefaultMessageHandler(String name) {
            this.name = name;
        }
        
        @Override
        public void handleMessage(Message message) {
            System.out.println(name + " handled message: " + message.getContent());
        }
        
        public String getName() {
            return name;
        }
    }
    
    // Message service (singleton pattern)
    public static class MessageService {
        private static MessageService instance;
        private EventDispatcher dispatcher;
        
        private MessageService() {
            this.dispatcher = new EventDispatcher();
        }
        
        public static synchronized MessageService getInstance() {
            if (instance == null) {
                instance = new MessageService();
            }
            return instance;
        }
        
        public void registerHandler(MessageHandler handler) {
            dispatcher.registerHandler(handler);
        }
        
        public void processMessage(String content, String sender) {
            Message message = new Message(content, sender);
            dispatcher.dispatchMessage(message);
        }
        
        public int getHandlerCount() {
            return dispatcher.getHandlerCount();
        }
    }
    
    // Factory methods
    public static Message createMessage(String content, String sender) {
        return new Message(content, sender);
    }
    
    public static EventDispatcher createEventDispatcher() {
        return new EventDispatcher();
    }
    
    public static DefaultMessageHandler createDefaultHandler(String name) {
        return new DefaultMessageHandler(name);
    }
    
    // Utility methods
    public static String formatMessage(Message message) {
        if (message == null) return "null";
        return "[" + message.getSender() + "] " + message.getContent();
    }
    
    public static boolean isEmptyMessage(Message message) {
        return message == null || message.getContent() == null || message.getContent().trim().isEmpty();
    }
}
