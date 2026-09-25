package org.example.eledangercheck.controller;

import jakarta.validation.Valid;
import org.example.eledangercheck.dto.LoginRequest;
import org.example.eledangercheck.dto.LoginResponse;
import org.example.eledangercheck.dto.RegisterRequest;
import org.example.eledangercheck.entity.User;
import org.example.eledangercheck.security.JwtTokenProvider;
import org.example.eledangercheck.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(UserService userService, JwtTokenProvider jwtTokenProvider) {
        this.userService = userService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        // 验证两次密码是否一致
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "两次输入的密码不一致");
            return ResponseEntity.badRequest().body(response);
        }
        
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        user.setEmail(request.getEmail());
        user = userService.register(user);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "注册成功");
        response.put("username", user.getUsername());
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = userService.login(request.getUsername(), request.getPassword());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "up");
        response.put("service", "ELeDangerCheck");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> getAllUsers() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", userService.getAllUsers());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User userDetails) {
        return ResponseEntity.ok(userService.updateUser(id, userDetails));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/user-info")
    public ResponseEntity<User> getUserInfo(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        String username = extractUsernameFromToken(authHeader);
        if (username == null) {
            return ResponseEntity.status(401).build();
        }
        User user = userService.getUserByUsername(username);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/update-profile")
    public ResponseEntity<User> updateProfile(@RequestHeader(value = "Authorization", required = false) String authHeader, @RequestBody Map<String, String> request) {
        String username = extractUsernameFromToken(authHeader);
        if (username == null) {
            return ResponseEntity.status(401).build();
        }
        User user = userService.getUserByUsername(username);
        
        User updated = userService.updateProfile(
            user.getId(),
            request.get("username"),
            request.get("realName"),
            request.get("phone"),
            request.get("email"),
            request.get("department"),
            request.get("role")
        );
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/change-password")
    public ResponseEntity<Map<String, Object>> changePassword(@RequestHeader(value = "Authorization", required = false) String authHeader, @RequestBody Map<String, String> request) {
        String username = extractUsernameFromToken(authHeader);
        if (username == null) {
            return ResponseEntity.status(401).build();
        }
        User user = userService.getUserByUsername(username);
        
        userService.changePassword(
            user.getId(),
            request.get("currentPassword"),
            request.get("newPassword")
        );
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "密码修改成功");
        return ResponseEntity.ok(response);
    }

    private String extractUsernameFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.substring(7);
        if (!jwtTokenProvider.validateToken(token)) {
            return null;
        }
        return jwtTokenProvider.getUsernameFromToken(token);
    }

    @GetMapping("/operation-records")
    public ResponseEntity<List<Map<String, Object>>> getOperationRecords() {
        List<Map<String, Object>> records = List.of();
        return ResponseEntity.ok(records);
    }
}