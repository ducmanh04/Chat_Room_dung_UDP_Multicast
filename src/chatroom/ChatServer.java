package chatroom;

import java.net.*;
import java.util.*;

public class ChatServer {
    private static final int PORT = 4446;
    private static List<InetSocketAddress> clientList = new ArrayList<>();

    public static void main(String[] args) {
        try (DatagramSocket socket = new DatagramSocket(PORT)) {
            System.out.println("🚀 Chat Server đang chạy trên cổng " + PORT);

            byte[] buffer = new byte[1024];
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String msg = new String(packet.getData(), 0, packet.getLength());
                InetSocketAddress clientAddr = new InetSocketAddress(packet.getAddress(), packet.getPort());

                // Nếu client mới thì thêm vào danh sách
                if (!clientList.contains(clientAddr)) {
                    clientList.add(clientAddr);
                    System.out.println("🟢 Client mới tham gia: " + clientAddr);
                }

                System.out.println("📩 Nhận: " + msg);

                // Gửi tin nhắn đến tất cả client khác
                for (InetSocketAddress addr : clientList) {
                    DatagramPacket sendPacket = new DatagramPacket(msg.getBytes(), msg.length(), addr.getAddress(), addr.getPort());
                    socket.send(sendPacket);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
