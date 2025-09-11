package chatroom;

import java.net.*;
import java.util.Scanner;

public class ChatClient {
    private static final String SERVER_ADDRESS = "localhost"; // có thể đổi sang IP Server thật
    private static final int SERVER_PORT = 4446;

    public static void main(String[] args) {
        try {
            DatagramSocket socket = new DatagramSocket();
            InetAddress serverAddr = InetAddress.getByName(SERVER_ADDRESS);

            Scanner scanner = new Scanner(System.in);
            System.out.print("Nhập tên của bạn: ");
            String name = scanner.nextLine();

            // Luồng nhận tin nhắn từ Server
            Thread receiver = new Thread(() -> {
                byte[] buffer = new byte[1024];
                while (true) {
                    try {
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        socket.receive(packet);
                        String msg = new String(packet.getData(), 0, packet.getLength());
                        System.out.println(msg);
                    } catch (Exception e) {
                        e.printStackTrace();
                        break;
                    }
                }
            });
            receiver.start();

            // Gửi tin nhắn tới Server
            while (true) {
                String message = scanner.nextLine();
                String fullMsg = name + ": " + message;
                byte[] data = fullMsg.getBytes();
                DatagramPacket packet = new DatagramPacket(data, data.length, serverAddr, SERVER_PORT);
                socket.send(packet);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
