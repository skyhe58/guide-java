package com.example.springcloud.boot.dto;

import jakarta.validation.constraints.*;

/**
 * 用户创建请求 DTO — 演示 @Valid 参数校验注解
 *
 * <p>常用校验注解：
 * <ul>
 *   <li>@NotBlank：不能为空且不能全是空格</li>
 *   <li>@Email：必须是合法的邮箱格式</li>
 *   <li>@Size：字符串长度范围</li>
 *   <li>@Min / @Max：数值范围</li>
 *   <li>@Pattern：正则表达式匹配</li>
 * </ul>
 */
public class CreateUserRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 20, message = "用户名长度必须在 2-20 之间")
    private String name;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @Min(value = 1, message = "年龄最小为 1")
    @Max(value = 150, message = "年龄最大为 150")
    private int age;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    // getter/setter
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
