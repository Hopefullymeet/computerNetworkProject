import java.io.IOException;
import java.net.*;
import java.util.Arrays;

/**
 * UDP Socket封装类
 * 提供基本的sendto/recvfrom系统调用封装
 * 
 * @author lyrics
 * @since 2025
 */
public class UdpSocketWrapper {
    
    private final DatagramSocket socket;
    private final boolean enableDump;
    private final String socketName;
    
    /**
     * 构造函数
     * @param socket DatagramSocket实例
     * @param socketName socket名称（用于日志）
     * @param enableDump 是否启用数据包转储
     */
    public UdpSocketWrapper(DatagramSocket socket, String socketName, boolean enableDump) {
        this.socket = socket;
        this.socketName = socketName;
        this.enableDump = enableDump;
    }
    
    /**
     * UDP sendto - 基本系统调用封装
     * @param data 要发送的数据
     * @param destination 目标地址
     * @throws IOException 网络IO异常
     */
    public void sendto(byte[] data, SocketAddress destination) throws IOException {
        DatagramPacket packet = new DatagramPacket(data, data.length, destination);
        socket.send(packet);
        
        if (enableDump) {
            DnsLogger.log("SEND from " + socketName + " to " + destination + ":");
            DnsMessageUtils.dump(data);
        }
    }
    
    /**
     * UDP recvfrom - 基本系统调用封装
     * @param buffer 接收缓冲区
     * @return 接收到的数据包
     * @throws IOException 网络IO异常
     */
    public DatagramPacket recvfrom(byte[] buffer) throws IOException {
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        
        if (enableDump) {
            DnsLogger.log("RECV at " + socketName + " from " + packet.getSocketAddress() + ":");
            DnsMessageUtils.dump(Arrays.copyOf(packet.getData(), packet.getLength()));
        }
        
        return packet;
    }
    
    /**
     * 设置socket超时时间
     * @param timeout 超时时间（毫秒）
     * @throws SocketException socket异常
     */
    public void setSoTimeout(int timeout) throws SocketException {
        socket.setSoTimeout(timeout);
    }
    
    /**
     * 获取socket的本地地址
     * @return 本地socket地址
     */
    public SocketAddress getLocalSocketAddress() {
        return socket.getLocalSocketAddress();
    }
    
    /**
     * 检查socket是否已关闭
     * @return 如果socket已关闭返回true
     */
    public boolean isClosed() {
        return socket.isClosed();
    }
    
    /**
     * 关闭socket
     */
    public void close() {
        if (!socket.isClosed()) {
            socket.close();
            DnsLogger.log(socketName + " socket closed");
        }
    }
    
    /**
     * 获取底层DatagramSocket（谨慎使用）
     * @return DatagramSocket实例
     */
    public DatagramSocket getSocket() {
        return socket;
    }
    
    /**
     * 发送DNS消息
     * @param message DNS消息对象
     * @param destination 目标地址
     * @throws IOException 网络IO异常
     */
    public void sendDnsMessage(DnsMessage message, SocketAddress destination) throws IOException {
        sendto(message.rawData, destination);
    }
    
    /**
     * 接收DNS消息
     * @param buffer 接收缓冲区
     * @return 包含DNS消息的数据包
     * @throws IOException 网络IO异常
     */
    public DatagramPacket receiveDnsMessage(byte[] buffer) throws IOException {
        return recvfrom(buffer);
    }
    
    /**
     * 创建服务器socket（绑定到指定端口）
     * @param port 端口号
     * @param enableDump 是否启用数据包转储
     * @return UdpSocketWrapper实例
     * @throws SocketException socket异常
     */
    public static UdpSocketWrapper createServerSocket(int port, boolean enableDump) throws SocketException {
        DatagramSocket socket = new DatagramSocket(port);
        return new UdpSocketWrapper(socket, "Server[:" + port + "]", enableDump);
    }
    
    /**
     * 创建客户端socket（随机端口）
     * @param enableDump 是否启用数据包转储
     * @return UdpSocketWrapper实例
     * @throws SocketException socket异常
     */
    public static UdpSocketWrapper createClientSocket(boolean enableDump) throws SocketException {
        DatagramSocket socket = new DatagramSocket();
        return new UdpSocketWrapper(socket, "Client[:" + socket.getLocalPort() + "]", enableDump);
    }
}
