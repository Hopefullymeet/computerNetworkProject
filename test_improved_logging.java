import java.net.*;
import java.io.*;

/**
 * 测试改进后的日志输出格式
 * 验证 Resolver/DNS Relay/DNS Server 的清晰标识
 */
public class test_improved_logging {
    public static void main(String[] args) throws Exception {
        System.out.println("Testing Improved DNS Relay Logging");
        System.out.println("==================================");
        System.out.println();
        System.out.println("Expected log format:");
        System.out.println("Resolver to DNS Relay: DNS[...] from /client:port");
        System.out.println("DNS Relay to Resolver: Intercepted domain -> NXDOMAIN");
        System.out.println("DNS Relay to Resolver: Local hit domain -> IP");
        System.out.println("DNS Relay to DNS Server: Forward domain id=X->Y to /server:53");
        System.out.println("DNS Server to DNS Relay: DNS[...] response");
        System.out.println("DNS Relay to Resolver: Relay response to /client:port (restored id=X)");
        System.out.println();
        
        // 测试不同场景
        testScenario("bad.bupt.edu.cn", 1, "Intercepted domain test");
        Thread.sleep(200);
        
        testScenario("ns.bupt.edu.cn", 1, "Local database hit test");
        Thread.sleep(200);
        
        testScenario("www.google.com", 1, "Upstream forwarding test");
        Thread.sleep(200);
        
        testScenario("www.google.com", 28, "IPv6 query test");
        Thread.sleep(200);
        
        System.out.println("\nTest completed. Check DNS relay output for improved logging format.");
        System.out.println("The communication flow should be clearly identifiable:");
        System.out.println("1. Resolver -> DNS Relay (incoming queries)");
        System.out.println("2. DNS Relay -> Resolver (responses to client)");
        System.out.println("3. DNS Relay -> DNS Server (upstream forwarding)");
        System.out.println("4. DNS Server -> DNS Relay (upstream responses)");
    }
    
    private static void testScenario(String domain, int qtype, String description) {
        System.out.println("Testing: " + description + " (" + domain + ")");
        
        try {
            byte[] query = buildDnsQuery(domain, qtype);
            
            DatagramSocket socket = new DatagramSocket();
            socket.setSoTimeout(3000);
            
            InetSocketAddress dnsServer = new InetSocketAddress("127.0.0.1", 53);
            DatagramPacket packet = new DatagramPacket(query, query.length, dnsServer);
            
            socket.send(packet);
            
            byte[] response = new byte[512];
            DatagramPacket responsePacket = new DatagramPacket(response, response.length);
            socket.receive(responsePacket);
            
            // 简单解析响应
            int rcode = response[3] & 0x0f;
            int ancount = ((response[6] & 0xff) << 8) | (response[7] & 0xff);
            
            if (rcode == 0) {
                System.out.println("  Result: SUCCESS with " + ancount + " answer(s)");
            } else if (rcode == 3) {
                System.out.println("  Result: NXDOMAIN");
            } else {
                System.out.println("  Result: Error code " + rcode);
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
