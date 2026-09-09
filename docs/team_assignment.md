# LEGALLENS TEAM ASSIGNMENT

## 1. Mục đích

File này lưu **phân công feature/flow hiện tại** của team Mobile LegalLens.

```text
AGENTS.md
= luật ổn định về kiến trúc, naming, Git, build/test, Figma, shared files

docs/team_assignment.md
= ai làm gì, branch nào, trạng thái nào, các dependency hiện tại
```

Phân công mới nhất được owner xác nhận luôn được ưu tiên.

---

## 2. Nguyên tắc phân công

- Ưu tiên chia theo **feature / flow lớn**, không chia theo từng màn nhỏ.
- `1 FEATURE / FLOW LỚN = 1 FEATURE BRANCH`.
- Một branch có thể chứa nhiều Activity / Fragment / Dialog / UI state nếu chúng thuộc cùng một flow.
- Không tự sửa feature của thành viên khác.
- Cross-feature chỉ sửa **integration boundary** đã thống nhất.
- Shared files phải tuân theo `AGENTS.md`.
- Không tạo branch riêng chỉ vì một màn có loading / error / success state.
- Khi Figma thay đổi đáng kể, cập nhật file này trước khi code.

---

# 3. PHÂN CÔNG HIỆN TẠI

## KHẢI

```text
KHAI
├── Auth / Verification Flow
│   └── Email Verification / OTP
│
├── Home Flow
│   └── Home / Dashboard
│
├── Contract Management Flow
│   ├── Contract List
│   ├── Search
│   ├── Filter
│   ├── Contract Detail
│   ├── Version History
│   └── Delete Contract Dialog
│
├── Important Dates / Reminder Flow
│   ├── Important Dates
│   └── các màn reminder / deadline liên quan trong Figma
│
├── Notification Flow
│   ├── Notification Fragment
│   ├── Danh sách thông báo
│   ├── Trạng thái đã đọc / chưa đọc
│   └── Điều hướng từ thông báo tới destination phù hợp
│
└── Profile / Account Flow
    └── các màn hồ sơ / tài khoản / cài đặt đã có trong Figma
```

---

## PHƯỚC

```text
PHUOC
├── Onboarding Flow
│   ├── Splash
│   ├── Onboarding Fragment 1
│   └── Onboarding Fragment 2
│
├── Add Contract Flow
│   ├── Thêm hợp đồng
│   ├── Chọn PDF
│   ├── Chọn DOCX
│   ├── Chọn ảnh từ thư viện
│   ├── Điểm vào Camera Scan
│   ├── Upload Progress
│   ├── Upload Error
│   ├── Upload List / Review
│   └── Upload Success
│
├── Camera / Scan Flow
│   ├── Camera Permission
│   ├── Camera Capture
│   └── Quản lý trang quét
│
├── OCR Flow
│   ├── Xử lý OCR
│   ├── Kết quả OCR
│   └── Chỉnh sửa văn bản OCR
│
├── AI Analysis Flow
│   ├── Processing / Analysis state nếu có
│   ├── Analysis Complete
│   └── các màn kết quả phân tích AI trong Figma
│
├── AI Chatbot Flow
│   └── Trò chuyện AI theo hợp đồng
│
└── Contract Compare Flow
    └── So sánh hợp đồng
```

---

# 3A. CHI TIẾT FLOW CỦA KHẢI

## 3A.1 Auth / Verification Flow

Phạm vi hiện đã xác nhận:

```text
Email Verification / OTP
```

Branch:

```text
khai/auth
```

Boundary chính:

```text
Phước Onboarding
→ Khải Email Verification / OTP
→ Khải Home
```

Quy tắc:

- Khải sở hữu màn Verification / OTP.
- Phước chỉ điều hướng tới Auth boundary, không implement lại Auth.
- Nếu Auth flow sau này có thêm Login/Register/Forgot Password nhưng Figma chưa chốt, không tự thêm vào assignment này.

---

## 3A.2 Home Flow

Phạm vi:

```text
Home / Dashboard
```

Các phần chính đã thấy trong Figma:

```text
Lời chào / user summary
thống kê hợp đồng
lối tắt nhanh
hợp đồng gần đây
ngày quan trọng sắp tới
Bottom Navigation
nút + mở Add Contract
```

Branch:

```text
khai/home
```

Boundary:

```text
Khải Home
├── Hợp đồng gần đây → Khải Contract Detail
├── Ngày quan trọng → Khải Important Dates
└── nút + → Phước Add Contract
```

