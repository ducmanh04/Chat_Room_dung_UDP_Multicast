package chatroom;

import javax.swing.*;
import java.awt.*;
import java.net.*;

public class ChatClient extends JFrame {
    private JTextArea textArea;
    private JTextField textField;
    private DatagramSocket socket; // chỉ để gửi
    private InetAddress group;
    private int port = 5555;
    private String clientName;

    public ChatClient(String name) {
        this.clientName = name;
        setTitle("Chat Client - " + name);
        setSize(400, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        textArea = new JTextArea();
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);

        textField = new JTextField();
        JButton sendButton = new JButton("Send");

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(textField, BorderLayout.CENTER);
        panel.add(sendButton, BorderLayout.EAST);

        add(scrollPane, BorderLayout.CENTER);
        add(panel, BorderLayout.SOUTH);

        try {
            socket = new DatagramSocket(); // để hệ điều hành chọn port gửi
            group = InetAddress.getByName("230.0.0.1");
        } catch (Exception e) {
            e.printStackTrace();
        }

        sendButton.addActionListener(e -> sendMessage());
        textField.addActionListener(e -> sendMessage());

        // Thread nhận tin nhắn
        new Thread(this::receiveMessages).start();
    }

    private void sendMessage() {
        try {
            String message = clientName + ": " + textField.getText();
            byte[] buffer = message.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, port);
            socket.send(packet);
            textField.setText("");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void receiveMessages() {
        try {
            group = InetAddress.getByName("230.0.0.1");

            MulticastSocket msocket = new MulticastSocket(null);
            msocket.setReuseAddress(true);
            msocket.bind(new InetSocketAddress(port));
            msocket.joinGroup(group);

            byte[] buffer = new byte[1024];
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                msocket.receive(packet);
                String msg = new String(packet.getData(), 0, packet.getLength());
                if (!msg.startsWith(clientName + ":")) {
                    textArea.append(msg + "\n");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String name = JOptionPane.showInputDialog("Nhập tên của bạn");
        SwingUtilities.invokeLater(() -> new ChatClient(name).setVisible(true));
    }
}
