<h2 align="center">
    <a href="https://dainam.edu.vn/vi/khoa-cong-nghe-thong-tin">
    🎓 Faculty of Information Technology (DaiNam University)
    </a>
</h2>
<h2 align="center">
   TẠO CHATROOM SỬ DỤNG UDP MULTICAST
</h2>
<div align="center">
    <p align="center">
        <img src="docs/aiotlab_logo.png" alt="AIoTLab Logo" width="170"/>
        <img src="docs/fitdnu_logo.png" alt="AIoTLab Logo" width="180"/>
        <img src="docs/dnu_logo.png" alt="DaiNam University Logo" width="200"/>
    </p>

[![AIoTLab](https://img.shields.io/badge/AIoTLab-green?style=for-the-badge)](https://www.facebook.com/DNUAIoTLab)
[![Faculty of Information Technology](https://img.shields.io/badge/Faculty%20of%20Information%20Technology-blue?style=for-the-badge)](https://dainam.edu.vn/vi/khoa-cong-nghe-thong-tin)
[![DaiNam University](https://img.shields.io/badge/DaiNam%20University-orange?style=for-the-badge)](https://dainam.edu.vn)

</div>

## 📖 1. Giới thiệu
Chat Room dùng UDP Multicast là một ứng dụng cho phép nhiều máy tính (hoặc nhiều tiến trình) cùng tham gia vào một “phòng trò chuyện” thông qua cơ chế truyền thông nhóm (multicast).

Trong mô hình này:

Server đóng vai trò quản lý phòng chat, gửi và nhận thông điệp từ nhóm multicast, đồng thời có thể phát tin nhắn đến tất cả Client.

Client là các thành viên tham gia phòng chat, có thể gửi tin nhắn đến nhóm và nhận lại tin nhắn từ các thành viên khác (kể cả từ Server).
## 🔧 2. Công nghệ và ngôn ngữ lập trình sử dụng
🔹 Ngôn ngữ lập trình sử dụng

Java

Dùng Java SE (Standard Edition), phiên bản phổ biến (Java 8 trở lên).

Thư viện sử dụng:

java.net.* (UDP, DatagramSocket, MulticastSocket, InetAddress).

javax.swing.* (tạo giao diện đồ họa).

java.awt.* (hỗ trợ bố cục giao diện).

🔹 Môi trường lập trình

IDE: Eclipse IDE (Eclipse IDE for Java Developers).

Hệ điều hành: Windows.

JDK: JDK 17

Project Type: Java Project (trong Eclipse).

## 🚀 3. Hình ảnh các chức năng
<p align="center">
  <img src="docs/Chat_Server.png" alt="Ảnh 1" width="400"/>
</p>
<p align="center">
  <em>Hình 1: Ảnh giao diện chat Server </em>
</p>
<p align="center">
  <img src="docs/Nhap_ten.png" alt="Ảnh 1" width="400"/>
</p>
<p align="center">
  <em>Hình 2: Ảnh giao diện nhập tên của chat Client</em>
</p>
<p align="center">
  <img src="docs/Chat_Client.png" alt="Ảnh 1" width="400"/>
</p>
<p align="center">
  <em>Hình 3: Ảnh giao diện chat Client</em>
</p>

### [Khoá 16](./docs/projects/K16/README.md)

## 📝 4. License

© 2025 AIoTLab, Faculty of Information Technology, DaiNam University. All rights reserved.

---