Khải chỉ chịu trách nhiệm điểm điều hướng tới Add Contract, không implement Add Contract.

---

## 3A.3 Contract Management Flow

Phạm vi đầy đủ đã xác nhận:

```text
Contract Management
├── Contract List
├── Search
├── Filter
├── Contract Detail
├── Version History
└── Delete Contract Dialog
```

Branch:

```text
khai/contracts
```

### Contract List

Bao gồm:

```text
danh sách hợp đồng
trạng thái hợp đồng
risk status
search
filter
mở Contract Detail
```

### Search

Search thuộc cùng Contract Management flow.

Không tạo branch riêng:

```text
khai/contract-search
```

### Filter

Filter UI / Bottom Sheet thuộc cùng flow:

```text
khai/contracts
```

Không tạo:

```text
khai/contract-filter
```

### Contract Detail

Contract Detail là màn trung tâm cho thông tin hợp đồng và các action liên quan.

Khải sở hữu:

```text
thông tin hợp đồng
metadata
version/history entry point
rename/delete entry point nếu có
các nút điều hướng tới feature khác
```

Các feature AI/OCR không thuộc Khải.

Boundary:

```text
Khải Contract Detail
├── OCR Văn bản → Phước OCR
├── Phân tích AI → Phước Analysis
├── Trò chuyện AI → Phước Chatbot
└── So sánh HĐ → Phước Compare
```

### Version History

Thuộc:

```text
khai/contracts
```

Không tách branch riêng.

### Delete Contract Dialog

Dialog xác nhận xóa thuộc Contract Management.

Khải sở hữu UI + event của dialog trong feature contracts.

Không để Phước sửa dialog này chỉ vì AI data có liên quan đến hợp đồng.

---

## 3A.4 Important Dates / Reminder Flow

Phạm vi đã thấy trong Figma:

```text
Important Dates
Reminder / deadline information liên quan đến hợp đồng
```

Branch:

```text
khai/important-dates
```

Điểm vào:

```text
Home
→ Ngày quan trọng sắp tới
→ Important Dates
```

Notification Fragment đã được tách thành feature riêng tại:

```text
khai/notifications
```

Không tự tách Important Dates chỉ vì có nhiều state như:

```text
Upcoming
Due Soon
Overdue
```

nếu chúng chỉ là filter/state của cùng một màn.

---

## 3A.5 Notification Flow

Phạm vi đã xác nhận từ Bottom Navigation:

```text
Thông báo
→ Notification Fragment
```

Branch:

```text
khai/notifications
```

Phạm vi hiện tại:

```text
Notification Flow
├── Notification Fragment
├── Danh sách thông báo
├── trạng thái đã đọc / chưa đọc nếu Figma có
└── điều hướng từ notification tới destination tương ứng
```

Điểm vào:

```text
Bottom Navigation
→ Thông báo
→ Notification Fragment
```

Boundary có thể gồm:

```text
Notification
├── thông báo liên quan hợp đồng → Contract Detail
├── thông báo liên quan ngày/hạn → Important Dates
└── thông báo hệ thống khác → destination đã được Figma/requirements xác nhận
```

Quy tắc:

- Notification là feature riêng, không gộp vào `Important Dates`.
- `Important Dates` quản lý ngày/hạn/reminder.
- `Notification` quản lý danh sách thông báo và hành vi khi người dùng mở thông báo.
- Không tự tạo notification type hoặc destination chưa được Figma/requirements xác nhận.
- Không tạo branch riêng cho read/unread state nếu chỉ là state của Notification Fragment.

---

## 3A.6 Profile / Account Flow

Phạm vi:

```text
Profile / Account
Settings liên quan trực tiếp đến profile nếu đã có trong Figma
```

Branch:

```text
khai/profile
```

Điểm vào:

```text
Bottom Navigation
→ Hồ sơ
→ Profile / Account
```

Không tự thêm các chức năng chưa được Figma/requirements xác nhận.

Ví dụ không tự thêm:

```text
đổi mật khẩu
đổi email
xóa tài khoản
đăng xuất nhiều thiết bị
```

nếu chưa có trong scope được duyệt.

---

## 3A.7 Boundary giữa các flow của Khải

```text
Auth
→ Home
→ Contract List
→ Contract Detail
```

và:

```text
Home
→ Important Dates
```

và:

```text
Bottom Navigation
├── Thông báo → Notification Fragment
└── Hồ sơ → Profile
```

