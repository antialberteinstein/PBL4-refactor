# GLOBAL RULES

Tài liệu này xác định các tiêu chuẩn, nguyên tắc thiết kế và phong cách lập trình bắt buộc mà bạn (AI Agent) phải tuân theo khi tạo hoặc điều chỉnh mã. Mục tiêu là tạo ra mã **dễ đọc, dễ mở rộng, có thể bảo trì** và **hiệu suất cao**.

## 1. 🏛️ Triết lý Cốt lõi

1.  **Readability is King (Khả năng đọc là Vua)**: Mã được đọc nhiều hơn viết. Ưu tiên hàng đầu là sự rõ ràng. Sử dụng tên biến, hàm, và lớp có ý nghĩa rõ ràng.
2.  **KISS (Keep It Simple, Stupid)**: Ưu tiên các giải pháp đơn giản và dễ hiểu hơn là các giải pháp phức tạp.
3.  **DRY (Don't Repeat Yourself)**: Tránh lặp lại logic. Tái cấu trúc thành các hàm hoặc lớp có thể tái sử dụng.
4.  **YAGNI (You Aren't Gonna Need It)**: **KHÔNG** thêm chức năng, lớp, hoặc phương thức trừ khi nó thực sự cần thiết cho yêu cầu hiện tại. Tránh suy đoán về nhu cầu trong tương lai.

## 2. 📐 Nguyên tắc Thiết kế (Design Principles)

Đây là những nguyên tắc quan trọng nhất.

### 2.1. SOLID
Tuân thủ nghiêm ngặt các nguyên tắc SOLID:

* **S - Single Responsibility Principle (Nguyên tắc Đơn trách nhiệm)**: Mỗi lớp hoặc mô-đun chỉ nên chịu trách nhiệm cho một phần chức năng duy nhất.
* **O - Open/Closed Principle (Nguyên tắc Đóng/Mở)**: Mã nguồn nên "mở" cho việc mở rộng nhưng "đóng" cho việc sửa đổi. (Sử dụng kế thừa, interfaces, và composition).
* **L - Liskov Substitution Principle (Nguyên tắc Thay thế Liskov)**: Các đối tượng của lớp con phải có thể thay thế các đối tượng của lớp cha mà không làm hỏng chương trình.
* **I - Interface Segregation Principle (Nguyên tắc Phân tách Interface)**: Không nên ép buộc client phụ thuộc vào các phương thức mà nó không sử dụng. Ưu tiên các interface nhỏ, chuyên biệt.
* **D - Dependency Inversion Principle (Nguyên tắc Đảo ngược Phụ thuộc)**:
    * Các mô-đun cấp cao không nên phụ thuộc vào các mô-đun cấp thấp. Cả hai nên phụ thuộc vào abstractions (ví dụ: interfaces).
    * Abstractions không nên phụ thuộc vào chi tiết. Chi tiết nên phụ thuộc vào abstractions.

### 2.2. Dependency Injection (DI)
**Đây là một yêu cầu bắt buộc.**

* **Luôn ưu tiên Dependency Injection** để quản lý các phụ thuộc.
* **KHÔNG** để một lớp tự khởi tạo các phụ thuộc của nó (ví dụ: `service = new ConcreteService()`).
* Thay vào đó, hãy **tiêm (inject)** các phụ thuộc thông qua constructor (ưu tiên nhất), phương thức setter, hoặc (ít ưu tiên hơn) interface.
* **Mục tiêu:** Giảm sự ràng buộc (decoupling), tăng khả năng kiểm thử (testability) và tính mô-đun hóa.

### 2.3. Composition over Inheritance (Ưu tiên Composition hơn Kế thừa)
* Khi cần mở rộng chức năng, hãy ưu tiên "chứa" một đối tượng của lớp khác (composition) thay vì "là" một đối tượng (kế thừa).
* Kế thừa tạo ra sự ràng buộc chặt chẽ. Composition linh hoạt hơn.

## 3. ✍️ Phong cách Comment và Tài liệu

Đây là một yêu cầu nghiêm ngặt để đảm bảo khả năng đọc và bảo trì.

### 3.1. Khung Comment (Comment Frames)
Sử dụng các "khung" comment rõ ràng để phân chia các phần logic lớn trong một tệp (ví dụ: imports, định nghĩa lớp, hàm tiện ích, logic chính).

**Ví dụ:**
```python
# ============================================================================== #
#                                                               SECTION: IMPORTS                                                                     #
# ============================================================================== #
import numpy as np
import torch

# ============================================================================== #
#                                                      SECTION: MODEL DEFINITION                                                            #
# ============================================================================== #
class MyNeuralNetwork(torch.nn.Module):
    # ...

# ============================================================================== #
#                                                      SECTION: UTILITY FUNCTIONS                                                           #
# ============================================================================== #
def helper_function():
    # ...
```

### 3.2. Comment từng bước (Step-by-Step)
Bên trong các hàm hoặc phương thức phức tạp, **bắt buộc** sử dụng comment được đánh số thứ tự, có phân cấp để giải thích luồng logic.

**Ví dụ:**
```python
def process_data(raw_data):
    ### 1. Khởi tạo
    # Chuẩn bị các biến và cấu hình cần thiết.
    config = get_config()

    ### 2. Tiền xử lý dữ liệu
    ### 2.1. Xử lý giá trị bị thiếu
    # Sử dụng phương pháp điền giá trị trung bình (mean imputation).
    cleaned_data = raw_data.fillna(raw_data.mean())

    ### 2.2. Chuẩn hóa (Standardization)
    # Áp dụng StandardScaler để đưa dữ liệu về phân phối chuẩn.
    scaler = StandardScaler()
    processed_data = scaler.fit_transform(cleaned_data)

    ### 3. Phân chia dữ liệu
    ### 3.1. Tách X (features) và y (target)
    X = processed_data[:, :-1]
    y = processed_data[:, -1]

    ### 3.2. Chia tập Train/Test
    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=config['test_size'])

    ### 4. Trả về kết quả
    return X_train, X_test, y_train, y_test
```

Khi tạo một biến hoặc hằng mới, nếu như tên biến hoặc hằng chưa thể hiện rõ ràng và đầy đủ vai trò của biến, hãy thêm chú thích vào phía sau biến, nếu chú thích quá dài, hãy thêm vào dòng code phía trên của biến.
Với chú thích là cầu nhưng dài, cần phái viết trên nhiều dòng thì cần phải thụt lề các dòng tiếp theo.
Ví dụ:
```python
# Đây là một chú thích siêu dài và rất dài ….. .. . . .  <- giả sử đã quá dài.
#        còn đây là phần tiếp theo của chú thích.
```
Luôn luôn giải thích công thức, nguyên lý trước những xử lý phức tạp.

### 3.3. Docstrings/Javadocs
* Tất cả các lớp (class), phương thức (method), và hàm (function) public phải có docstring/Javadoc đầy đủ.
* Mô tả rõ ràng mục đích, các tham số (`@param`), giá trị trả về (`@return`), và các ngoại lệ có thể ném ra (`@throws`).
* **Python:** Sử dụng **Google Style** hoặc **NumPy Style** docstrings.
* Docstrings cần phải đơn giản dễ hiểu, ưu tiên tiếng Anh.
* Không tạo thêm bất kỳ tài liệu nào trừ phi được yêu cầu.


## 4. 🌐 Hướng dẫn chung cho tất cả ngôn ngữ

### 4.1. Định dạng Tham số (Parameter Formatting)
* **Hàm/Phương thức nhiều tham số:** Phải ngắt dòng. Mỗi tham số nằm trên một dòng riêng biệt.
* **Indentation:** Các tham số bên trong phải được thụt vào một cấp so với dấu ngoặc `(` và `)`.
* **Dấu ngoặc đóng:** Dấu `)` (hoặc `)` đi kèm với `{` trong constructor/method) phải nằm trên một dòng riêng và có cùng mức thụt lề với dòng bắt đầu khai báo hàm.
* **Tham số dài:** Nếu một khai báo tham số (bao gồm kiểu và tên) quá dài, nó có thể được ngắt xuống dòng mới, với mức thụt lề sâu hơn một cấp.

**Ví dụ (Python):**
```python
def my_complex_function(
    param1,  # Chú thích cho param1
    param2: str,
    key_param='default_value',
    long_parameter_name_that_needs_to_be_wrapped: bool = False,
):
    # code
```

**Ví dụ (Java/C++):**
```java
public void myMethod(
    String param1, // Chú thích cho param1
    int param2,
    MyObject longParamThatMightWrap
) {
    // code
}
```

### 4.2. Phong cách Dấu ngoặc nhọn (Brace Style)
* **Bắt buộc** sử dụng phong cách **Java (K&R Style)**.
* Dấu `{` mở đầu nằm cùng dòng với câu lệnh (class, method, if, for,...).
* Dấu `}` đóng nằm trên một dòng riêng, cùng cấp thụt lề với câu lệnh ban đầu.
* **Nghiêm cấm** sử dụng các phong cách khác (như Allman, C# style).

**Ví dụ (Java/C++):**
```java
// === ĐÚNG ===
if (condition) {
    // code
} else {
    // code
}

// === SAI: TUYỆT ĐỐI KHÔNG DÙNG ===
if (condition)
{
    // code
}
```

### 4.3. Khai báo Ngoại lệ (Exception Throws - Java)
* Mệnh đề `throws` **phải** được đặt trên một dòng mới.
* Dòng `throws` phải được thụt lề (thường là 2 cấp) so với dòng khai báo phương thức.
* Nếu có nhiều ngoại lệ, mỗi ngoại lệ nên nằm trên một dòng riêng, được sắp xếp theo thứ tự bảng chữ cái.

**Ví dụ (Java):**
```java
public void processFile(String filePath)
        throws CustomException,
               IOException,
               SQLException {
    // code
}
```

### 4.4. Tránh "Magic Numbers" (Số ma thuật)
* **KHÔNG** sử dụng các giá trị số hoặc chuỗi "kỳ lạ" (magic numbers/strings) trực tiếp trong mã mà không có giải thích.
* Hãy định nghĩa chúng thành các **hằng số (constants)** với tên gọi rõ ràng (ví dụ: `MAX_RETRIES`, `SECONDS_PER_DAY`) ở đầu tệp, lớp, hoặc trong một tệp `constants` riêng.
* **Lý do:** Giúp mã dễ đọc (biết được số `86400` nghĩa là gì) và dễ bảo trì (chỉ cần thay đổi giá trị ở một nơi).

**Ví dụ (Tệ):**
```python
# TỆ
if (status == 2): # 2 là gì?
    # ...
timeout
```

### 4.5. Luôn luôn sử dụng .env để lưu các API keys, access tokens, các dữ liệu cần bảo mật.

### 4.6. Không cần tạo thêm bất kỳ tài liệu nào nếu không được yêu cầu.

## 5. 🐍 Hướng dẫn cho Python (AI/ML/DL)

1.  **PEP 8**: **Tuân thủ nghiêm ngặt PEP 8.** Sử dụng các công cụ như `black` hoặc `ruff` để tự động định dạng mã.
2.  **Type Hinting**: **Bắt buộc** sử dụng type hints của Python (từ module `typing`) cho tất cả các tham số hàm, giá trị trả về, và các biến phức tạp. Điều này cực kỳ quan trọng trong các dự án AI/ML.
3.  **Quản lý Cấu hình (Configuration)**:
    * **KHÔNG** hardcode các tham số (ví dụ: learning rate, batch size, epochs, đường dẫn tệp) trong mã đối với chương trình lớn.
   Đối với các script test thì nên làm đơn giản nhất có thể.
    * Sử dụng tệp cấu hình (ví dụ: `config.yaml`, `config.json`) trừ phi chương trình muốn tuning hyperparemeters.
4.  **Tính tái lập (Reproducibility)**: Luôn thiết lập `random seeds` (cho `numpy`, `random`, `torch`, `tensorflow`) ở đầu các script huấn luyện để đảm bảo kết quả nhất quán.
5.  **Cấu trúc Mô-đun (Modularity)**: Phân tách rõ ràng logic:
    * `data_loader.py`: Chỉ xử lý việc tải và tiền xử lý dữ liệu (Dataset, DataLoader).
    * `model.py`: Chỉ định nghĩa kiến trúc mô hình.
    * `train.py`: Chỉ chứa vòng lặp huấn luyện (training loop).
    * `evaluate.py`: Chỉ chứa logic đánh giá mô hình.
    * `utils.py`: Chứa các hàm tiện ích.

## 6. ☕ Hướng dẫn cho Java

1.  **Naming Conventions**: Tuân thủ quy ước đặt tên chuẩn của Java (ví dụ: `PascalCase` cho Lớp, `camelCase` cho phương thức và biến).
2.  **Interfaces**: Ưu tiên lập trình dựa trên interface. (Ví dụ: `List<String> list = new ArrayList<>();` thay vì `ArrayList<String> list = ...`).
3.  **Dependency Injection**: Nếu sử dụng framework (ví dụ: Spring), hãy tận dụng tối đa DI của nó (@Autowired, @Inject). Nếu không, hãy áp dụng DI thủ công qua constructor.
4.  **Modern Java**: Sử dụng các tính năng của Java 8+ (Streams, Lambdas, `Optional`) để viết mã ngắn gọn và dễ đọc hơn.
5.  **Logging**: Sử dụng framework logging (như SLF4J với Logback/Log4j2) thay vì `System.out.println()`.

## 7. 🇨 Hướng dẫn cho C++

1.  **Modern C++**: Ưu tiên sử dụng các tính năng của C++17/C++20 (ví dụ: smart pointers, `auto`, range-based for loops, `std::optional`).
2.  **RAII (Resource Acquisition Is Initialization)**:
    * Đây là nguyên tắc quan trọng nhất trong C++.
    * Quản lý tài nguyên (bộ nhớ, file handles, network sockets) bằng các đối tượng (ví dụ: `std::unique_ptr`, `std::shared_ptr`, `std::vector`, `std::fstream`).
    * **TRÁNH** gọi `new` và `delete` thủ công.
3.  **Header Files (`.h` / `.hpp`)**:
    * Luôn sử dụng include guards (ví dụ: `#pragma once` hoặc `#ifndef ... #define ... #endif`).
    * Chỉ `#include` những gì thực sự cần thiết trong tệp header.
4.  **`const` Correctness**: Sử dụng `const` một cách triệt để cho các biến và phương thức không thay đổi trạng thái của đối tượng.

## 7. 🧪 Kiểm thử (Testing)
* Không cần kiểm thử trong mọi tình huống trừ phi được yêu cầu một cách trực tiếp, rõ ràng và kiên định, ví dụ như *“hãy tạo cho tôi phần kiểm thử cho toàn bộ chương trình"*.
* Mã nguồn mới phải đi kèm với unit tests (sử dụng `pytest` cho Python, `JUnit` cho Java, `GTest` cho C++).
* Tập trung kiểm thử logic nghiệp vụ và các trường hợp biên (edge cases).
* Trong các dự án AI/ML, viết các bài test để kiểm tra shape (kích thước) của tensors/arrays sau các thao tác.
