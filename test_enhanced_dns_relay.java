import java.net.*;
import java.io.*;

/**
 * 测试增强版DNS中继的功能
 * 验证RFC1034消息结构解析、线程池处理、ID映射等功能
 */
public class test_enhanced_dns_relay {
    public static void main(String[] args) throws Exception {
        System.out.println("Enhanced DNS Relay Test");
        System.out.println("=======================");
        
        // 测试不同类型的查询
        testQuery("example.com", 1, "A record test");
        Thread.sleep(100);
        
        testQuery("ipv6.example.com", 28, "AAAA record test");
        Thread.sleep(100);
        
        testQuery("bad.bupt.edu.cn", 1, "Intercepted domain test");
        Thread.sleep(100);
        
        testQuery("ns.bupt.edu.cn", 1, "Local database hit test");
        Thread.sleep(100);
        
        testQuery("www.google.com", 1, "Upstream forwarding test");
        Thread.sleep(100);
        
        System.out.println("\nTest completed. Check DNS relay output for:");
        System.out.println("1. RFC1034 message structure parsing");
        System.out.println("2. Proper ID mapping and restoration");
        System.out.println("3. Thread pool concurrent processing");
        System.out.println("4. Address type filtering");
        System.out.println("5. Intercepted vs not found distinction");
    }
    
    private static void testQuery(String domain, int qtype, String description) {
        System.out.println("\n" + description + ": " + domain);
        
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
            
            parseResponse(response, responsePacket.getLength());
            System.out.println("  Response time: " + (endTime - startTime) + "ms");
            
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
        
        // Header (12 bytes)
        query[pos++] = (byte)(Math.random() * 256); // Random ID high byte
        query[pos++] = (byte)(Math.random() * 256); // Random ID low byte
        query[pos++] = 0x01; query[pos++] = 0x00;   // Flags: standard query, RD=1
        query[pos++] = 0x00; query[pos++] = 0x01;   // QDCOUNT = 1
        query[pos++] = 0x00; query[pos++] = 0x00;   // ANCOUNT = 0
        query[pos++] = 0x00; query[pos++] = 0x00;   // NSCOUNT = 0
        query[pos++] = 0x00; query[pos++] = 0x00;   // ARCOUNT = 0
        
        // Question section
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
    
    private static void parseResponse(byte[] response, int length) {
        if (length < 12) {
            System.out.println("  Invalid response (too short)");
            return;
        }
        
        // Parse header
        int id = ((response[0] & 0xff) << 8) | (response[1] & 0xff);
        int flags = ((response[2] & 0xff) << 8) | (response[3] & 0xff);
        boolean qr = (flags & 0x8000) != 0;
        int opcode = (flags >> 11) & 0x0f;
        boolean aa = (flags & 0x0400) != 0;
        boolean tc = (flags & 0x0200) != 0;
        boolean rd = (flags & 0x0100) != 0;
        boolean ra = (flags & 0x0080) != 0;
        int rcode = flags & 0x0f;
        
        int qdcount = ((response[4] & 0xff) << 8) | (response[5] & 0xff);
        int ancount = ((response[6] & 0xff) << 8) | (response[7] & 0xff);
        int nscount = ((response[8] & 0xff) << 8) | (response[9] & 0xff);
        int arcount = ((response[10] & 0xff) << 8) | (response[11] & 0xff);
        
        System.out.println("  Response: ID=" + id + ", QR=" + (qr ? "Response" : "Query"));
        System.out.println("  Flags: OPCODE=" + opcode + ", AA=" + aa + ", TC=" + tc + 
                          ", RD=" + rd + ", RA=" + ra + ", RCODE=" + rcode);
        System.out.println("  Counts: QD=" + qdcount + ", AN=" + ancount + 
                          ", NS=" + nscount + ", AR=" + arcount);
        
        if (rcode == 0) {
            System.out.println("  Result: SUCCESS with " + ancount + " answer(s)");
        } else if (rcode == 3) {
            System.out.println("  Result: NXDOMAIN (domain not found or intercepted)");
        } else {
            System.out.println("  Result: Error code " + rcode);
        }
    }
}
