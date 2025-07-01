# 模块化DNS中继实现

## 🎯 模块化设计概述

按照您的要求，我将DNS中继的不同功能和模块分成了独立的类文件，实现了清晰的代码组织和更好的可维护性。

## 📁 模块结构

### 1. **DnsMessage.java** - DNS消息结构
**功能**：RFC1034标准DNS消息解析和处理
```java
public class DnsMessage {
    // RFC1034标准字段
    public int id, opcode, rcode;
    public boolean qr, aa, tc, rd, ra;
    public int qdcount, ancount, nscount, arcount;
    public String qname;
    public int qtype, qclass;
    
    // 核心方法
    public static DnsMessage parse(byte[] data);
    public String toDebugString();
    public String getQTypeString();
    public String getRCodeString();
}
```

### 2. **DnsMessageUtils.java** - DNS消息工具
**功能**：DNS消息解析、构造等基础功能
```java
public class DnsMessageUtils {
    public static String parseQName(byte[] msg, int idx);
    public static byte[] buildNX(byte[] query);
    public static byte[] buildLocalAnswer(...);
    public static void dump(byte[] data);
    public static int getMessageId(byte[] data);
    public static void setMessageId(byte[] data, int id);
}
```

### 3. **AddressFilter.java** - 地址类型过滤
**功能**：控制IPv4/IPv6地址记录的返回
```java
public class AddressFilter {
    public enum FilterType { BOTH, IPV4_ONLY, IPV6_ONLY }
    
    public static void setFilter(FilterType filter);
    public static boolean shouldReturnAddressType(int recordType);
    public static byte[] applyFilterToUpstreamResponse(...);
    public static String getFilterDescription();
}
```

### 4. **UdpSocketWrapper.java** - UDP通信封装
**功能**：基本UDP系统调用封装
```java
public class UdpSocketWrapper {
    public void sendto(byte[] data, SocketAddress destination);
    public DatagramPacket recvfrom(byte[] buffer);
    public void setSoTimeout(int timeout);
    
    // 工厂方法
    public static UdpSocketWrapper createServerSocket(int port, boolean enableDump);
    public static UdpSocketWrapper createClientSocket(boolean enableDump);
}
```

### 5. **IdMappingManager.java** - ID映射管理
**功能**：DNS查询ID的分配、映射和恢复
```java
public class IdMappingManager {
    public static class IdMapping {
        public final InetSocketAddress client;
        public final DnsMessage originalMsg;
        public final int originalId, newId;
        public final long timestamp;
    }
    
    public int createMapping(InetSocketAddress client, DnsMessage originalMsg);
    public IdMapping removeMapping(int newId);
    public void startPeriodicCleanup(long intervalMs);
}
```

### 6. **DnsLogger.java** - 统一日志记录
**功能**：提供清晰的通信日志格式
```java
public class DnsLogger {
    public static void setDebugEnabled(boolean debug);
    public static void setDumpEnabled(boolean dump);
    
    // 专门的通信日志方法
    public static void logQuery(String client, DnsMessage dnsMessage);
    public static void logLocalResponse(String domain, String result, boolean isIntercepted);
    public static void logUpstreamForward(String domain, int originalId, int newId, String upstream);
    public static void logUpstreamResponse(DnsMessage dnsMessage);
    public static void logRelayResponse(String client, int restoredId);
}
```

### 7. **LocalDatabase.java** - 本地DNS数据库
**功能**：本地DNS记录的加载和查询
```java
public class LocalDatabase {
    public LocalDatabase(String filename) throws IOException;
    public String lookup(String domain);
    public boolean isIntercepted(String domain);
    public void reload() throws IOException;
    public String getStatistics();
}
```

### 8. **DnsRelayBasic.java** - 主控制器
**功能**：协调各个模块，实现主要业务逻辑
```java
public class DnsRelayBasic {
    // 组合各个模块
    private final LocalDatabase database;
    private final IdMappingManager idManager;
    private final UdpSocketWrapper serverSocket;
    private final UdpSocketWrapper upstreamSocket;
    
    // 核心业务方法
    private void handleQuery(InetSocketAddress client, byte[] rawMsg);
    private void forwardToUpstream(InetSocketAddress client, DnsMessage originalMsg);
    private void handleUpstreamResponse(byte[] rawResponse);
}
```

## 🚀 模块化优势

### ✅ **清晰的职责分离**
- 每个类专注于单一功能
- 降低代码耦合度
- 提高代码可读性

### ✅ **易于维护和扩展**
- 修改某个功能只需要修改对应的类
- 新功能可以独立开发和测试
- 减少修改影响范围

### ✅ **更好的可测试性**
- 每个模块可以独立测试
- 便于单元测试和集成测试
- 更容易定位问题

### ✅ **代码复用性**
- 工具类可以在其他项目中复用
- 模块化设计便于组件化开发
- 降低重复代码

## 🔧 使用方法

### 编译所有模块
```bash
javac src/main/java/*.java
```

### 运行DNS中继
```bash
# 基本运行
java -cp src/main/java DnsRelayBasic 8.8.8.8 src/main/java/dnsrelay.txt

# 调试模式
java -cp src/main/java DnsRelayBasic -d 8.8.8.8 src/main/java/dnsrelay.txt

# IPv4过滤
java -cp src/main/java DnsRelayBasic -d -4 8.8.8.8 src/main/java/dnsrelay.txt
```

### 测试模块化实现
```bash
javac -cp src/main/java test_modular_dns_relay.java
java -cp src/main/java test_modular_dns_relay
```

## 📊 通信日志示例

模块化实现保持了清晰的通信日志格式：

```
Resolver to DNS Relay: DNS[ID=123, QR=Query, QNAME=bad.bupt.edu.cn, QTYPE=1] from /127.0.0.1:54321
DNS Relay to Resolver: Intercepted bad.bupt.edu.cn -> NXDOMAIN

Resolver to DNS Relay: DNS[ID=124, QR=Query, QNAME=www.google.com, QTYPE=1] from /127.0.0.1:54322
DNS Relay to DNS Server: Forward www.google.com id=124->45678 to /8.8.8.8:53
DNS Server to DNS Relay: DNS[ID=45678, QR=Response, RCODE=0, AN=2]
DNS Relay to Resolver: Relay response to /127.0.0.1:54322 (restored id=124)
```

## 🎯 技术特性保持

### ✅ **所有原有功能完整保留**
- RFC1034标准实现
- 线程池并发处理
- 基本UDP系统调用
- ID映射管理
- 地址类型过滤
- 拦截vs找不到区分

### ✅ **性能和可靠性不受影响**
- 模块化不影响运行效率
- 线程安全性得到维护
- 内存使用优化

### ✅ **向后兼容**
- 命令行参数保持不变
- 配置文件格式不变
- 网络协议完全兼容

## 📈 开发和维护效益

1. **新功能开发**：可以独立开发新模块而不影响现有功能
2. **问题定位**：模块化使问题定位更加精确
3. **代码审查**：每个模块可以独立审查
4. **团队协作**：不同开发者可以负责不同模块
5. **单元测试**：每个模块都可以编写独立的测试用例

这种模块化设计大大提升了代码的可维护性和扩展性，同时保持了所有原有的功能和性能特性。
