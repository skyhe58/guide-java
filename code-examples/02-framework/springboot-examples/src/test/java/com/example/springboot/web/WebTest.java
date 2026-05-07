package com.example.springboot.web;

import com.example.springboot.SpringBootApp;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Web Controller 接口测试
 *
 * <p>使用 MockMvc 测试 RESTful API，验证以下知识点：</p>
 * <ul>
 *   <li>GET 请求正常返回</li>
 *   <li>POST 请求参数校验</li>
 *   <li>全局异常处理</li>
 *   <li>统一返回格式</li>
 * </ul>
 */
@SpringBootTest(classes = SpringBootApp.class)
@AutoConfigureMockMvc
class WebTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/users/hello 应该返回问候语")
    void helloShouldReturnGreeting() throws Exception {
        mockMvc.perform(get("/api/users/hello")
                        .param("name", "Spring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("Hello, Spring!"));
    }

    @Test
    @DisplayName("GET /api/users/hello 默认参数应该返回 Hello, World!")
    void helloDefaultShouldReturnWorld() throws Exception {
        mockMvc.perform(get("/api/users/hello"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("Hello, World!"));
    }

    @Test
    @DisplayName("GET /api/users/{id} 应该返回用户信息")
    void getUserShouldReturnUser() throws Exception {
        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.username").value("user-1"));
    }

    @Test
    @DisplayName("POST /api/users 合法参数应该创建用户")
    void createUserWithValidParamsShouldSucceed() throws Exception {
        String requestBody = """
                {
                    "username": "testuser",
                    "email": "test@example.com",
                    "age": 25
                }
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("testuser"))
                .andExpect(jsonPath("$.data.email").value("test@example.com"));
    }

    @Test
    @DisplayName("POST /api/users 空用户名应该返回校验错误")
    void createUserWithBlankUsernameShouldFail() throws Exception {
        String requestBody = """
                {
                    "username": "",
                    "email": "test@example.com",
                    "age": 25
                }
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("POST /api/users 无效邮箱应该返回校验错误")
    void createUserWithInvalidEmailShouldFail() throws Exception {
        String requestBody = """
                {
                    "username": "testuser",
                    "email": "invalid-email",
                    "age": 25
                }
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("POST /api/users 年龄超出范围应该返回校验错误")
    void createUserWithInvalidAgeShouldFail() throws Exception {
        String requestBody = """
                {
                    "username": "testuser",
                    "email": "test@example.com",
                    "age": 200
                }
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }
}
