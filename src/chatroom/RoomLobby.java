package chatroom;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.List;

public class RoomLobby extends JFrame {
	
    private final String ROOM_SYNC_IP = "230.0.0.251"; 
    private final int ROOM_SYNC_PORT = 4448;
    private final String MSG_SYNC_REQUEST = "SYNC_REQUEST";
    private final String MSG_SYNC_RESPONSE = "SYNC_RESPONSE";

    // Lớp nội bộ để mô phỏng thông tin phòng
    private static class ChatRoom {
        String name;
        String server;
        int port;
        String owner;
        int members; 

        public ChatRoom(String name, String server, int port, String owner, int members) {
            this.name = name;
            this.server = server;
            this.port = port;
            this.owner = owner;
            this.members = members;
        }

        public String[] toRow() {
            return new String[]{name, server, String.valueOf(port), owner, String.valueOf(members)};
        }
    }

    private JTable roomTable;
    private List<ChatRoom> rooms = new ArrayList<>();
    
    private final String[] columnNames = {"Tên Phòng", "Server IP", "Port", "Chủ Phòng", "SL Thành viên"};
    

    public RoomLobby() {
        setTitle("🏠 Danh sách Phòng Chat (Lobby)");
        setSize(700, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // 1. Giao diện Danh sách Phòng
        // Khởi tạo JTable với DefaultTableModel rỗng
        roomTable = new JTable(new DefaultTableModel(new Object[][]{}, columnNames));
        roomTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(roomTable);
        
        // Nút chức năng
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton joinButton = new JButton("➡️ Tham gia Phòng");
        JButton createButton = new JButton("➕ Tạo Phòng Mới");
        
        startRoomSyncListener();
        
        // [BỔ SUNG] Khởi động Listener cho Lobby Update
        startLobbyUpdateListener();
        
        // Thêm các thành phần vào Frame
        add(scrollPane, BorderLayout.CENTER);
        buttonPanel.add(joinButton);
        buttonPanel.add(createButton);
        add(buttonPanel, BorderLayout.SOUTH);

        // Xử lý sự kiện Tham gia
        joinButton.addActionListener(e -> {
            int selectedRow = roomTable.getSelectedRow();
            if (selectedRow != -1) {
                // Lấy thông tin từ model của JTable
                String roomName = (String) roomTable.getValueAt(selectedRow, 0);
                String server = (String) roomTable.getValueAt(selectedRow, 1);
                int port = Integer.parseInt((String) roomTable.getValueAt(selectedRow, 2));
                String owner = (String) roomTable.getValueAt(selectedRow, 3);
                
                // [SỬA] Cần truyền đủ 5 tham số: roomOwner là owner, isNewRoom=false
                startClient(server, port, roomName, false, owner);	
            } else {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn một phòng để tham gia.", "Lỗi", JOptionPane.WARNING_MESSAGE);
            }
        });

        // Xử lý sự kiện Tạo phòng (2)
        createButton.addActionListener(e -> showCreateRoomDialog());
    }
    
    private void startLobbyUpdateListener() {
		// TODO Auto-generated method stub
		
	}

	// Khởi động Listener nhận thông báo phòng mới
    private void startRoomSyncListener() {
        try {
            MulticastSocket syncSocket = new MulticastSocket(ROOM_SYNC_PORT);
            InetAddress syncGroup = InetAddress.getByName(ROOM_SYNC_IP);
            syncSocket.joinGroup(syncGroup);

            new Thread(() -> {
                byte[] buffer = new byte[1024];
                while (true) {
                    try {
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        syncSocket.receive(packet);
                        String received = new String(packet.getData(), 0, packet.getLength()).trim();

                        if (received.startsWith("NEW_ROOM:")) {
                            // Xử lý thông báo phòng mới
                            handleNewRoomSync(received.substring(9)); 
                        } else if (received.equals(MSG_SYNC_REQUEST)) {
                            // [BỔ SUNG] Xử lý yêu cầu đồng bộ từ Lobby khác
                            handleSyncRequest();
                        } else if (received.startsWith(MSG_SYNC_RESPONSE + ":")) {
                            // [BỔ SUNG] Xử lý phản hồi (dữ liệu phòng)
                            handleNewRoomSync(received.substring((MSG_SYNC_RESPONSE + ":").length())); 
                        }
                    } catch (IOException e) {
                        System.err.println("Lỗi nhận gói tin đồng bộ phòng: " + e.getMessage());
                        break;
                    }
                }
            }).start();

            // [BỔ SUNG] Sau khi listener chạy, gửi yêu cầu đồng bộ hóa
            sendSyncRequest();

        } catch (Exception e) {
            System.err.println("Lỗi khởi tạo Room Sync Listener: " + e.getMessage());
        }
    }
    private void sendSyncRequest() {
        try (MulticastSocket tempSocket = new MulticastSocket()) {
            InetAddress groupIP = InetAddress.getByName(ROOM_SYNC_IP);
            
            // Gửi tin nhắn yêu cầu
            byte[] buffer = MSG_SYNC_REQUEST.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, groupIP, ROOM_SYNC_PORT);
            tempSocket.send(packet);
            System.out.println("📢 Gửi yêu cầu đồng bộ phòng...");
        } catch (IOException e) {
            System.err.println("Lỗi gửi Sync Request: " + e.getMessage());
        }
    }

