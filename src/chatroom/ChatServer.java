package chatroom;

import java.net.*;
import java.util.*;

public class ChatServer {
    private DatagramSocket serverSocket;
    private InetAddress group;
    private int multicastPort = 4446;
    private int serverPort = 5000;

    // Lưu username theo địa chỉ client
    private Map<SocketAddress, String> clients = new HashMap<>();

    public ChatServer() {
        try {
            serverSocket = new DatagramSocket(serverPort);
            group = InetAddress.getByName("230.0.0.1");

            // Thread nhận tin từ Client
            new Thread(() -> {
                byte[] buffer = new byte[1024];
                while (true) {
                    try {
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        serverSocket.receive(packet);

                        String msg = new String(packet.getData(), 0, packet.getLength());
                        SocketAddress clientAddr = packet.getSocketAddress();

                        if (msg.startsWith("JOIN:")) {
                            String username = msg.substring(5);

                            // Gửi danh sách hiện tại cho client mới
                            sendMemberList(clientAddr);

                            // Thêm client vào danh sách
                            clients.put(clientAddr, username);

                            // Thông báo cho tất cả
                            broadcastSystem(username + " đã tham gia. (Hiện có " + clients.size() + " người)");
                        } else if (msg.startsWith("LEAVE:")) {
                            String username = clients.remove(clientAddr);
                            if (username != null) {
                                broadcastSystem(username + " đã rời phòng. (Còn " + clients.size() + " người)");
                            }
                        } else {
                            handleMessage(msg);
                        }

                    } catch (Exception e) {
                        break;
                    }
                }
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Gửi danh sách thành viên cho client mới
    private void sendMemberList(SocketAddress newClient) {
        try {
            if (clients.isEmpty()) return;

            StringBuilder sb = new StringBuilder("__MEMBERLIST__:");
            for (String user : clients.values()) {
                sb.append(user).append(",");
            }

            byte[] buf = sb.toString().getBytes();
            DatagramPacket packet = new DatagramPacket(
                    buf, buf.length,
                    ((InetSocketAddress) newClient).getAddress(),
                    ((InetSocketAddress) newClient).getPort()
            );
            serverSocket.send(packet);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleMessage(String msg) {
        try {
            byte[] buf = msg.getBytes();
            DatagramPacket multiPacket = new DatagramPacket(buf, buf.length, group, multicastPort);
            serverSocket.send(multiPacket);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void broadcastSystem(String msg) {
        try {
            byte[] buf = msg.getBytes();
            DatagramPacket packet = new DatagramPacket(buf, buf.length, group, multicastPort);
            serverSocket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new ChatServer();
        System.out.println("✅ Chat Server đang chạy...");
    }
}
