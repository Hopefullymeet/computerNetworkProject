/**
 * DNS消息结构类 - 严格按照RFC1034标准实现
 * 
 * @author lyrics
 * @since 2025
 */
public class DnsMessage {
    // Header fields (12 bytes)
    public int id;          // 16-bit: Query/Response ID
    public boolean qr;      // 1-bit: Query(0) or Response(1)
    public int opcode;      // 4-bit: Operation code (0=standard query)
    public boolean aa;      // 1-bit: Authoritative Answer
    public boolean tc;      // 1-bit: Truncation
    public boolean rd;      // 1-bit: Recursion Desired
    public boolean ra;      // 1-bit: Recursion Available
    public int z;           // 3-bit: Reserved (must be 0)
    public int rcode;       // 4-bit: Response Code (0=no error, 3=NXDOMAIN)
    public int qdcount;     // 16-bit: Question count
    public int ancount;     // 16-bit: Answer count
    public int nscount;     // 16-bit: Authority count
    public int arcount;     // 16-bit: Additional count

    // Question section
    public String qname;    // Domain name
    public int qtype;       // Query type (1=A, 28=AAAA)
    public int qclass;      // Query class (1=IN)

    // Raw message data
    public byte[] rawData;

    /**
     * 从字节数组解析DNS消息
     * @param data 原始DNS消息字节数组
     * @return 解析后的DnsMessage对象，解析失败返回null
     */
    public static DnsMessage parse(byte[] data) {
        if (data.length < 12) return null;

        DnsMessage msg = new DnsMessage();
        msg.rawData = data.clone();

        // 解析Header
        msg.id = ((data[0] & 0xff) << 8) | (data[1] & 0xff);
        int flags = ((data[2] & 0xff) << 8) | (data[3] & 0xff);
        msg.qr = (flags & 0x8000) != 0;
        msg.opcode = (flags >> 11) & 0x0f;
        msg.aa = (flags & 0x0400) != 0;
        msg.tc = (flags & 0x0200) != 0;
        msg.rd = (flags & 0x0100) != 0;
        msg.ra = (flags & 0x0080) != 0;
        msg.z = (flags >> 4) & 0x07;
        msg.rcode = flags & 0x0f;

        msg.qdcount = ((data[4] & 0xff) << 8) | (data[5] & 0xff);
        msg.ancount = ((data[6] & 0xff) << 8) | (data[7] & 0xff);
        msg.nscount = ((data[8] & 0xff) << 8) | (data[9] & 0xff);
        msg.arcount = ((data[10] & 0xff) << 8) | (data[11] & 0xff);

        // 解析Question section (如果存在)
        if (msg.qdcount > 0 && data.length > 12) {
            int idx = 12;
            msg.qname = DnsMessageUtils.parseQName(data, idx);
            idx += msg.qname.length() + 2; // +2 for null terminator
            if (idx + 4 <= data.length) {
                msg.qtype = ((data[idx] & 0xff) << 8) | (data[idx+1] & 0xff);
                msg.qclass = ((data[idx+2] & 0xff) << 8) | (data[idx+3] & 0xff);
            }
        }

        return msg;
    }

    /**
     * 生成调试输出字符串
     * @return 包含DNS消息详细信息的字符串
     */
    public String toDebugString() {
        StringBuilder sb = new StringBuilder();
        sb.append("DNS[ID=").append(id);
        sb.append(", QR=").append(qr ? "Response" : "Query");
        sb.append(", OPCODE=").append(opcode);
        if (qr) {
            sb.append(", AA=").append(aa);
            sb.append(", RA=").append(ra);
            sb.append(", RCODE=").append(rcode);
            if (rcode == 3) sb.append("(NXDOMAIN)");
        } else {
            sb.append(", RD=").append(rd);
        }
        sb.append(", QD=").append(qdcount);
        sb.append(", AN=").append(ancount);
        sb.append(", NS=").append(nscount);
        sb.append(", AR=").append(arcount);
        if (qname != null) {
            sb.append(", QNAME=").append(qname);
            sb.append(", QTYPE=").append(qtype);
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * 获取查询类型的字符串表示
     * @return 查询类型字符串
     */
    public String getQTypeString() {
        switch (qtype) {
            case 1: return "A";
            case 28: return "AAAA";
            case 12: return "PTR";
            case 15: return "MX";
            case 16: return "TXT";
            case 255: return "ANY";
            default: return "TYPE" + qtype;
        }
    }

    /**
     * 获取响应码的字符串表示
     * @return 响应码字符串
     */
    public String getRCodeString() {
        switch (rcode) {
            case 0: return "NOERROR";
            case 1: return "FORMERR";
            case 2: return "SERVFAIL";
            case 3: return "NXDOMAIN";
            case 4: return "NOTIMP";
            case 5: return "REFUSED";
            default: return "RCODE" + rcode;
        }
    }
}
