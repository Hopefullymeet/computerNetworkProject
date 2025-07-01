import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * 本地DNS数据库
 * 负责加载和查询本地DNS记录
 * 
 * @author lyrics
 * @since 2025
 */
public class LocalDatabase {
    
    private final Map<String, String> records = new HashMap<>();
    private final String databaseFile;
    
    /**
     * 构造函数，加载指定的数据库文件
     * @param filename 数据库文件路径
     * @throws IOException 文件读取异常
     */
    public LocalDatabase(String filename) throws IOException {
        this.databaseFile = filename;
        loadDatabase();
        DnsLogger.log("Loaded local database: " + filename + " (" + records.size() + " records)");
    }
    
    /**
     * 加载数据库文件
     * @throws IOException 文件读取异常
     */
    private void loadDatabase() throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(databaseFile))) {
            String line;
            int lineNumber = 0;
            
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                
                // 跳过空行和注释行
                if (line.isEmpty() || line.startsWith("#") || line.startsWith(";")) {
                    continue;
                }
                
                // 解析记录行：域名 IP地址
                String[] parts = line.split("\\s+");
                if (parts.length >= 2) {
                    String domain = parts[0].toLowerCase();
                    String ip = parts[1];
                    
                    // 验证记录格式
                    if (isValidDomainName(domain) && isValidIpAddress(ip)) {
                        records.put(domain, ip);
                        DnsLogger.debug("Loaded record: " + domain + " -> " + ip);
                    } else {
                        DnsLogger.error("Invalid record at line " + lineNumber + ": " + line);
                    }
                } else {
                    DnsLogger.error("Malformed record at line " + lineNumber + ": " + line);
                }
            }
        }
    }
    
    /**
     * 查询域名对应的IP地址
     * @param domain 域名（不区分大小写）
     * @return IP地址字符串，如果不存在返回null
     */
    public String lookup(String domain) {
        if (domain == null) return null;
        
        String result = records.get(domain.toLowerCase());
        DnsLogger.debug("Database lookup: " + domain + " -> " + 
            (result != null ? result : "not found"));
        
        return result;
    }
    
    /**
     * 检查域名是否存在于数据库中
     * @param domain 域名
     * @return 如果存在返回true
     */
    public boolean contains(String domain) {
        return domain != null && records.containsKey(domain.toLowerCase());
    }
    
    /**
     * 检查域名是否被拦截（映射到0.0.0.0）
     * @param domain 域名
     * @return 如果被拦截返回true
     */
    public boolean isIntercepted(String domain) {
        String result = lookup(domain);
        return "0.0.0.0".equals(result);
    }
    
    /**
     * 获取数据库中的记录数量
     * @return 记录数量
     */
    public int getRecordCount() {
        return records.size();
    }
    
    /**
     * 获取数据库文件路径
     * @return 文件路径
     */
    public String getDatabaseFile() {
        return databaseFile;
    }
    
    /**
     * 重新加载数据库
     * @throws IOException 文件读取异常
     */
    public void reload() throws IOException {
        records.clear();
        loadDatabase();
        DnsLogger.log("Reloaded database: " + databaseFile + " (" + records.size() + " records)");
    }
    
    /**
     * 添加或更新记录
     * @param domain 域名
     * @param ip IP地址
     */
    public void addRecord(String domain, String ip) {
        if (isValidDomainName(domain) && isValidIpAddress(ip)) {
            records.put(domain.toLowerCase(), ip);
            DnsLogger.debug("Added record: " + domain + " -> " + ip);
        } else {
            throw new IllegalArgumentException("Invalid domain or IP: " + domain + " -> " + ip);
        }
    }
    
    /**
     * 删除记录
     * @param domain 域名
     * @return 如果记录存在并被删除返回true
     */
    public boolean removeRecord(String domain) {
        String removed = records.remove(domain.toLowerCase());
        if (removed != null) {
            DnsLogger.debug("Removed record: " + domain + " -> " + removed);
            return true;
        }
        return false;
    }
    
    /**
     * 获取所有记录的副本
     * @return 记录映射的副本
     */
    public Map<String, String> getAllRecords() {
        return new HashMap<>(records);
    }
    
    /**
     * 验证域名格式
     * @param domain 域名
     * @return 如果格式有效返回true
     */
    private boolean isValidDomainName(String domain) {
        if (domain == null || domain.isEmpty() || domain.length() > 253) {
            return false;
        }
        
        // 简单的域名格式检查
        return domain.matches("^[a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?(\\.[a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?)*$");
    }
    
    /**
     * 验证IP地址格式
     * @param ip IP地址字符串
     * @return 如果格式有效返回true
     */
    private boolean isValidIpAddress(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        
        // 检查IPv4格式
        if (ip.matches("^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$")) {
            return true;
        }
        
        // 检查IPv6格式（简化版）
        if (ip.matches("^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$") ||
            ip.matches("^::1$") || ip.matches("^::$")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 获取数据库统计信息
     * @return 统计信息字符串
     */
    public String getStatistics() {
        int total = records.size();
        int intercepted = 0;
        int ipv4 = 0;
        int ipv6 = 0;
        
        for (String ip : records.values()) {
            if ("0.0.0.0".equals(ip)) {
                intercepted++;
            } else if (ip.contains(".")) {
                ipv4++;
            } else if (ip.contains(":")) {
                ipv6++;
            }
        }
        
        return String.format("Database: total=%d, intercepted=%d, IPv4=%d, IPv6=%d", 
            total, intercepted, ipv4, ipv6);
    }
}
