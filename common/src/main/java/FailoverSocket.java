import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;

public class FailoverSocket {

    private static final Logger log = LoggerFactory.getLogger(FailoverSocket.class);

    private boolean isConnected = false;
    private InvokeLimiter connectionLimiter = new InvokeLimiter(60000);
    private List<Host> hosts;
    private Socket socket;

    /**
     * @param hosts
     * Set hosts for socket connections
     */
    public void setHosts(List<Host> hosts) {
        this.hosts = hosts;
    }

    /**
     * @param intervalLimit default = 60000 milliseconds
     */
    public void setConnectionIntervalLimit(long intervalLimit) {
        connectionLimiter = new InvokeLimiter(intervalLimit);
    }

    /**
     * Connect with interval limit
     */
    public void connect() {
        connect(false);
    }

    public void connect(boolean ignoreConnectionIntervalLimit) {
        if (connectionLimiter.isInvokable()) {
            connectOneNode(hosts);
        } else {
            if (ignoreConnectionIntervalLimit) {
                connectOneNode(hosts);
            } else {
                String err = "connect too fast";
                log.error(err);
                throw new RuntimeException(err);
            }
        }
    }

    private void connectOneNode(List<Host> hosts) {
        for (Host host : hosts) {
            try {
                basicConnect(host.getIp(), host.getPort());
                isConnected = true;
                break;
            } catch (IOException e) {
                log.info("connection failed, host: " + host.toString());
            }
        }
        if (!isConnected) {
            String err = "all connection failed, hosts: " + hosts.toString();
            log.error(err);
            throw new RuntimeException(err);
        }
    }

    private void basicConnect(String host, int port) throws IOException {
        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), 5000);
    }

    /**
     * @return connection status
     */
    public boolean isConnected() {
        return this.isConnected;
    }

    /**
     * Disconnect, release resource
     */
    public void disconnect() {
        try {
            if (socket != null) {
                socket.close();
            }
            this.isConnected = false;
        } catch (IOException e) {
            log.warn("disconnect failed", e);
        }
    }

    /**
     * @param bytes
     * @throws IOException
     * Write bytes with OutputStream
     */
    public void write(byte[] bytes) throws IOException {
        if (!isConnected()) {
            connect();
        }
        try {
            atomicWrite(bytes);
        } catch (IOException e) {
            log.warn("first write failed, try second time");
            disconnect();
            connect(true);
            atomicWrite(bytes);
        }
    }

    private void atomicWrite(byte[] bytes) throws IOException {
        socket.getOutputStream().write(bytes);
        socket.getOutputStream().flush();
    }

    static class Host {

        private String ip;
        private int port;

        public String getIp() {
            return ip;
        }

        public Host setIp(String ip) {
            this.ip = ip;
            return this;
        }

        public int getPort() {
            return port;
        }

        public Host setPort(int port) {
            this.port = port;
            return this;
        }

        @Override
        public String toString() {
            return "Host{" +
                    "ip='" + ip + '\'' +
                    ", port=" + port +
                    '}';
        }

    }

//    public String hosts = "10.1.53.39:28,10.1.53.32:22,10.1.53.38:8080,10.1.53.37:23"; // dev

    private FailoverSocket setUp() {
        FailoverSocket client = new FailoverSocket();
        String host = "121.40.218.7:223";
        List<Host> hosts = new java.util.ArrayList<>();
        Host host1 = new Host();
        host1.setIp("127.0.0.1").setPort(80);
        hosts.add(host1);
        client.setHosts(hosts);
        return client;
    }

    private void testConnect() throws InterruptedException {
        FailoverSocket client = setUp();
        for (int i = 0; i < 100; i++) {
            try {
                client.connect();
            } catch (Exception e) {

            }
            client.disconnect();
            Thread.sleep(1000);
        }
    }

    private void testWrite() throws IOException {
        FailoverSocket client = setUp();
        client.write(new byte[]{12});
    }

    public static void main(String[] args) throws Exception {
//        String sHost = "10.1.53.39";
//        int sPort = 22;
        FailoverSocket client = new FailoverSocket();
//        String host = "121.40.218.7:22";
//        client.setHosts(client.parseHosts(host));
        client.testConnect();
        System.out.println("end");
    }

}
