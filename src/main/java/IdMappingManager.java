import java.net.InetSocketAddress;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ID映射管理器
 * 负责DNS查询ID的分配、映射和恢复，确保上游转发的正确性
 * 
 * @author lyrics
 * @since 2025
 */
public class IdMappingManager {
    
    /**
     * ID映射条目
     */
    public static class IdMapping {
        public final InetSocketAddress client;     // 客户端地址
        public final DnsMessage originalMsg;       // 原始DNS消息
        public final int originalId;               // 原始ID
        public final int newId;                    // 新分配的ID
        public final long timestamp;               // 时间戳
        
        public IdMapping(InetSocketAddress client, DnsMessage msg, int newId) {
            this.client = client;
            this.originalMsg = msg;
            this.originalId = msg.id;
            this.newId = newId;
            this.timestamp = System.currentTimeMillis();
        }
        
        /**
         * 检查映射是否已过期
         * @param timeoutMs 超时时间（毫秒）
         * @return 如果已过期返回true
         */
        public boolean isExpired(long timeoutMs) {
            return System.currentTimeMillis() - timestamp > timeoutMs;
        }
        
        @Override
        public String toString() {
            return String.format("IdMapping[%d->%d, client=%s, domain=%s, age=%dms]",
                originalId, newId, client, originalMsg.qname, 
                System.currentTimeMillis() - timestamp);
        }
    }
    
    private final ConcurrentHashMap<Integer, IdMapping> mappings = new ConcurrentHashMap<>();
    private final Random random = new Random();
    private final Object lockObject = new Object();
    
    // 配置参数
    private static final int MAX_ID = 0xFFFF;
    private static final long DEFAULT_TIMEOUT_MS = 30000; // 30秒超时
    
    /**
     * 生成唯一的新ID并创建映射
     * @param client 客户端地址
     * @param originalMsg 原始DNS消息
     * @return 新分配的ID
     */
    public int createMapping(InetSocketAddress client, DnsMessage originalMsg) {
        synchronized (lockObject) {
            int newId;
            int attempts = 0;
            do {
                newId = random.nextInt(MAX_ID);
                attempts++;
                if (attempts > 1000) {
                    // 防止无限循环，清理一些过期映射
                    cleanupExpiredMappings();
                    attempts = 0;
                }
            } while (mappings.containsKey(newId));
            
            IdMapping mapping = new IdMapping(client, originalMsg, newId);
            mappings.put(newId, mapping);
            
            DnsLogger.debug("Created ID mapping: " + mapping);
            return newId;
        }
    }
    
    /**
     * 根据新ID查找并移除映射
     * @param newId 新ID
     * @return 对应的映射，如果不存在返回null
     */
    public IdMapping removeMapping(int newId) {
        IdMapping mapping = mappings.remove(newId);
        if (mapping != null) {
            DnsLogger.debug("Removed ID mapping: " + mapping);
        }
        return mapping;
    }
    
    /**
     * 根据新ID查找映射（不移除）
     * @param newId 新ID
     * @return 对应的映射，如果不存在返回null
     */
    public IdMapping getMapping(int newId) {
        return mappings.get(newId);
    }
    
    /**
     * 清理过期的映射
     * @return 清理的映射数量
     */
    public int cleanupExpiredMappings() {
        return cleanupExpiredMappings(DEFAULT_TIMEOUT_MS);
    }
    
    /**
     * 清理过期的映射
     * @param timeoutMs 超时时间（毫秒）
     * @return 清理的映射数量
     */
    public int cleanupExpiredMappings(long timeoutMs) {
        int removedCount = 0;
        long now = System.currentTimeMillis();
        
        mappings.entrySet().removeIf(entry -> {
            boolean expired = entry.getValue().isExpired(timeoutMs);
            if (expired) {
                DnsLogger.debug("Expired ID mapping: " + entry.getValue());
            }
            return expired;
        });
        
        return removedCount;
    }
    
    /**
     * 获取当前活跃映射数量
     * @return 活跃映射数量
     */
    public int getActiveMappingCount() {
        return mappings.size();
    }
    
    /**
     * 检查是否存在指定ID的映射
     * @param id ID值
     * @return 如果存在返回true
     */
    public boolean hasMapping(int id) {
        return mappings.containsKey(id);
    }
    
    /**
     * 清空所有映射
     */
    public void clearAllMappings() {
        int count = mappings.size();
        mappings.clear();
        DnsLogger.log("Cleared " + count + " ID mappings");
    }
    
    /**
     * 获取映射统计信息
     * @return 统计信息字符串
     */
    public String getStatistics() {
        int total = mappings.size();
        long now = System.currentTimeMillis();
        int expired = 0;
        long oldestAge = 0;
        long newestAge = Long.MAX_VALUE;
        
        for (IdMapping mapping : mappings.values()) {
            long age = now - mapping.timestamp;
            if (mapping.isExpired(DEFAULT_TIMEOUT_MS)) {
                expired++;
            }
            oldestAge = Math.max(oldestAge, age);
            newestAge = Math.min(newestAge, age);
        }
        
        return String.format("ID Mappings: total=%d, expired=%d, oldest=%dms, newest=%dms",
            total, expired, oldestAge, newestAge == Long.MAX_VALUE ? 0 : newestAge);
    }
    
    /**
     * 启动定期清理任务
     * @param intervalMs 清理间隔（毫秒）
     */
    public void startPeriodicCleanup(long intervalMs) {
        Thread cleanupThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(intervalMs);
                    int cleaned = cleanupExpiredMappings();
                    if (cleaned > 0) {
                        DnsLogger.log("Cleaned " + cleaned + " expired ID mappings, active: " + getActiveMappingCount());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        cleanupThread.setDaemon(true);
        cleanupThread.setName("IdMapping-Cleanup");
        cleanupThread.start();
        
        DnsLogger.log("Started ID mapping cleanup thread (interval=" + intervalMs + "ms)");
    }
}
