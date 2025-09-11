package chatroom;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.io.*;

public class Client extends JFrame {
    private JTextArea chatArea;
    private JTextField inputField;
    private JButton sendButton;
    private MulticastSocket socket;
    private InetAddress group;
    private int port = 4446;
    private String username;

    public Client(String username) {
        this.username = username;
        setTitle("Chat Client - " + username);
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        inputField = new JTextField();
        sendButton = new JButton("Gửi");

        add(new JScrollPane(chatArea), BorderLayout.CENTER);
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(inputField, BorderLayout.CENTER);
        panel.add(sendButton, BorderLayout.EAST);
        add(panel, BorderLayout.SOUTH);

        try {
        	socket = new MulticastSocket(0); // hệ thống tự chọn port trống
        	group = InetAddress.getByName("230.0.0.1");
        	socket.joinGroup(group);

            // Thread lắng nghe tin nhắn
            Thread listener = new Thread(() -> {
                byte[] buffer = new byte[1024];
                while (true) {
                    try {
                    	DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, port);
                    	socket.send(packet);
                        String msg = new String(packet.getData(), 0, packet.getLength());
                        chatArea.append(msg + "\n");
                    } catch (IOException e) {
                        e.printStackTrace();
                        break;
                    }
                }
            });
            listener.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());
    }

    private void sendMessage() {
        try {
            String msg = username + ": " + inputField.getText();
            byte[] buffer = msg.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, port);
            socket.send(packet);
            inputField.setText("");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String username = JOptionPane.showInputDialog("Nhập tên của bạn:");
        if (username != null && !username.trim().isEmpty()) {
            SwingUtilities.invokeLater(() -> new Client(username).setVisible(true));
        }
    }
}
