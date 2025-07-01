import java.io.*;
import java.net.*;
import java.util.concurrent.*;

/**
 * DNS Relay - 模块化
 * 
 * 功能模块：
 *   • DnsMessage: RFC1034标准DNS消息结构
 *   • LocalDatabase: 本地DNS记录数据库
 *   • IdMappingManager: ID映射管理
 *   • UdpSocketWrapper: UDP通信封装
 *   • AddressFilter: 地址类型过滤
 *   • DnsLogger: 统一日志记录
 * 
 * 编译: javac *.java
 * 运行: java DnsRelayBasic [-d|-dd] [-4|-6] [dns-server-ip] [db-file]
 *       java DnsRelayBasic -d 10.3.9.4 dnsrelay.txt
 * 
 * @author lyrics
 * @since 2025
 */
public class DnsRelayBasic {
    
    private static final int DNS_PORT = 53;
    
    private final LocalDatabase database;
    private final IdMappingManager idManager;
    private final UdpSocketWrapper serverSocket;
    private final UdpSocketWrapper upstreamSocket;
    private final ExecutorService threadPool;
    private final InetSocketAddress upstreamAddress;
    
    private volatile boolean running = true;
    
    /**
     * 构造器
     * @param upstreamIP 上游DNS服务器IP
     * @param dbFile 本地数据库文件路径
     * @throws Exception 初始化异常
     */
    public DnsRelayBasic(String upstreamIP, String dbFile) throws Exception {
        this.database = new LocalDatabase(dbFile);
        this.idManager = new IdMappingManager();
        this.threadPool = Executors.newCachedThreadPool();
        
        // 创建socket，处理端口冲突
        try {
            this.serverSocket = UdpSocketWrapper.createServerSocket(DNS_PORT, DnsLogger.isDumpEnabled());
        } catch (java.net.BindException e) {
            DnsLogger.error("Failed to bind to port " + DNS_PORT + ": " + e.getMessage());
            DnsLogger.error("Port " + DNS_PORT + " is already in use. Please:");
            DnsLogger.error("1. Stop any existing DNS services on port " + DNS_PORT);
            DnsLogger.error("2. Kill any previous DNS relay instances");
            DnsLogger.error("3. On Windows, try running as Administrator");
            DnsLogger.error("4. Check if Windows DNS Client service is using the port");
            throw new Exception("Cannot bind to DNS port " + DNS_PORT + ". Port is already in use.", e);
        }

        this.upstreamSocket = UdpSocketWrapper.createClientSocket(DnsLogger.isDumpEnabled());
        this.upstreamSocket.setSoTimeout(5000);
        
        // 设置上游地址
        this.upstreamAddress = new InetSocketAddress(InetAddress.getByName(upstreamIP), DNS_PORT);
        
        DnsLogger.logStartup(upstreamIP, dbFile, AddressFilter.getFilterDescription());
    }
    
    /**
     * 主循环，使用线程池并发处理
     */
    public void loop() throws IOException {
        // 启动上游响应处理线程
        threadPool.submit(this::handleUpstreamResponses);
        
        // 启动ID映射清理线程
        idManager.startPeriodicCleanup(30000);

        // 主线程处理客户端查询
        byte[] buffer = new byte[512];

        while (running) {
            try {
                DatagramPacket packet = serverSocket.recvfrom(buffer);
                byte[] data = new byte[packet.getLength()];
                System.arraycopy(packet.getData(), 0, data, 0, packet.getLength());
                InetSocketAddress client = (InetSocketAddress) packet.getSocketAddress();
                
                // 使用线程池处理查询
                threadPool.submit(() -> {
                    try {
                        handleQuery(client, data);
                    } catch (Exception e) {
                        DnsLogger.error("Error handling query from " + client, e);
                    }
                });
                
            } catch (IOException e) {
                if (running) {
                    DnsLogger.error("Error receiving query", e);
                }
            }
        }
    }
    
    /**
     * 处理客户端查询
     */
    private void handleQuery(InetSocketAddress client, byte[] rawMsg) throws IOException {
        // 解析DNS消息
        DnsMessage dnsMsg = DnsMessage.parse(rawMsg);
        if (dnsMsg == null || dnsMsg.qdcount != 1) {
            DnsLogger.debug("Invalid DNS message from " + client);
            return;
        }

        DnsLogger.logQuery(client.toString(), dnsMsg);

        // 查询本地数据库（根据查询类型）
        String localResult = database.lookup(dnsMsg.qname, dnsMsg.qtype);
        if (localResult != null) {
            byte[] response;
            if ("0.0.0.0".equals(localResult)) {
                // Case 1: 被拦截的域名
                response = DnsMessageUtils.buildNX(rawMsg);
                DnsLogger.logLocalResponse(dnsMsg.qname, "NXDOMAIN", true);
            } else {
                // Case 2: 本地数据库命中
                response = DnsMessageUtils.buildLocalAnswer(rawMsg, dnsMsg.qname, dnsMsg.qtype, localResult);
                DnsLogger.logLocalResponse(dnsMsg.qname, localResult, false);
            }
            serverSocket.sendto(response, client);
            return;
        }

        // Case 3: 转发到上游DNS服务器
        forwardToUpstream(client, dnsMsg);
    }

