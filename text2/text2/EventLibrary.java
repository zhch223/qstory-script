// EventLibrary.java
// Event dispatcher library to solve QStory script onMsg conflicts

import java.util.ArrayList;
import java.util.List;

public class EventLibrary {
    // Event dispatcher
    public static class EventDispatcher {
        private static List<SimpleHandler> handlers = new ArrayList<>();
        
        // Register simple handler
        public static void register(SimpleHandler handler) {
            if (handler != null && !handlers.contains(handler)) {
                handlers.add(handler);
            }
        }
        
        // Dispatch message
        public static void dispatch(Object msg) {
            for (SimpleHandler handler : handlers) {
                try {
                    handler.handle(msg);
                } catch (Exception e) {
                    System.err.println("Error handling message: " + e.getMessage());
                }
            }
        }
        
        // Get handler count
        public static int getCount() {
            return handlers.size();
        }
    }
    
    // Convenience methods
    public static void registerHandler(SimpleHandler handler) {
        EventDispatcher.register(handler);
    }
    
    public static void dispatchMessage(Object msg) {
        EventDispatcher.dispatch(msg);
    }
    
    public static int getHandlerCount() {
        return EventDispatcher.getCount();
    }
}
