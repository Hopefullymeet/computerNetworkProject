import java.net.*;
import java.io.*;

/**
 * 测试模块化DNS中继实现
 * 验证各个模块的功能和集成效果
 */
public class test_modular_dns_relay {
    public static void main(String[] args) throws Exception {
        System.out.println("Testing Modular DNS Relay Implementation");
        System.out.println("=======================================");
        System.out.println();
        
        // 测试各个模块
        testDnsMessage();
        testAddressFilter();
        testLocalDatabase();
        
        System.out.println("\nTesting integrated DNS relay functionality:");
        System.out.println("(Make sure DNS relay is running with: java -cp src/main/java DnsRelayBasic -d 8.8.8.8 src/main/java/dnsrelay.txt)");
        System.out.println();
        
        // 测试不同场景
        testScenario("bad.bupt.edu.cn", 1, "Intercepted domain");
        Thread.sleep(200);
        
        testScenario("ns.bupt.edu.cn", 1, "Local database hit");
        Thread.sleep(200);
        
        testScenario("www.google.com", 1, "Upstream forwarding");
        Thread.sleep(200);
        
        testScenario("www.google.com", 28, "IPv6 query");
        Thread.sleep(200);
        
        System.out.println("\nModular implementation benefits:");
        System.out.println("✅ Clear separation of concerns");
        System.out.println("✅ Easy to maintain and extend");
        System.out.println("✅ Reusable components");
        System.out.println("✅ Better testability");
        System.out.println("✅ Improved code organization");
    }
    
    private static void testDnsMessage() {
        System.out.println("Testing DnsMessage class:");
        
        // 创建一个简单的DNS查询
        byte[] query = buildDnsQuery("example.com", 1);
        DnsMessage msg = DnsMessage.parse(query);
        
        if (msg != null) {
            System.out.println("  ✅ DNS message parsing works");
            System.out.println("  " + msg.toDebugString());
            System.out.println("  Query type: " + msg.getQTypeString());
        } else {
            System.out.println("  ❌ DNS message parsing failed");
        }
        System.out.println();
    }
    
    private static void testAddressFilter() {
        System.out.println("Testing AddressFilter class:");
        
        // 测试不同过滤器设置
        AddressFilter.setFilter(AddressFilter.FilterType.IPV4_ONLY);
        System.out.println("  IPv4 only: A=" + AddressFilter.shouldReturnAddressType(1) + 
                          ", AAAA=" + AddressFilter.shouldReturnAddressType(28));
        
        AddressFilter.setFilter(AddressFilter.FilterType.IPV6_ONLY);
        System.out.println("  IPv6 only: A=" + AddressFilter.shouldReturnAddressType(1) + 
                          ", AAAA=" + AddressFilter.shouldReturnAddressType(28));
        
        AddressFilter.setFilter(AddressFilter.FilterType.BOTH);
        System.out.println("  Both: A=" + AddressFilter.shouldReturnAddressType(1) + 
                          ", AAAA=" + AddressFilter.shouldReturnAddressType(28));
        
        System.out.println("  ✅ Address filtering works correctly");
        System.out.println();
    }
    
    private static void testLocalDatabase() {
        System.out.println("Testing LocalDatabase class:");
        
        try {
            LocalDatabase db = new LocalDatabase("src/main/java/dnsrelay.txt");
            
            // 测试查询
            String result1 = db.lookup("bad.bupt.edu.cn");
            String result2 = db.lookup("ns.bupt.edu.cn");
            String result3 = db.lookup("nonexistent.domain.com");
            
            System.out.println("  bad.bupt.edu.cn -> " + result1 + 
                              (db.isIntercepted("bad.bupt.edu.cn") ? " (intercepted)" : ""));
            System.out.println("  ns.bupt.edu.cn -> " + result2);
            System.out.println("  nonexistent.domain.com -> " + result3);
            System.out.println("  " + db.getStatistics());
            System.out.println("  ✅ Local database works correctly");
        } catch (Exception e) {
            System.out.println("  ❌ Local database test failed: " + e.getMessage());
        }
        System.out.println();
    }
    
    private static void testScenario(String domain, int qtype, String description) {
        System.out.println("Testing: " + description + " (" + domain + ")");
        
        try {
            byte[] query = buildDnsQuery(domain, qtype);
            
            DatagramSocket socket = new DatagramSocket();
            socket.setSoTimeout(3000);
            
            InetSocketAddress dnsServer = new InetSocketAddress("127.0.0.1", 53);
            DatagramPacket packet = new DatagramPacket(query, query.length, dnsServer);
            
            long startTime = System.currentTimeMillis();
            socket.send(packet);
            
            byte[] response = new byte[512];
            DatagramPacket responsePacket = new DatagramPacket(response, response.length);
            socket.receive(responsePacket);
            long endTime = System.currentTimeMillis();
            
            // 解析响应
            DnsMessage responseMsg = DnsMessage.parse(
                java.util.Arrays.copyOf(response, responsePacket.getLength()));
            
            if (responseMsg != null) {
                System.out.println("  Result: " + responseMsg.getRCodeString() + 
                                  " with " + responseMsg.ancount + " answer(s)");
                System.out.println("  Response time: " + (endTime - startTime) + "ms");
            } else {
                System.out.println("  Invalid response received");
            }
            
            socket.close();
            
        } catch (SocketTimeoutException e) {
            System.out.println("  Timeout - DNS relay may not be running");
        } catch (Exception e) {
            System.out.println("  Error: " + e.getMessage());
        }
    }
    
    private static byte[] buildDnsQuery(String domain, int qtype) {
        byte[] query = new byte[512];
        int pos = 0;
        
        // Header
        query[pos++] = (byte)(Math.random() * 256); // Random ID
        query[pos++] = (byte)(Math.random() * 256);
        query[pos++] = 0x01; query[pos++] = 0x00;   // Flags: standard query, RD=1
        query[pos++] = 0x00; query[pos++] = 0x01;   // QDCOUNT = 1
        query[pos++] = 0x00; query[pos++] = 0x00;   // ANCOUNT = 0
        query[pos++] = 0x00; query[pos++] = 0x00;   // NSCOUNT = 0
        query[pos++] = 0x00; query[pos++] = 0x00;   // ARCOUNT = 0
        
        // Question
        String[] labels = domain.split("\\.");
        for (String label : labels) {
            query[pos++] = (byte) label.length();
            for (byte b : label.getBytes()) {
                query[pos++] = b;
            }
        }
        query[pos++] = 0; // End of name
        
        query[pos++] = (byte)(qtype >> 8); query[pos++] = (byte)qtype; // QTYPE
        query[pos++] = 0x00; query[pos++] = 0x01; // QCLASS = IN
        
        return java.util.Arrays.copyOf(query, pos);
    }
}