    /**
     * 转发查询到上游DNS服务器
     */
    private void forwardToUpstream(InetSocketAddress client, DnsMessage originalMsg) throws IOException {
        int newId = idManager.createMapping(client, originalMsg);
        
        // 修改ID并转发
        byte[] forwardMsg = originalMsg.rawData.clone();
        DnsMessageUtils.setMessageId(forwardMsg, newId);
        
        upstreamSocket.sendto(forwardMsg, upstreamAddress);
        DnsLogger.logUpstreamForward(originalMsg.qname, originalMsg.id, newId, upstreamAddress.toString());
    }
    
    /**
     * 处理上游DNS响应的线程方法
     */
    private void handleUpstreamResponses() {
        byte[] buffer = new byte[512];
        
        while (running) {
            try {
                DatagramPacket packet = upstreamSocket.recvfrom(buffer);
                byte[] data = new byte[packet.getLength()];
                System.arraycopy(packet.getData(), 0, data, 0, packet.getLength());
                handleUpstreamResponse(data);
            } catch (SocketTimeoutException ignore) {
                // 超时，继续循环
            } catch (IOException e) {
                if (running) {
                    DnsLogger.error("Error receiving upstream response", e);
                }
            }
        }
    }

    /**
     * 处理上游DNS响应
     */
    private void handleUpstreamResponse(byte[] rawResponse) throws IOException {
        DnsMessage responseMsg = DnsMessage.parse(rawResponse);
        if (responseMsg == null) {
            DnsLogger.debug("Invalid upstream response");
            return;
        }

        // 查找ID映射
        IdMappingManager.IdMapping mapping = idManager.removeMapping(responseMsg.id);
        if (mapping == null) {
            DnsLogger.debug("No mapping found for response ID: " + responseMsg.id);
            return;
        }

        DnsLogger.logUpstreamResponse(responseMsg);

        // 保存成功的响应到本地数据库
        if (responseMsg.rcode == 0 && responseMsg.ancount > 0) {
            saveUpstreamResponse(rawResponse, mapping.originalMsg);
        }

        // 恢复原始ID
        DnsMessageUtils.setMessageId(rawResponse, mapping.originalId);

        byte[] filteredResponse = AddressFilter.applyFilterToUpstreamResponse(rawResponse, mapping.originalMsg);

        serverSocket.sendto(filteredResponse, mapping.client);
        DnsLogger.logRelayResponse(mapping.client.toString(), mapping.originalId);
    }

    /**
     * 保存上游DNS响应到本地数据库
     */
    private void saveUpstreamResponse(byte[] response, DnsMessage originalQuery) {
        try {
            String[] ipAddresses = DnsMessageUtils.extractIpAddresses(response);

            if (ipAddresses.length > 0) {
                // 保存所有IP地址到本地数据库
                for (String ip : ipAddresses) {
                    database.saveUpstreamRecord(originalQuery.qname, ip, originalQuery.qtype);
                }

                // 如果有多个IP地址，记录到调试日志
                if (ipAddresses.length > 1) {
                    DnsLogger.debug("Multiple IPs found for " + originalQuery.qname +
                        ", saved " + ipAddresses.length + " records");
                }
            }
        } catch (Exception e) {
            DnsLogger.error("Error saving upstream response for " + originalQuery.qname, e);
        }
    }
    
    public void shutdown() {
        running = false;
        try {
            threadPool.shutdown();
            if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
            
            serverSocket.close();
            upstreamSocket.close();
            
            DnsLogger.logShutdown();
        } catch (Exception e) {
            DnsLogger.error("Error during shutdown", e);
        }
    }

    public static void main(String[] args) throws Exception {
        String upstream = "10.3.9.4";
        String file = "dnsrelay.txt";
        
        // 解析命令行参数
        for (String arg : args) {
            if ("-d".equals(arg)) {
                DnsLogger.setDebugEnabled(true);
            } else if ("-dd".equals(arg)) {
                DnsLogger.setDumpEnabled(true);
            } else if ("-4".equals(arg)) {
                AddressFilter.setFilter(AddressFilter.FilterType.IPV4_ONLY);
            } else if ("-6".equals(arg)) {
                AddressFilter.setFilter(AddressFilter.FilterType.IPV6_ONLY);
            } else if (arg.matches("\\d+\\.\\d+\\.\\d+\\.\\d+|\\[.*]")) {
                upstream = arg;
            } else {
                file = arg;
            }
        }
        
        DnsRelayBasic relay = new DnsRelayBasic(upstream, file);
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            DnsLogger.log("Shutting down DNS Relay...");
            relay.shutdown();
        }));
        
        relay.loop();
    }
}
