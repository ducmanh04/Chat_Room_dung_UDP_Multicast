package chatroom;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ChatClient {
    private static final String MULTICAST_ADDRESS = "230.0.0.0"; // địa chỉ multicast
    private static final int PORT = 4446;

    public static void main(String[] args) {
        try {
            InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
            MulticastSocket socket = new MulticastSocket(PORT);

            // Tham gia nhóm multicast
            socket.joinGroup(group);

            // Luồng nhận tin nhắn
            Thread receiver = new Thread(() -> {
                byte[] buffer = new byte[1024];
                while (true) {
                    try {
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        socket.receive(packet);
                        String msg = new String(packet.getData(), 0, packet.getLength());
                        System.out.println("📩 " + msg);
                    } catch (IOException e) {
                        e.printStackTrace();
                        break;
                    }
                }
            });

            receiver.start();

            // Luồng gửi tin nhắn
            Scanner scanner = new Scanner(System.in);
            System.out.print("Nhập tên của bạn: ");
            String name = scanner.nextLine();

            while (true) {
                String message = scanner.nextLine();
                String fullMsg = name + ": " + message;
                byte[] buffer = fullMsg.getBytes();
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, PORT);
                socket.send(packet);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
