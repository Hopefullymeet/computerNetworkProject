import java.net.*;
import java.io.*;

/**
 * Simple test client for DNS Relay
 * Tests basic functionality without using high-level DNS libraries
 */
public class test_dns_relay {
    public static void main(String[] args) throws Exception {
        // Test basic DNS query construction and sending
        testDnsQuery("example.com", 1); // A record
        testDnsQuery("ipv6.example.com", 28); // AAAA record
        testDnsQuery("bad.bupt.edu.cn", 1); // Should return NXDOMAIN
    }
    
    private static void testDnsQuery(String domain, int qtype) throws Exception {
        System.out.println("Testing query for: " + domain + " (type=" + qtype + ")");
        
        // Build DNS query packet manually
        byte[] query = buildDnsQuery(domain, qtype);
        
        // Send to local DNS relay (assuming it's running on localhost:53)
        DatagramSocket socket = new DatagramSocket();
        socket.setSoTimeout(5000); // 5 second timeout
        
        InetSocketAddress dnsServer = new InetSocketAddress("127.0.0.1", 53);
        DatagramPacket packet = new DatagramPacket(query, query.length, dnsServer);
        
        try {
            socket.send(packet);
            
            // Receive response
            byte[] response = new byte[512];
            DatagramPacket responsePacket = new DatagramPacket(response, response.length);
            socket.receive(responsePacket);
            
            // Parse basic response
            parseResponse(response, responsePacket.getLength());
            
        } catch (SocketTimeoutException e) {
            System.out.println("  Timeout - DNS relay may not be running");
        } catch (Exception e) {
            System.out.println("  Error: " + e.getMessage());
        } finally {
            socket.close();
        }
        
        System.out.println();
    }
    
    private static byte[] buildDnsQuery(String domain, int qtype) {
        // Simple DNS query construction
        byte[] query = new byte[512];
        int pos = 0;
        
        // Header
        query[pos++] = 0x12; query[pos++] = 0x34; // ID
        query[pos++] = 0x01; query[pos++] = 0x00; // Flags (standard query)
        query[pos++] = 0x00; query[pos++] = 0x01; // QDCOUNT = 1
        query[pos++] = 0x00; query[pos++] = 0x00; // ANCOUNT = 0
        query[pos++] = 0x00; query[pos++] = 0x00; // NSCOUNT = 0
        query[pos++] = 0x00; query[pos++] = 0x00; // ARCOUNT = 0
        
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
        
        int id = ((response[0] & 0xff) << 8) | (response[1] & 0xff);
        int flags = ((response[2] & 0xff) << 8) | (response[3] & 0xff);
        int rcode = flags & 0x0f;
        int ancount = ((response[6] & 0xff) << 8) | (response[7] & 0xff);
        
        System.out.println("  Response ID: " + id);
        System.out.println("  RCODE: " + rcode + (rcode == 3 ? " (NXDOMAIN)" : rcode == 0 ? " (NOERROR)" : ""));
        System.out.println("  Answer count: " + ancount);
        
        if (rcode == 0 && ancount > 0) {
            System.out.println("  Query successful with " + ancount + " answer(s)");
        }
    }
}
