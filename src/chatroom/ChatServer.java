package chatroom;

import javax.swing.*;
import java.awt.*;
import java.net.*;
import java.util.*;

public class ChatServer extends JFrame {
    private JTextArea chatArea;
    private DatagramSocket serverSocket;
    private InetAddress group;
    private int multicastPort = 4446;
    private int serverPort = 5000; // cổng Server nhận unicast

    // Lưu username theo địa chỉ client
    private Map<SocketAddress, String> clients = new HashMap<>();

    public ChatServer() {
        setTitle("💻 Chat Server (Hub)");
        setSize(600, 450);
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

                            // Gửi danh sách hiện tại cho client mới
                            sendMemberList(clientAddr);

                            // Thêm client vào danh sách
                            clients.put(clientAddr, username);

                            // Thông báo cho tất cả
                            String time = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
                            String notice = "👉 " + username + " đã tham gia từ " 
                                    + ((InetSocketAddress) clientAddr).getAddress().getHostAddress() 
                                    + ":" + ((InetSocketAddress) clientAddr).getPort() 
                                    + " lúc " + time + ". (Hiện có " + clients.size() + " người)";
                            broadcastSystem(notice);

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

    private void handleMessage(String msg, SocketAddress sender) {
        chatArea.append(msg + "\n");
        try {
            byte[] buf = msg.getBytes();
            DatagramPacket multiPacket = new DatagramPacket(buf, buf.length, group, multicastPort);
            serverSocket.send(multiPacket);
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
