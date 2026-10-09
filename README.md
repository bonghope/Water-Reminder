# WaterReminder: giao diện mới, backend gốc

Giao diện, tài nguyên XML, biểu đồ, thú cưng và nhắc nhở lấy từ WaterReminder_2.
Các file database/, models/ và services/ trong com.example.waterreminder giữ nguyên từng byte từ D:/WaterReminder trước khi ghép.
WaterStore là lớp kết nối giao diện mới với DatabaseHelper gốc, không tạo bảng theo schema của WaterReminder_2.
Giữ applicationId com.example.waterreminder và WaterReminder.db phiên bản 1 để tiếp tục đọc dữ liệu gốc.
Tổng lượng nước sử dụng hydration_amount: nước và sữa 100%, trà/cà phê 80%, nước ngọt 70% như logic gốc.
Backend gốc gộp trà và cà phê vào category 2, nên lịch sử hiển thị Trà / Cà phê.
Dịch vụ nhắc nhở của giao diện mới dùng package com.aqua.water.services riêng; services gốc vẫn được giữ nguyên.
Không sao chép cache build, .gradle hay .idea từ WaterReminder_2.
Mở thư mục dự án bằng Android Studio; build bằng gradlew.bat :app:assembleDebug.

Thẻ thêm nước được giữ theo UI gốc: 4 loại đồ uống cùng icon gốc, nút Khác/100/200/500, đóng/mở thẻ và liên kết lịch sử. Loại đang chọn được giữ khi thêm nước và xoay màn hình.

Trang chủ bỏ phần Lần uống gần đây; thẻ thêm nước rộng toàn màn hình trong vùng an toàn của hệ thống, kể cả màn hình lớn. Các phần tiêu đề/tiến độ giữ khoảng đệm hai bên.

Trang chủ bỏ hai dòng giới thiệu, thu gọn tiến độ/khoảng cách. Điều hướng giữ 6 màn hình, icon kiểu gốc trên nền trắng và nút giọt nước giữa để về trang chủ/mở thẻ thêm nước.

Thẻ thêm nước là lớp phủ sát đáy vùng màn hình an toàn, che thanh điều hướng khi mở. Bấm X để lộ thanh điều hướng; nút giọt nước giữa mở lại thẻ. Thẻ không còn nằm trong nội dung cuộn của trang chủ.

Lịch sử: vuốt trái để mở nút Sửa/Xóa, vuốt phải để đóng; chỉnh sửa loại đồ uống và lượng ml. Vuốt dọc vẫn cuộn danh sách.

Các bản ghi lịch sử và hộp thoại thêm/sửa/xóa dùng góc bo 22dp theo bg_card, hỗ trợ màu giao diện sáng/tối. Bản ghi giữ bo góc khi vuốt lộ nút hành động.
