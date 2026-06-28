package org.example.datn_sd69.modules.category.request;

public class CategoryRequest {
    private String name;
    private Integer status;

    // Nếu dự án của bạn dùng Lombok thì chỉ cần gõ @Data ở trên đầu class nhé.
    // Nếu không dùng Lombok thì dùng Getter/Setter thuần dưới đây:
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
