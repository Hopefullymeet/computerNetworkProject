/**
 * 地址类型过滤器
 * 控制DNS中继返回IPv4、IPv6或两者的地址记录
 * 
 * @author lyrics
 * @since 2025
 */
public class AddressFilter {
    
    /**
     * 地址过滤类型枚举
     */
    public enum FilterType {
        BOTH,       // 默认：返回 A 和 AAAA 记录
        IPV4_ONLY,  // 仅返回 A 记录
        IPV6_ONLY   // 仅返回 AAAA 记录
    }
    
    private static FilterType currentFilter = FilterType.BOTH;
    
    /**
     * 设置地址过滤类型
     * @param filter 过滤类型
     */
    public static void setFilter(FilterType filter) {
        currentFilter = filter;
    }
    
    /**
     * 获取当前地址过滤类型
     * @return 当前过滤类型
     */
    public static FilterType getFilter() {
        return currentFilter;
    }
    
    /**
     * 检查是否应该返回指定类型的地址记录
     * @param recordType DNS记录类型 (1=A, 28=AAAA)
     * @return 如果应该返回该类型记录返回true
     */
    public static boolean shouldReturnAddressType(int recordType) {
        switch (currentFilter) {
            case IPV4_ONLY:
                return recordType == 1;  // A record
            case IPV6_ONLY:
                return recordType == 28; // AAAA record
            case BOTH:
            default:
                return true;
        }
    }
    
    /**
     * 应用地址类型过滤到上游DNS响应
     * @param response 原始响应消息
     * @param originalQuery 原始查询消息
     * @return 过滤后的响应消息
     */
    public static byte[] applyFilterToUpstreamResponse(byte[] response, DnsMessage originalQuery) {
        // 如果地址过滤设置为BOTH，直接返回原响应
        if (currentFilter == FilterType.BOTH) {
            return response;
        }
        
        // 检查查询类型是否应该被过滤
        boolean shouldFilter = false;
        if (currentFilter == FilterType.IPV4_ONLY && originalQuery.qtype == 28) {
            // IPv4 only模式下，过滤AAAA查询的响应
            shouldFilter = true;
        } else if (currentFilter == FilterType.IPV6_ONLY && originalQuery.qtype == 1) {
            // IPv6 only模式下，过滤A查询的响应
            shouldFilter = true;
        }
        
        if (shouldFilter) {
            // 返回NXDOMAIN响应
            return DnsMessageUtils.buildNX(response);
        }
        
        return response;
    }
    
    /**
     * 检查查询类型是否被当前过滤器允许
     * @param qtype 查询类型
     * @return 如果查询类型被允许返回true
     */
    public static boolean isQueryTypeAllowed(int qtype) {
        switch (currentFilter) {
            case IPV4_ONLY:
                return qtype == 1 || qtype == 255; // A or ANY
            case IPV6_ONLY:
                return qtype == 28 || qtype == 255; // AAAA or ANY
            case BOTH:
            default:
                return true;
        }
    }
    
    /**
     * 获取过滤器的描述字符串
     * @return 过滤器描述
     */
    public static String getFilterDescription() {
        switch (currentFilter) {
            case IPV4_ONLY:
                return "IPv4 only (A records)";
            case IPV6_ONLY:
                return "IPv6 only (AAAA records)";
            case BOTH:
            default:
                return "Both IPv4 and IPv6 (A and AAAA records)";
        }
    }
    
    /**
     * 从命令行参数解析过滤器类型
     * @param arg 命令行参数
     * @return 对应的过滤器类型，如果参数无效返回null
     */
    public static FilterType parseFromCommandLine(String arg) {
        switch (arg) {
            case "-4":
                return FilterType.IPV4_ONLY;
            case "-6":
                return FilterType.IPV6_ONLY;
            default:
                return null;
        }
    }
}