Các flow có thể dùng navigation boundary chung nhưng không được trộn ViewModel/business logic chỉ vì cùng owner.

---

## 3A.8 Branch của Khải

```text
khai/auth
khai/home
khai/contracts
khai/important-dates
khai/notifications
khai/profile
```

Không tạo branch riêng cho:

```text
khai/search
khai/filter
khai/version-history
khai/delete-dialog
```

vì các màn này thuộc Contract Management.

---

## 3A.9 Thứ tự triển khai gợi ý cho Khải

Theo dependency UI hiện tại:

```text
Auth / Verification
        ↓
Home
        ↓
Contract Management
        ↓
Important Dates

Notification và Profile có thể làm song song khi Bottom Navigation/shared resources đã ổn.
```

Nếu Home cần destination chưa tồn tại:

```text
dùng integration boundary nhỏ nhất đã thống nhất
HOẶC
báo dependency
```

Không tự implement destination feature của Phước.

# 4. BRANCH THEO FEATURE / FLOW

## Khải

```text
khai/auth
└── Email Verification / OTP

khai/home
└── Home / Dashboard

khai/contracts
├── Contract List
├── Search
├── Filter
├── Contract Detail
├── Version History
└── Delete Contract Dialog

khai/important-dates
└── Important Dates / Reminder / Deadline liên quan

khai/notifications
├── Notification Fragment
├── Danh sách thông báo
├── Read / Unread state nếu có
└── Navigation từ notification

khai/profile
└── Profile / Account / Settings
```

---

## Phước

```text
phuoc/onboarding
├── Splash
├── Onboarding Fragment 1
└── Onboarding Fragment 2

phuoc/add-contract
├── Thêm hợp đồng
├── PDF / DOCX / Gallery
├── Upload Progress
├── Upload Error
├── Upload List / Review
└── Upload Success

phuoc/camera-scan
├── Camera Permission
├── Camera Capture
└── Quản lý trang quét

phuoc/ocr
├── Xử lý OCR
├── Kết quả OCR
└── Chỉnh sửa văn bản OCR

phuoc/analysis
└── AI Analysis / Analysis Result

phuoc/chatbot
└── Trò chuyện AI

phuoc/compare
└── So sánh hợp đồng
```

Không tạo:

```text
phuoc/ocr-processing
phuoc/ocr-result
phuoc/ocr-edit

phuoc/upload-progress
phuoc/upload-error
phuoc/upload-success
```

vì đó chỉ là các màn/state nhỏ trong cùng một flow.

---

# 5. FLOW TOÀN APP LIÊN QUAN ĐẾN PHÂN CÔNG

## 5.1 Onboarding → Auth → Home

```text
Phước
Splash
→ Onboarding

        ↓

Khải
Email Verification / OTP
→ Home
```

Boundary:

```text
Phước Onboarding
→ navigation
→ Khải Auth
```

Phước không implement lại Auth.

Khải không sửa Onboarding nếu chưa được giao.

---

## 5.2 Home → Add Contract

```text
Khải Home
   ↓ nút +
Phước Add Contract
```

Khải chịu trách nhiệm điểm điều hướng từ Home.

Phước chịu trách nhiệm toàn bộ Add Contract sau khi navigation bắt đầu.

---

## 5.3 Add Contract → Upload hoặc Camera

```text
                    ┌→ PDF
                    ├→ DOCX
Add Contract ───────┼→ Gallery
                    └→ Camera Scan
```

PDF / DOCX / Gallery thuộc:

```text
phuoc/add-contract
```

Camera riêng thuộc:

```text
phuoc/camera-scan
```

Không gộp implementation Camera vào Add Contract chỉ vì Camera được mở từ Add Contract.

---

## 5.4 Camera Flow

```text
Camera Permission
→ Camera Capture
→ Quản lý trang quét
→ Hoàn tất
→ OCR
```

Branch:

```text
phuoc/camera-scan
```

Camera flow kết thúc tại boundary gửi tài liệu/trang scan sang OCR.

---

## 5.5 OCR Flow

Luồng đã xác nhận từ Figma:

```text
Tài liệu đã upload / scan
→ Xử lý OCR
→ Kết quả OCR
→ Chỉnh sửa văn bản OCR (nếu người dùng chọn Chỉnh sửa)
→ Lưu
→ Kết quả OCR
```

### Xử lý OCR

Các trạng thái hiển thị gồm:

