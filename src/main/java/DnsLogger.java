/**
 * DNS中继日志记录器
 * 提供统一的日志输出格式，支持不同级别的日志记录
 * 
 * @author lyrics
 * @since 2025
 */
public class DnsLogger {
    
    private static boolean debugEnabled = false;
    private static boolean dumpEnabled = false;
    
    /**
     * 启用调试模式
     * @param debug 是否启用调试输出
     */
    public static void setDebugEnabled(boolean debug) {
        debugEnabled = debug;
    }
    
    /**
     * 启用数据包转储
     * @param dump 是否启用数据包转储
     */
    public static void setDumpEnabled(boolean dump) {
        dumpEnabled = dump;
        if (dump) {
            debugEnabled = true; // 转储模式自动启用调试
        }
    }
    
    /**
     * 检查调试模式是否启用
     * @return 如果启用返回true
     */
    public static boolean isDebugEnabled() {
        return debugEnabled;
    }
    
    /**
     * 检查转储模式是否启用
     * @return 如果启用返回true
     */
    public static boolean isDumpEnabled() {
        return dumpEnabled;
    }
    
    /**
     * 记录普通日志（总是输出）
     * @param message 日志消息
     */
    public static void log(String message) {
        System.out.println(formatMessage("INFO", message));
    }
    
    /**
     * 记录调试日志（仅在调试模式下输出）
     * @param message 日志消息
     */
    public static void debug(String message) {
        if (debugEnabled) {
            System.out.println(formatMessage("DEBUG", message));
        }
    }
    
    /**
     * 记录错误日志
     * @param message 错误消息
     */
    public static void error(String message) {
        System.err.println(formatMessage("ERROR", message));
    }
    
    /**
     * 记录错误日志（带异常）
     * @param message 错误消息
     * @param throwable 异常对象
     */
    public static void error(String message, Throwable throwable) {
        System.err.println(formatMessage("ERROR", message + ": " + throwable.getMessage()));
        if (debugEnabled) {
            throwable.printStackTrace();
        }
    }
    
    /**
     * 记录DNS通信日志
     * @param from 发送方
     * @param to 接收方
     * @param message 消息内容
     */
    public static void logCommunication(String from, String to, String message) {
        if (debugEnabled) {
            System.out.println(from + " to " + to + ": " + message);
        }
    }
    
    /**
     * 记录DNS查询
     * @param client 客户端地址
     * @param dnsMessage DNS消息
     */
    public static void logQuery(String client, DnsMessage dnsMessage) {
        logCommunication("Resolver", "DNS Relay", 
            dnsMessage.toDebugString() + " from " + client);
    }
    
    /**
     * 记录本地响应
     * @param domain 域名
     * @param result 结果（IP地址或"NXDOMAIN"）
     * @param isIntercepted 是否为拦截
     */
    public static void logLocalResponse(String domain, String result, boolean isIntercepted) {
        if (isIntercepted) {
            logCommunication("DNS Relay", "Resolver", 
                "Intercepted " + domain + " -> NXDOMAIN");
        } else {
            logCommunication("DNS Relay", "Resolver", 
                "Local hit " + domain + " -> " + result);
        }
    }
    
    /**
     * 记录上游转发
     * @param domain 域名
     * @param originalId 原始ID
     * @param newId 新ID
     * @param upstream 上游服务器地址
     */
    public static void logUpstreamForward(String domain, int originalId, int newId, String upstream) {
        logCommunication("DNS Relay", "DNS Server", 
            "Forward " + domain + " id=" + originalId + "->" + newId + " to " + upstream);
    }
    
    /**
     * 记录上游响应
     * @param dnsMessage DNS响应消息
     */
    public static void logUpstreamResponse(DnsMessage dnsMessage) {
        logCommunication("DNS Server", "DNS Relay", dnsMessage.toDebugString());
    }
    
    /**
     * 记录中继响应
     * @param client 客户端地址
     * @param restoredId 恢复的原始ID
     */
    public static void logRelayResponse(String client, int restoredId) {
        logCommunication("DNS Relay", "Resolver", 
            "Relay response to " + client + " (restored id=" + restoredId + ")");
    }
    
    /**
     * 记录地址过滤信息
     * @param domain 域名
     * @param queryType 查询类型
     * @param filtered 是否被过滤
     */
    public static void logAddressFilter(String domain, int queryType, boolean filtered) {
        if (debugEnabled && filtered) {
            String typeStr = (queryType == 1) ? "A" : (queryType == 28) ? "AAAA" : "TYPE" + queryType;
            debug("Address filter: " + domain + " " + typeStr + " query filtered");
        }
    }
    
    /**
     * 格式化日志消息
     * @param level 日志级别
     * @param message 消息内容
     * @return 格式化后的消息
     */
    private static String formatMessage(String level, String message) {
        if (debugEnabled) {
            return String.format("[%s] %s", level, message);
        } else {
            return message;
        }
    }
    
    /**
     * 记录启动信息
     * @param upstream 上游DNS服务器
     * @param dbFile 数据库文件
     * @param addressFilter 地址过滤器设置
     */
    public static void logStartup(String upstream, String dbFile, String addressFilter) {
        log("DNS-Relay ready  upstream=" + upstream + "  db=" + dbFile);
        log("Address filter: " + addressFilter);
        log("DNS-Relay started with thread pool processing");
    }
    
    /**
     * 记录关闭信息
     */
    public static void logShutdown() {
        log("DNS Relay shutdown completed");
    }
}
