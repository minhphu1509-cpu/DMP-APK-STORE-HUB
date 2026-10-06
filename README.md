# DMP APK STORE HUB

Ứng dụng thư viện APK tối giản cho Android TV Box/Smart TV, dùng remote D-pad và hỗ trợ Android 4.4 trở lên. Danh mục và bốn tệp APK hiện được đóng gói trong ứng dụng.

## Tính năng

- Trang chủ tối giản, danh mục và tìm kiếm.
- Trang thông tin có biểu tượng gốc, phiên bản, yêu cầu Android và trạng thái cài đặt.
- Mở trình cài đặt hệ thống; sau khi cài xong có lựa chọn mở ứng dụng hoặc quay lại DMP APK STORE HUB.
- Logo DMP Store làm biểu tượng APK và nhận diện ở giao diện.
- Điều hướng bằng remote, giao diện ngang tối ưu cho TV.

## Tải DMP Store bằng Downloader

- Mở ứng dụng Downloader trên TV và nhập mã: **8535968**.
- Liên kết ngắn: [aftv.news/8535968](https://aftv.news/8535968).
- [Tải bản mới nhất](https://github.com/minhphu1509-cpu/DMP-APK-STORE-HUB/releases/latest/download/dmp-store.apk) từ GitHub Releases.

Mã Downloader này trỏ đến liên kết tải bản mới nhất. Khi phát hành phiên bản mới, giữ tên asset `dmp-store.apk` để mã hiện tại tiếp tục hoạt động.

## APK được đưa vào thư viện

Các tệp gốc được giữ trong `File APK/`; bản sao dùng trong ứng dụng nằm trực tiếp tại `app/src/main/assets/` để tương thích với AssetManager trên Android 4.4+.

| Ứng dụng | APK | Android tối thiểu |
|---|---|---:|
| DMP Smart TV | `app-release.apk` | 4.4 |
| MovieLegend Store | `movie-legend.apk` | 5.0 |
| VietMITV | `vietmitv.apk` | 6.0 |
| SIRO-TV | `SIRO-ATV.apk` | 6.0 |

Bốn APK chiếm khoảng 47,9 MB; các biểu tượng gốc được trích từ từng APK và hiển thị trên thẻ ứng dụng. Các APK được sao chép vào bộ nhớ đệm của TV khi người dùng chọn cài. Một số tệp có thể chỉ hỗ trợ kiến trúc CPU nhất định, Android installer sẽ báo nếu thiết bị không tương thích.

## Build

Yêu cầu Android SDK Platform 36, Build Tools 36.0.0 và JDK có trong Android Studio. Chạy lệnh sau tại thư mục dự án:

```powershell
powershell -ExecutionPolicy Bypass -File .\build.ps1
```

APK đầu ra: `app/build/outputs/apk/manual/dmp-store-debug.apk`. Đây là bản ký khóa debug để cài thử; tạo khóa phát hành riêng trước khi phân phối công khai.

## Cấu hình thư viện

Thông tin hiển thị được quản lý trong `app/src/main/assets/catalog.json`. Tên tệp trong trường `file` phải trùng với tên APK trong `app/src/main/assets/`. `minSdk` là API tối thiểu đọc từ manifest của APK.

Trước khi phát hành, xác minh quyền phân phối và chữ ký của từng APK. Cài đặt hoặc cập nhật ứng dụng có thể bị Android từ chối nếu chữ ký khác với bản đã cài trên thiết bị.