```text
Tải lên tài liệu
→ Xử lý phân tích hình ảnh
→ OCR văn bản & Số hóa
→ Sẵn sàng xem lại kết quả
```

### Kết quả OCR

Có:

```text
độ tin cậy OCR
nội dung OCR
Chỉnh sửa
Phân tích AI
```

### Chỉnh sửa văn bản OCR

Có:

```text
chỉnh sửa text
đánh dấu vùng độ tin cậy thấp
Khôi phục bản gốc
Lưu
```

Toàn bộ dùng:

```text
phuoc/ocr
```

---

## 5.6 OCR → AI Analysis

```text
Kết quả OCR
   ↓ Phân tích AI
AI Analysis
```

Boundary:

```text
phuoc/ocr
→ dữ liệu OCR đã xác nhận
→ phuoc/analysis
```

Không đưa logic AI Analysis vào OCR ViewModel.

---

## 5.7 Contract Detail → AI Features

Màn Contract Detail của Khải là điểm vào các feature AI của Phước:

```text
Khải Contract Detail
├── OCR Văn bản ─────→ Phước OCR
├── Phân tích AI ────→ Phước AI Analysis
├── Trò chuyện AI ───→ Phước Chatbot
└── So sánh HĐ ──────→ Phước Contract Compare
```

Khải chỉ chịu trách nhiệm source screen và navigation boundary.

Phước sở hữu destination feature.

Không copy/reimplement destination trong Contract Detail.

---

# 6. AI FEATURE BOUNDARY

Ba feature sau là độc lập:

```text
AI Analysis
≠
AI Chatbot
≠
Contract Compare
```

## AI Analysis

Mục tiêu:

```text
phân tích toàn bộ hợp đồng
→ đưa ra kết quả / mức độ / cảnh báo / giải thích
```

Branch:

```text
phuoc/analysis
```

## AI Chatbot

Mục tiêu:

```text
người dùng hỏi
→ chatbot trả lời theo ngữ cảnh hợp đồng hiện tại
```

Branch:

```text
phuoc/chatbot
```

Không nhét Chatbot vào Analysis ViewModel.

## Contract Compare

Mục tiêu:

```text
chọn / nhận hai hợp đồng
→ so sánh nội dung / khác biệt theo UI Figma
```

Branch:

```text
phuoc/compare
```

Không nhét Compare vào Contract Management chỉ vì dữ liệu đầu vào là hợp đồng.

---

# 7. CONTRACT MANAGEMENT BOUNDARY

Khải sở hữu:

```text
Contract List
Search
Filter
Contract Detail
Version History
Delete Contract Dialog
```

Branch:

```text
khai/contracts
```

Khải không implement:

```text
OCR
AI Analysis
AI Chatbot
Compare
```

Phước không tự sửa:

```text
Contract List
Contract Detail
Version History
Delete Dialog
```

chỉ để nối feature AI.

---

# 8. IMPORTANT DATES / NOTIFICATION

Khải sở hữu các màn liên quan:

```text
Important Dates
Reminder
Notification liên quan đến ngày/hạn hợp đồng
```

Nếu Figma sau này cho thấy Notification là một feature hoàn toàn độc lập và lớn, có thể tách branch mới sau khi owner xác nhận.

Hiện tại ưu tiên:

```text
khai/important-dates
```

---

# 9. NOTIFICATION FLOW

Khải sở hữu màn **Thông báo** trong Bottom Navigation.

Branch:

```text
khai/notifications
```

Flow:

```text
Bottom Navigation
→ Notification Fragment
→ chọn notification
→ điều hướng tới destination phù hợp
```

Phạm vi:

```text
danh sách thông báo
read / unread state nếu có
empty/loading/error state nếu Figma có
mở notification
điều hướng sang Contract Detail / Important Dates / destination đã xác nhận
```

Notification không đồng nghĩa với Important Dates:

```text
Important Dates
= ngày/hạn/reminder

Notification
= danh sách thông báo + hành vi mở thông báo
```

Không tự thêm loại notification hoặc destination chưa được xác nhận.

---

# 10. PROFILE / ACCOUNT

Các màn hồ sơ / tài khoản / settings trong Figma thuộc Khải.

Branch:

```text
khai/profile
```

Không tự thêm chức năng ngoài những gì Figma/requirements đã xác nhận.

---

# 11. SHARED UI / SHARED FILES

Các phần dễ conflict:

