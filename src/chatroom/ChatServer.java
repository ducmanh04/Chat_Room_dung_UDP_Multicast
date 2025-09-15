package chatroom;

import javax.swing.*;
import java.awt.*;
import java.net.*;
import java.util.*;

public class ChatServer extends JFrame {
    private JTextArea chatArea;
    private DatagramSocket serverSocket;
    private MulticastSocket multicastSocket;
    private InetAddress group;
    private int multicastPort = 4446;
    private int serverPort = 5000; // cổng Server nhận unicast
    private Set<SocketAddress> clients = new HashSet<>();

    public ChatServer() {
        setTitle("💻 Chat Server (Hub)");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JLabel header = new JLabel("Chat Server đang chạy...", SwingConstants.CENTER);
        header.setFont(new Font("Arial", Font.BOLD, 16));
        header.setForeground(new Color(30, 144, 255));
        add(header, BorderLayout.NORTH);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        chatArea.setBackground(new Color(245, 245, 245));
        add(new JScrollPane(chatArea), BorderLayout.CENTER);

        try {
            serverSocket = new DatagramSocket(serverPort);
            multicastSocket = new MulticastSocket(multicastPort);
            group = InetAddress.getByName("230.0.0.1");

            // Thread nhận tin từ Client
            Thread listener = new Thread(() -> {
                byte[] buffer = new byte[1024];
                while (true) {
                    try {
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        serverSocket.receive(packet);

                        String msg = new String(packet.getData(), 0, packet.getLength());
                        SocketAddress clientAddr = packet.getSocketAddress();

                        if (msg.startsWith("JOIN:")) {
                            String username = msg.substring(5);
                            clients.add(clientAddr);
                            broadcastSystem("👉 " + username + " đã tham gia. (Hiện có " + clients.size() + " người)");
                        } else {
                            handleMessage(msg, clientAddr);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        break;
                    }
                }
            });
            listener.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleMessage(String msg, SocketAddress sender) {
        chatArea.append(msg + "\n");
        try {
            if (clients.size() < 2) {
                // Gửi riêng lại cho người gửi
                String notice = "[Hệ thống]: Bạn là người duy nhất trong phòng, tin nhắn sẽ không gửi cho ai.";
                byte[] buf = notice.getBytes();
                DatagramPacket noticePacket = new DatagramPacket(buf, buf.length,
                        ((InetSocketAddress) sender).getAddress(), ((InetSocketAddress) sender).getPort());
                serverSocket.send(noticePacket);
            } else {
                // Phát cho tất cả bằng multicast
                byte[] buf = msg.getBytes();
                DatagramPacket multiPacket = new DatagramPacket(buf, buf.length, group, multicastPort);
                serverSocket.send(multiPacket);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void broadcastSystem(String msg) {
        chatArea.append("[SYSTEM]: " + msg + "\n");
        try {
            byte[] buf = msg.getBytes();
            DatagramPacket packet = new DatagramPacket(buf, buf.length, group, multicastPort);
            serverSocket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChatServer().setVisible(true));
    }
}
