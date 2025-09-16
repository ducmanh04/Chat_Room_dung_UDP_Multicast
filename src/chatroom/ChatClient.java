package chatroom;

import javax.swing.*;
import java.awt.*;
import java.net.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class ChatClient extends JFrame {
    private JPanel chatPanel;
    private JTextField inputField;
    private JButton sendButton, switchRoomButton;
    private String name;
    private MulticastSocket socket;
    private InetAddress group;
    private int port;
    private String serverAddress;

    private DefaultListModel<String> participantsModel = new DefaultListModel<>();
    private JList<String> participantsList = new JList<>(participantsModel);

    public ChatClient(String name, String serverAddress, int port) {
        this.name = name;
        this.port = port;
        this.serverAddress = serverAddress;

        setTitle("Chat Client - " + name + " @" + serverAddress + ":" + port);
        setSize(600, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // ====== Chat panel ======
        chatPanel = new JPanel();
        chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.Y_AXIS));
        chatPanel.setAlignmentX(Component.LEFT_ALIGNMENT);


        JScrollPane chatScroll = new JScrollPane(chatPanel);
        chatScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        inputField = new JTextField();
        sendButton = new JButton("Gửi");
        sendButton.setBackground(new Color(59, 130, 246));
        sendButton.setForeground(Color.white);

        switchRoomButton = new JButton("Đổi phòng");

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(inputField, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
        buttonPanel.add(sendButton);
        buttonPanel.add(switchRoomButton);

        inputPanel.add(buttonPanel, BorderLayout.EAST);

        // ====== Participants list ======
        participantsList.setBorder(BorderFactory.createTitledBorder("Người tham gia"));
        JScrollPane participantsScroll = new JScrollPane(participantsList);
        participantsScroll.setPreferredSize(new Dimension(150, 0));

        // ====== Layout chính ======
        add(chatScroll, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);
        add(participantsScroll, BorderLayout.EAST);

        try {
            socket = new MulticastSocket(port);
            group = InetAddress.getByName(serverAddress);
            socket.joinGroup(group);

            // Thread nhận tin nhắn
            new Thread(() -> {
                byte[] buffer = new byte[1024];
                while (true) {
                    try {
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        socket.receive(packet);
                        String received = new String(packet.getData(), 0, packet.getLength());

                        String[] parts = received.split(":", 2);
                        String sender = parts[0];
                        String msg = parts.length > 1 ? parts[1] : "";

                        if (msg.equals("__JOIN__")) {
                            if (!participantsModel.contains(sender)) {
                                participantsModel.addElement(sender);
                            }
                            appendMessage("🔵 " + sender + " tham gia phòng", "", false);
                        } else if (msg.equals("__LEAVE__")) {
                            participantsModel.removeElement(sender);
                            appendMessage("🔴 " + sender + " rời phòng", "", false);
                        } else {
                            appendMessage(sender, msg, sender.equals(name));
                        }
                    } catch (IOException e) {
                        break;
                    }
                }
            }).start();

            // Gửi thông báo tham gia
            sendMessage("__JOIN__");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Gửi tin nhắn
        sendButton.addActionListener(e -> sendMessage(inputField.getText()));
        inputField.addActionListener(e -> sendMessage(inputField.getText()));

        // Đổi phòng
        switchRoomButton.addActionListener(e -> {
            JTextField serverField = new JTextField(serverAddress);
            JTextField portField = new JTextField(String.valueOf(port));

            JPanel switchPanel = new JPanel(new GridLayout(2, 2));
            switchPanel.add(new JLabel("Server (IP Multicast):"));
            switchPanel.add(serverField);
            switchPanel.add(new JLabel("Port:"));
            switchPanel.add(portField);

            int result = JOptionPane.showConfirmDialog(
                    this, switchPanel, "Đổi phòng chat", JOptionPane.OK_CANCEL_OPTION);

            if (result == JOptionPane.OK_OPTION) {
                try {
                    String newServer = serverField.getText().trim();
                    int newPort = Integer.parseInt(portField.getText().trim());

                    // Gửi rời phòng cũ
                    sendMessage("__LEAVE__");

                    // Đóng cửa sổ hiện tại
                    dispose();

                    // Tạo cửa sổ phòng mới
                    SwingUtilities.invokeLater(() ->
                            new ChatClient(name, newServer, newPort).setVisible(true));

                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this,
                            "Lỗi đổi phòng: " + ex.getMessage(),
                            "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
            // Nếu Cancel thì không làm gì, vẫn ở phòng cũ
        });
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

    private void appendMessage(String sender, String message, boolean isSelf) {
        JLabel messageLabel;
        if (message.isEmpty()) {
            messageLabel = new JLabel(sender); // dùng cho join/leave
        } else if (isSelf) {
            messageLabel = new JLabel("<html><div style='padding:6px; background:#3b82f6; color:white; border-radius:8px;'>"
                    + message + "</div></html>");
        } else {
            messageLabel = new JLabel("<html><div style='padding:6px; background:#E0E0E0; border-radius:8px;'>"
                    + sender + ": " + message + "</div></html>");
        }

        String time = new SimpleDateFormat("HH:mm").format(new Date());
        JLabel timeLabel = new JLabel(time);
        timeLabel.setFont(new Font("Arial", Font.ITALIC, 10));
        timeLabel.setForeground(Color.GRAY);

        JPanel bubble = new JPanel();
        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
        bubble.setOpaque(false);
        bubble.add(messageLabel);
        bubble.add(timeLabel);

        JPanel messageWrapper = new JPanel(new BorderLayout());
        messageWrapper.setOpaque(false);
        if (isSelf) {
            messageWrapper.add(bubble, BorderLayout.EAST);
        } else {
            messageWrapper.add(bubble, BorderLayout.WEST);
        }

        chatPanel.add(messageWrapper);
        chatPanel.add(Box.createVerticalStrut(5));
        chatPanel.add(Box.createVerticalGlue());
        chatPanel.revalidate();
        chatPanel.repaint();

        SwingUtilities.invokeLater(() -> {
            JScrollBar vertical = ((JScrollPane) chatPanel.getParent().getParent()).getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        });
    }

    public static void main(String[] args) {
        JTextField nameField = new JTextField();
        JTextField serverField = new JTextField("230.0.0.0");
        JTextField portField = new JTextField("12345");

        JPanel panel = new JPanel(new GridLayout(3, 2));
        panel.add(new JLabel("Tên:"));
        panel.add(nameField);
        panel.add(new JLabel("Server (IP Multicast):"));
        panel.add(serverField);
        panel.add(new JLabel("Port:"));
        panel.add(portField);

        int result = JOptionPane.showConfirmDialog(null, panel,
                "Nhập thông tin để tham gia chat", JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            String name = nameField.getText().trim();
            String server = serverField.getText().trim();
            int port = Integer.parseInt(portField.getText().trim());

            if (!name.isEmpty()) {
                SwingUtilities.invokeLater(() -> new ChatClient(name, server, port).setVisible(true));
            }
        }
    }
}