```text
Bottom Navigation
AndroidManifest.xml
strings.xml
colors.xml
dimens.xml
themes.xml
navigation graph
Gradle files
presentation/common/*
```

Quy tắc:

- chỉ sửa phần cần thiết
- không format lại toàn file
- không rename resource không liên quan
- không xóa entry của người khác
- review diff trước commit
- resource riêng của feature ưu tiên prefix theo feature

Chi tiết xem `AGENTS.md`.

---

# 12. TRẠNG THÁI HIỆN TẠI

Dùng:

```text
TODO
IN_PROGRESS
READY_FOR_OWNER_TEST
OWNER_APPROVED
COMMITTED
PUSHED
MERGED
BLOCKED
```

| Người | Feature / Flow             | Status      | Branch                 | Ghi chú                                                     |
| ----- | -------------------------- | ----------- | ---------------------- | ----------------------------------------------------------- |
| Khải  | Auth / Verification        | TODO        | `khai/auth`            | Email Verification / OTP                                    |
| Khải  | Home                       | TODO        | `khai/home`            | Home / Dashboard                                            |
| Khải  | Contract Management        | TODO        | `khai/contracts`       | List/Search/Filter/Detail/History/Delete                    |
| Khải  | Important Dates / Reminder | TODO        | `khai/important-dates` | Important Dates + reminder/deadline liên quan               |
| Khải  | Notification Flow          | TODO        | `khai/notifications`   | Notification Fragment + danh sách thông báo + navigation    |
| Khải  | Profile / Account          | TODO        | `khai/profile`         | Các màn profile/settings trong Figma                        |
| Phước | Onboarding                 | MERGED      | `phuoc/onboarding`     | Đã merge vào `phuoc_develop` theo trạng thái owner cung cấp |
| Phước | Add Contract               | IN_PROGRESS | `phuoc/add-contract`   | Feature đang có code, chưa hoàn tất Git flow                |
| Phước | Camera / Scan              | IN_PROGRESS | `phuoc/camera-scan`    | Camera flow đang triển khai                                 |
| Phước | OCR                        | TODO        | `phuoc/ocr`            | Processing + Result + Edit OCR                              |
| Phước | AI Analysis                | TODO        | `phuoc/analysis`       | Analysis processing/result/complete theo Figma              |
| Phước | AI Chatbot                 | TODO        | `phuoc/chatbot`        | Trò chuyện AI theo hợp đồng                                 |
| Phước | Contract Compare           | TODO        | `phuoc/compare`        | So sánh hợp đồng                                            |

Không tự đổi status nếu chưa có bằng chứng từ repo hoặc owner.

---

# 13. THỨ TỰ TRIỂN KHAI GỢI Ý CHO PHƯỚC

Theo dependency hiện tại:

```text
Onboarding          ✅ đã merge
        ↓
Add Contract        IN_PROGRESS
        ↓
Camera / Scan       IN_PROGRESS
        ↓
OCR
        ↓
AI Analysis
        ↓
Chatbot / Compare
```

`Chatbot` và `Compare` không bắt buộc phải chờ nhau.

Có thể phát triển độc lập sau khi boundary với Contract Detail / dữ liệu hợp đồng đã rõ.

---

# 14. KHI CÓ FIGMA MỚI

```text
XEM MÀN
→ XÁC ĐỊNH MÀN CŨ / MÀN MỚI
→ GROUP THEO FEATURE
→ XÁC ĐỊNH DEPENDENCY
→ XÁC ĐỊNH OWNER
→ CHỌN / TẠO FEATURE BRANCH
→ CẬP NHẬT team_assignment.md
→ CODE
```

Không chia việc chỉ bằng số lượng màn.

Ưu tiên:

```text
cùng flow
cùng nghiệp vụ
ít cross-dependency
ít conflict shared files
workload hợp lý
```

---

# 15. QUY TẮC CUỐI

```text
AGENTS.md
= luật ổn định

docs/team_assignment.md
= ownership + branch + status hiện tại
```

Nếu mâu thuẫn về:

```text
architecture
Git
build/test
Figma rules
shared-file rules
```

→ `AGENTS.md` ưu tiên.

Nếu mâu thuẫn về:

```text
feature owner
branch hiện tại
status hiện tại
```

→ phân công mới nhất được owner xác nhận ưu tiên.

Nếu một màn Figma chưa đủ rõ để xác định feature:

```text
DỪNG
→ BÁO
→ KHÔNG TỰ GÁN OWNER / TỰ TÁCH BRANCH
```
