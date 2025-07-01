import java.net.*;
import java.io.*;

/**
 * Test client to verify address filtering works correctly
 */
public class test_address_filtering {
    public static void main(String[] args) throws Exception {
        System.out.println("Testing DNS Relay Address Filtering");
        System.out.println("===================================");
        
        // Test A record query (IPv4)
        System.out.println("\n1. Testing A record query (IPv4):");
        testQuery("www.baidu.com", 1);
        
        // Test AAAA record query (IPv6)
        System.out.println("\n2. Testing AAAA record query (IPv6):");
        testQuery("www.baidu.com", 28);
        
        System.out.println("\nTest completed. Check the DNS relay output to verify:");
        System.out.println("- With -4 flag: Only A queries should get valid responses, AAAA should get NXDOMAIN");
        System.out.println("- With -6 flag: Only AAAA queries should get valid responses, A should get NXDOMAIN");
        System.out.println("- Without flags: Both A and AAAA queries should get valid responses");
    }
    
    private static void testQuery(String domain, int qtype) throws Exception {
        String typeStr = (qtype == 1) ? "A" : (qtype == 28) ? "AAAA" : "Unknown";
        System.out.println("Querying " + domain + " for " + typeStr + " record...");
        
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
            
            parseResponse(response, responsePacket.getLength());
            socket.close();
            
        } catch (SocketTimeoutException e) {
            System.out.println("  Timeout - DNS relay may not be running on localhost:53");
        } catch (Exception e) {
            System.out.println("  Error: " + e.getMessage());
        }
    }
    
    private static byte[] buildDnsQuery(String domain, int qtype) {
        byte[] query = new byte[512];
        int pos = 0;
        
        // Header
        query[pos++] = 0x12; query[pos++] = 0x34; // ID
        query[pos++] = 0x01; query[pos++] = 0x00; // Flags
        query[pos++] = 0x00; query[pos++] = 0x01; // QDCOUNT = 1
        query[pos++] = 0x00; query[pos++] = 0x00; // ANCOUNT = 0
        query[pos++] = 0x00; query[pos++] = 0x00; // NSCOUNT = 0
        query[pos++] = 0x00; query[pos++] = 0x00; // ARCOUNT = 0
        
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
    
    private static void parseResponse(byte[] response, int length) {
        if (length < 12) {
            System.out.println("  Invalid response");
            return;
        }
        
        int rcode = response[3] & 0x0f;
        int ancount = ((response[6] & 0xff) << 8) | (response[7] & 0xff);
        
        if (rcode == 3) {
            System.out.println("  Result: NXDOMAIN (filtered out)");
        } else if (rcode == 0) {
            System.out.println("  Result: SUCCESS with " + ancount + " answer(s)");
        } else {
            System.out.println("  Result: Error code " + rcode);
        }
    }
}
