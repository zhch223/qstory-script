// MyLibrary.java
// Simple external library implementation

public class MyLibrary {
    // Process message method
    public static String processMessage(String message) {
        return "[Library] Processed: " + message;
    }
    
    // Calculate message length
    public static int getMessageLength(String message) {
        return message != null ? message.length() : 0;
    }
    
    // Reverse message
    public static String reverseMessage(String message) {
        if (message == null) return null;
        return new StringBuilder(message).reverse().toString();
    }
    
    // Check if it's a test message
    public static boolean isTestMessage(String message) {
        return "测试".equals(message != null ? message.trim() : null);
    }
}