    // [HÀM MỚI] Xử lý yêu cầu đồng bộ (các Lobby đang chạy sẽ trả lời)
    private void handleSyncRequest() {
        // Duyệt qua tất cả các phòng đang có (trừ phòng mặc định nếu có)
        for (ChatRoom room : rooms) {
            // Gửi thông tin phòng đó dưới dạng SYNC_RESPONSE
            String roomInfo = room.name + ":" + room.server + ":" + room.port + ":" + room.owner;
            sendSyncResponse(roomInfo);
        }
    }

    // [HÀM MỚI] Gửi thông tin phòng đã tạo khi có yêu cầu SYNC_REQUEST
    private void sendSyncResponse(String roomInfo) {
        try (MulticastSocket tempSocket = new MulticastSocket()) {
            InetAddress groupIP = InetAddress.getByName(ROOM_SYNC_IP);
            
            // Định dạng: SYNC_RESPONSE:[Tên phòng]:[IP Server]:[Port Server]:[Chủ phòng]
            String fullMsg = MSG_SYNC_RESPONSE + ":" + roomInfo;
            byte[] buffer = fullMsg.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, groupIP, ROOM_SYNC_PORT);
            tempSocket.send(packet);
        } catch (IOException e) {
            // Lỗi này thường gặp khi một Lobby phản hồi yêu cầu
            // System.err.println("Lỗi gửi Sync Response: " + e.getMessage()); 
        }
    }

    private void handleNewRoomSync(String roomInfo) {
        String[] parts = roomInfo.split(":");
        if (parts.length < 4) return;

        String name = parts[0];
        String server = parts[1];
        int port = Integer.parseInt(parts[2]);
        String owner = parts[3];

        SwingUtilities.invokeLater(() -> {
            // Kiểm tra trùng lặp trước khi thêm
            boolean exists = rooms.stream()
                                    .anyMatch(r -> r.server.equals(server) && r.port == port);
            
            if (!exists) {
                ChatRoom newRoom = new ChatRoom(name, server, port, owner, 1);
                rooms.add(newRoom);
                refreshRoomTable();
                System.out.println("✅ Đã đồng bộ phòng mới: " + name);
            }
        });
    }
    
    // [BỔ SUNG] Xử lý cập nhật từ ChatClient (OWNER/REMOVE)
    private void handleLobbyUpdate(String updateMsg) {
        SwingUtilities.invokeLater(() -> {
            String[] parts = updateMsg.split(":");
            if (parts.length < 3) return; // REMOVE:[IP]:[Port]

            String command = parts[0]; 
            String server = parts[1];
            int port = Integer.parseInt(parts[2]);
            String roomKey = server + ":" + port;

            if (command.equals("REMOVE")) {
                rooms.removeIf(room -> (room.server + ":" + room.port).equals(roomKey));
                refreshRoomTable();
                System.out.println("❌ Phòng đã bị gỡ: " + roomKey);

            } else if (command.equals("OWNER") && parts.length >= 4) { // OWNER:[IP]:[Port]:[NewOwner]
                String newOwner = parts[3];
                rooms.stream()
                     .filter(room -> (room.server + ":" + room.port).equals(roomKey))
                     .findFirst()
                     .ifPresent(room -> {
                         room.owner = newOwner;
                         refreshRoomTable();
                         System.out.println("👑 Chủ phòng đã cập nhật cho " + roomKey + ": " + newOwner);
                     });
            }
        });
    }

    // Hàm hiển thị Dialog tạo phòng mới
    private void showCreateRoomDialog() {
        JTextField nameField = new JTextField();
        JTextField serverField = new JTextField("230.0.0.0");    
        JTextField portField = new JTextField("12345");

        JPanel panel = new JPanel(new GridLayout(3, 2));
        panel.add(new JLabel("Tên Phòng:"));
        panel.add(nameField);
        panel.add(new JLabel("Server (IP Multicast):"));
        panel.add(serverField);
        panel.add(new JLabel("Port:"));
        panel.add(portField);

        int result = JOptionPane.showConfirmDialog(
            this, panel,
            "Tạo Phòng Chat Mới",
            JOptionPane.OK_CANCEL_OPTION
        );

        if (result == JOptionPane.OK_OPTION) {
            try {
                String name = nameField.getText().trim();
                String server = serverField.getText().trim();
                int port = Integer.parseInt(portField.getText().trim());
                
                if (name.isEmpty() || server.isEmpty()) {
                     JOptionPane.showMessageDialog(this, "Vui lòng nhập đủ thông tin.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                     return;
                }
                
                // [SỬA] Gọi startClient với 4 tham số đầu. Tham số cuối (roomOwner) là null vì chưa biết tên người dùng.
                startClient(server, port, name, true, null); // true: là tạo phòng mới, null: owner tạm thời
                

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Port phải là một số nguyên hợp lệ.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    // Hàm cập nhật lại bảng phòng
    private void refreshRoomTable() {
        // [SỬA] Phải sử dụng lại DefaultTableModel để JTable cập nhật lại dữ liệu
        Object[][] data = rooms.stream().map(ChatRoom::toRow).toArray(Object[][]::new);
        roomTable.setModel(new DefaultTableModel(data, columnNames));
    }
    
    // [SỬA] Hàm khởi động ChatClient và đóng Lobby - Bổ sung tham số currentOwner
    private void startClient(String server, int port, String roomName, boolean isNewRoom, String currentOwner) {
        String userName = JOptionPane.showInputDialog(this, "Nhập tên người dùng của bạn:");
        
        if (userName != null && !userName.trim().isEmpty()) {
            
            String ownerForClient; // Khai báo biến mới

            // Logic xác định giá trị finalOwner/ownerForClient 
            if (isNewRoom) {
                ownerForClient = userName; // Chủ phòng là người tạo
                
                // Cập nhật danh sách phòng ngay lập tức (chỉ xảy ra khi tạo phòng mới)
                ChatRoom newRoom = new ChatRoom(roomName, server, port, ownerForClient, 1);
                rooms.add(newRoom);
                refreshRoomTable();
                sendNewRoomSync(roomName, server, port, ownerForClient);
            } else {
                ownerForClient = currentOwner; // Tham gia phòng đã có, owner giữ nguyên
            }
            
            this.dispose(); // Đóng giao diện Lobby
            
            // Biến ownerForClient hiện tại là effectively final 
            // vì nó chỉ được gán duy nhất một lần trong khối if/else
            SwingUtilities.invokeLater(() ->
                // Sử dụng biến ownerForClient đã được xác định giá trị và là effectively final
                new ChatClient(userName, server, port, this, ownerForClient).setVisible(true)
            );
        }
    }

    private void sendNewRoomSync(String name, String server, int port, String owner) {
        try (MulticastSocket tempSocket = new MulticastSocket()) {
            InetAddress groupIP = InetAddress.getByName(ROOM_SYNC_IP);
            
            // Định dạng: SYNC_RESPONSE:[Tên phòng]:[IP Server]:[Port Server]:[Chủ phòng]
            String fullMsg = MSG_SYNC_RESPONSE + ":" + name + ":" + server + ":" + port + ":" + owner;
            byte[] buffer = fullMsg.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, groupIP, ROOM_SYNC_PORT);
            tempSocket.send(packet);
            System.out.println("🚀 Phát sóng thông tin phòng mới: " + name);
        } catch (IOException e) {
            System.err.println("Lỗi gửi Room Sync: " + e.getMessage());
        }
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new RoomLobby().setVisible(true));
    }
}