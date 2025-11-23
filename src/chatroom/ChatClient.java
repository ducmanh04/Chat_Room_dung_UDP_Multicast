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
    private RoomLobby lobby; // Biến tham chiếu đến Lobby
    private String roomOwner; // [BỔ SUNG] Biến lưu tên chủ phòng hiện tại
    private JMenuItem giveAdminMenuItem; // [BỔ SUNG] Menu item để trao quyền

    private DefaultListModel<String> participantsModel = new DefaultListModel<>();
    private JList<String> participantsList = new JList<>(participantsModel);
    
    private final String LOBBY_UPDATE_IP = "230.0.0.250"; // IP Multicast riêng cho thông báo Lobby
    private final int LOBBY_UPDATE_PORT = 4447; // Port riêng cho thông báo Lobby

    // Constructor đã sửa đổi để nhận 5 tham số (kể cả roomOwner)
    public ChatClient(String name, String serverAddress, int port, RoomLobby roomLobby, String roomOwner) {
        this.name = name;
        this.port = port;
        this.serverAddress = serverAddress;
        this.lobby = roomLobby;    
        this.roomOwner = roomOwner; // GÁN CHỦ PHÒNG
        
        setTitle("🌈 Chat Client - " + name + " @" + serverAddress + ":" + port);
        setSize(650, 520);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Tạo Gradient Background
        setContentPane(new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                GradientPaint gp = new GradientPaint(
                    0, 0, Color.decode("#FFFFCC"),
                    getWidth(), getHeight(), Color.decode("#99FF99")
                );
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        });

        setLayout(new BorderLayout());

        // ====== Chat panel ======
        chatPanel = new JPanel();
        chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.Y_AXIS));
        chatPanel.setOpaque(false);

        JScrollPane chatScroll = new JScrollPane(chatPanel);
        chatScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        chatScroll.setBorder(BorderFactory.createTitledBorder("💬 Nội dung chat"));
        chatScroll.setOpaque(false);
        chatScroll.getViewport().setOpaque(false);

        // ====== Input + Buttons ======
        inputField = new JTextField();
        inputField.setFont(new Font("Arial", Font.PLAIN, 14));
        inputField.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 2));

        sendButton = new JButton("🚀 Gửi");
        sendButton.setBackground(new Color(59, 130, 246));
        sendButton.setForeground(Color.white);
        sendButton.setFocusPainted(false);

        switchRoomButton = new JButton("🔄 Đổi phòng");
        switchRoomButton.setBackground(new Color(255, 165, 0));
        switchRoomButton.setForeground(Color.white);
        switchRoomButton.setFocusPainted(false);

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setOpaque(false);
        inputPanel.add(inputField, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 5, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.add(sendButton);
        buttonPanel.add(switchRoomButton);

        inputPanel.add(buttonPanel, BorderLayout.EAST);

        // ====== Participants list ======
        participantsList.setBorder(BorderFactory.createTitledBorder("👥 Người tham gia"));
        participantsList.setOpaque(false);
        participantsList.setFont(new Font("Arial", Font.PLAIN, 13));
        participantsList.setBackground(new Color(0, 0, 0, 0));

        JScrollPane participantsScroll = new JScrollPane(participantsList);
        participantsScroll.setPreferredSize(new Dimension(160, 0));
        participantsScroll.setOpaque(false);
        participantsScroll.getViewport().setOpaque(false);

        // [BỔ SUNG]: Setup Menu Trao quyền Admin
        giveAdminMenuItem = new JMenuItem("Trao quyền Chủ phòng (Admin)");
        JPopupMenu participantMenu = new JPopupMenu();
        participantMenu.add(giveAdminMenuItem);

        participantsList.setComponentPopupMenu(participantMenu);
        
        // Xử lý sự kiện trao quyền
        giveAdminMenuItem.addActionListener(e -> {
            String selectedUser = participantsList.getSelectedValue();
            if (selectedUser == null) return;
            
            if (selectedUser.equals(name)) {
                JOptionPane.showMessageDialog(this, "Bạn đã là chủ phòng.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            if (name.equals(roomOwner)) { // Chỉ chủ phòng mới có thể trao quyền
                broadcastNewOwner(selectedUser);
            } else {
                JOptionPane.showMessageDialog(this, "Bạn không phải là chủ phòng.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });


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
                            appendMessage("🔵 [SYSTEM] " + sender + " đã tham gia nhóm", "", false);

                            if (!sender.equals(name)) {
                                sendMessage("__EXIST__");
                            }

                        } else if (msg.equals("__EXIST__")) {
                            if (!participantsModel.contains(sender)) {
                                participantsModel.addElement(sender);
                            }

                        } else if (msg.equals("__LEAVE__")) {
                            participantsModel.removeElement(sender);
                            appendMessage("🔴 [SYSTEM] " + sender + " đã rời khỏi nhóm", "", false);

                        } else if (msg.startsWith("__NEW_OWNER__:")) { // [BỔ SUNG] Xử lý tin nhắn Admin mới
                            String newOwner = msg.substring(14);
                            handleNewOwner(newOwner);

                        } else {
                            boolean isSelf = sender.equals(name);
                            appendMessage(sender, msg, isSelf);
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

        // Đổi phòng (Quay về Lobby)
        switchRoomButton.addActionListener(e -> {
            // Gửi rời phòng cũ
            sendMessage("__LEAVE__");

            // Đóng cửa sổ hiện tại
            dispose();

            // Quay lại màn hình Lobby
            SwingUtilities.invokeLater(() -> lobby.setVisible(true));
        });

        // Khi đóng cửa sổ → gửi thông báo rời
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent e) {
                sendMessage("__LEAVE__");
                // Kiểm tra và thông báo nếu mình là người cuối cùng
                new Thread(() -> {
                    try {
                        Thread.sleep(500); // Đợi nửa giây
                        // participantsModel.size() == 1 vì tin LEAVE của mình chưa kịp xử lý
                        if (participantsModel.size() <= 1) { 
                            sendRoomEmptyNotification();
                        }
                    } catch (InterruptedException ex) { }
                }).start();
            }
        });
    }

    // [BỔ SUNG] Phát thông báo Admin mới
    private void broadcastNewOwner(String newOwnerName) {
        sendMessage("__NEW_OWNER__:" + newOwnerName);
        
        // [BỔ SUNG] Gửi thông báo đến Lobby Channel để các cửa sổ Lobby cập nhật
        sendLobbyUpdate("OWNER:" + serverAddress + ":" + port + ":" + newOwnerName);
    }

    // [BỔ SUNG] Xử lý khi nhận được thông báo Admin mới
    private void handleNewOwner(String newOwnerName) {
        this.roomOwner = newOwnerName;
        appendMessage("👑 [SYSTEM] " + newOwnerName + " đã trở thành Chủ phòng mới.", "", false);
    }
    
    // [BỔ SUNG] Gửi thông báo phòng trống (REMOVE)
    private void sendRoomEmptyNotification() {
        sendLobbyUpdate("REMOVE:" + serverAddress + ":" + port);
    }
    
    // [HÀM MỚI] Gửi tin nhắn đến kênh cập nhật Lobby chung
    private void sendLobbyUpdate(String updateMsg) {
        try (MulticastSocket tempSocket = new MulticastSocket()) {
            InetAddress groupIP = InetAddress.getByName(LOBBY_UPDATE_IP);
            String fullMsg = "LOBBY_UPDATE:" + updateMsg;
            byte[] buffer = fullMsg.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, groupIP, LOBBY_UPDATE_PORT);
            tempSocket.send(packet);
        } catch (IOException e) {
            System.err.println("Lỗi gửi Lobby Update: " + e.getMessage());
        }
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
            messageLabel = new JLabel(
                "<html><div style='padding:6px; background:#3b82f6; color:white; border-radius:8px;'>"
                + message + "</div></html>"
            );
        } else {
            messageLabel = new JLabel(
                "<html><div style='padding:6px; background:#E0E0E0; border-radius:8px;'>"
                + sender + ": " + message + "</div></html>"
            );
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
        chatPanel.add(Box.createVerticalStrut(2));
        chatPanel.revalidate();
        chatPanel.repaint();

        SwingUtilities.invokeLater(() -> {
            JScrollBar vertical = ((JScrollPane) chatPanel.getParent().getParent()).getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        });
    }

    // XÓA HÀM main CŨ. Hàm main mới sẽ nằm trong RoomLobby.java
    // public static void main(String[] args) {
    //     // Mã này bị loại bỏ
    // }
}