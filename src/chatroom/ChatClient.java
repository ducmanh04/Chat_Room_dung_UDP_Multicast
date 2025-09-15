package chatroom;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ChatClient extends JFrame {
    private JPanel chatPanel;
    private JTextField inputField;
    private JButton sendButton;
    private String name;
    private MulticastSocket socket;
    private InetAddress group;
    private int port = 12345;

    public ChatClient(String name) {
        this.name = name;
        setTitle("Chat Client - " + name);
        setSize(400, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        chatPanel = new JPanel();
        chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.Y_AXIS));
        chatPanel.setAlignmentY(Component.TOP_ALIGNMENT);
     // Thêm glue ở cuối
        chatPanel.add(Box.createVerticalGlue());

        JScrollPane scrollPane = new JScrollPane(chatPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        inputField = new JTextField();
        sendButton = new JButton("Gửi");
        sendButton.setBackground(new Color(59, 130, 246));
        sendButton.setForeground(Color.white);

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        add(scrollPane, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);

        try {
            socket = new MulticastSocket(port);
            group = InetAddress.getByName("230.0.0.0");
            socket.joinGroup(group);

            // Thread nhận tin nhắn
            new Thread(() -> {
                byte[] buffer = new byte[1024];
                while (true) {
                    try {
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        socket.receive(packet);
                        String received = new String(packet.getData(), 0, packet.getLength());

                        // Tách thông tin: [Tên]:[Nội dung]
                        String[] parts = received.split(":", 2);
                        String sender = parts[0];
                        String msg = parts.length > 1 ? parts[1] : "";

                        appendMessage(sender, msg, sender.equals(name));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

            // Gửi thông báo tham gia
            sendMessage(name + " đã tham gia chat!");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Sự kiện gửi tin nhắn
        sendButton.addActionListener(e -> sendMessage(inputField.getText()));
        inputField.addActionListener(e -> sendMessage(inputField.getText()));
    }

    private void sendMessage(String msg) {
        if (msg.trim().isEmpty()) return;
        try {
            String fullMsg = name + ":" + msg;
            byte[] buffer = fullMsg.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, port);
            socket.send(packet);
            inputField.setText("");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Hàm hiển thị tin nhắn
    private void appendMessage(String sender, String message, boolean isSelf) {
        // Bong bóng chat
        JLabel messageLabel;
        if (isSelf) {
            messageLabel = new JLabel("<html><div style='padding:6px; background:#3b82f6; color:white; border-radius:8px;'>"
                    + message + "</div></html>");
        } else {
            messageLabel = new JLabel("<html><div style='padding:6px; background:#E0E0E0; border-radius:8px;'>"
                    + message + "</div></html>");
        }

        // Thời gian
        String time = new SimpleDateFormat("HH:mm").format(new Date());
        JLabel timeLabel = new JLabel(time);
        timeLabel.setFont(new Font("Arial", Font.ITALIC, 10));
        timeLabel.setForeground(Color.GRAY);

        // Gom message + time theo chiều dọc
        JPanel bubble = new JPanel();
        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
        bubble.setOpaque(false);
        bubble.add(messageLabel);
        bubble.add(timeLabel);

        // Wrapper canh trái/phải
        JPanel messageWrapper = new JPanel(new BorderLayout());
        messageWrapper.setOpaque(false);
        if (isSelf) {
            messageWrapper.add(bubble, BorderLayout.EAST);
        } else {
            messageWrapper.add(bubble, BorderLayout.WEST);
        }

        // Thêm vào panel chính
        chatPanel.add(messageWrapper);
        chatPanel.add(Box.createVerticalStrut(2)); // khoảng cách nhỏ 2px
        chatPanel.revalidate();
        chatPanel.repaint();

        // Auto scroll xuống cuối
        SwingUtilities.invokeLater(() -> {
            JScrollBar vertical = ((JScrollPane) chatPanel.getParent().getParent()).getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        });
    }

    public static void main(String[] args) {
        String name = JOptionPane.showInputDialog("Nhập tên:");
        if (name != null && !name.trim().isEmpty()) {
            SwingUtilities.invokeLater(() -> new ChatClient(name).setVisible(true));
        }
    }
}
