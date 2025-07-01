import java.net.*;
import java.util.Arrays;

/**
 * DNS消息处理工具类
 * 提供DNS消息解析、构造等基础功能
 * 
 * @author lyrics
 * @since 2025
 */
public class DnsMessageUtils {
    
    /**
     * 解析DNS消息中的域名（QNAME）
     * @param msg DNS消息字节数组
     * @param idx 开始解析的位置
     * @return 解析出的域名字符串
     */
    public static String parseQName(byte[] msg, int idx) {
        StringBuilder sb = new StringBuilder();
        int len;
        while ((len = msg[idx++] & 0xff) != 0) {
            if (sb.length() > 0) sb.append('.');
            sb.append(new String(msg, idx, len));
            idx += len;
        }
        return sb.toString();
    }

    /**
     * 构造NXDOMAIN响应
     * @param query 原始查询消息
     * @return NXDOMAIN响应消息
     */
    public static byte[] buildNX(byte[] query) {
        byte[] resp = query.clone();
        resp[2] |= 0x80; resp[3] |= 0x03;            // QR=1, RCODE=3
        resp[7] = resp[9] = resp[11] = 0;            // 清空AN/NS/AR计数
        return Arrays.copyOf(resp, query.length);
    }

    /**
     * 构造本地数据库命中的响应
     * @param q 原始查询消息
     * @param name 域名
     * @param qtype 查询类型
     * @param ip IP地址字符串
     * @return 构造的响应消息
     * @throws UnknownHostException 如果IP地址格式错误
     */
    public static byte[] buildLocalAnswer(byte[] q, String name, int qtype, String ip)
            throws UnknownHostException {
        byte[] addr = InetAddress.getByName(ip).getAddress();
        int type = (addr.length == 4) ? 1 : 28;        // A / AAAA

        // 检查地址类型过滤
        if (!AddressFilter.shouldReturnAddressType(type)) {
            return buildNX(q);
        }

        if (qtype != type && qtype != 255) return buildNX(q);

        int qlen = q.length;
        int ans = 12 + addr.length;
        byte[] out = new byte[qlen + ans];
        System.arraycopy(q, 0, out, 0, qlen);
        int p = qlen;
        out[p++] = (byte) 0xC0; out[p++] = 0x0C;      // NAME ptr
        out[p++] = (byte) (type >> 8); out[p++] = (byte) type;
        out[p++] = 0; out[p++] = 1;                  // CLASS IN
        out[p++] = 0; out[p++] = 0; out[p++] = 0x0E; out[p++] = 0x10; // TTL 3600
        out[p++] = 0; out[p++] = (byte) addr.length;
        System.arraycopy(addr, 0, out, p, addr.length);
        // ancount=1
        out[6] = 0; out[7] = 1;
        return out;
    }

    /**
     * 转储字节数组为十六进制格式（用于调试）
     * @param data 要转储的字节数组
     * @return 十六进制字符串
     */
    public static String dumpHex(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            sb.append(String.format("%02x ", data[i]));
            if ((i & 15) == 15) sb.append("\n");
        }
        if ((data.length & 15) != 0) sb.append("\n");
        return sb.toString();
    }

    /**
     * 打印字节数组的十六进制转储
     * @param data 要打印的字节数组
     */
    public static void dump(byte[] data) {
        for (int i = 0; i < data.length; i++) {
            System.out.printf("%02x ", data[i]);
            if ((i & 15) == 15) System.out.println();
        }
        if ((data.length & 15) != 0) System.out.println();
    }

    /**
     * 验证DNS消息的基本格式
     * @param data DNS消息字节数组
     * @return 如果格式有效返回true
     */
    public static boolean isValidDnsMessage(byte[] data) {
        if (data.length < 12) return false;
        
        int qdcount = ((data[4] & 0xff) << 8) | (data[5] & 0xff);
        int ancount = ((data[6] & 0xff) << 8) | (data[7] & 0xff);
        int nscount = ((data[8] & 0xff) << 8) | (data[9] & 0xff);
        int arcount = ((data[10] & 0xff) << 8) | (data[11] & 0xff);
        
        // 基本合理性检查
        return qdcount <= 10 && ancount <= 100 && nscount <= 100 && arcount <= 100;
    }

    /**
     * 获取DNS消息的ID
     * @param data DNS消息字节数组
     * @return 消息ID，如果消息无效返回-1
     */
    public static int getMessageId(byte[] data) {
        if (data.length < 2) return -1;
        return ((data[0] & 0xff) << 8) | (data[1] & 0xff);
    }

    /**
     * 设置DNS消息的ID
     * @param data DNS消息字节数组
     * @param id 新的消息ID
     */
    public static void setMessageId(byte[] data, int id) {
        if (data.length >= 2) {
            data[0] = (byte) (id >> 8);
            data[1] = (byte) id;
        }
    }
}
