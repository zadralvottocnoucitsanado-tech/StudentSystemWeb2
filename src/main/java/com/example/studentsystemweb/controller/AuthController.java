package com.example.studentsystemweb.controller;

import com.example.studentsystemweb.model.User;
import com.example.studentsystemweb.service.AdminService;
import com.example.studentsystemweb.service.AuthService;
import com.example.studentsystemweb.service.LogService;
import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;

@Controller
public class AuthController {

    private final AuthService authService;
    private final AdminService adminService;
    private final LogService logService;

    public AuthController(AuthService authService,
                          AdminService adminService,
                          LogService logService) {
        this.authService = authService;
        this.adminService = adminService;
        this.logService = logService;
    }

    // -----------------------------------
    // LOGIN PAGE
    // -----------------------------------
    @GetMapping({"/", "/login"})
    public String loginPage() {
        return "login";
    }

    // -----------------------------------
    // LOGIN PROCESS
    // -----------------------------------
    @PostMapping("/login")
    public String doLogin(@RequestParam("login") String login,
                          @RequestParam("password") String password,
                          HttpSession session,
                          Model model) {

        User user = authService.login(login, password);

        if (user == null) {
            model.addAttribute("error", "Неверный логин или пароль");
            return "login";
        }

        // Сохранение в сессию
        session.setAttribute("userId", user.getId());
        session.setAttribute("role", user.getRole());
        session.setAttribute("studentId", user.getLinkedStudentId());

        // Последний вход
        session.setAttribute("lastLogin", LocalDateTime.now());

        // Логирование
        logService.add("Пользователь '" + login + "' вошёл в систему");

        // Переход по ролям
        return switch (user.getRole()) {
            case "ADMIN" -> "redirect:/admin/dashboard";
            case "TEACHER" -> "redirect:/teacher/dashboard";
            default -> "redirect:/student/dashboard";
        };
    }

    // -----------------------------------
    // LOGOUT
    // -----------------------------------
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        Object id = session.getAttribute("userId");
        Object role = session.getAttribute("role");

        if (role != null) {
            logService.add("Пользователь (ID=" + id + ", роль=" + role + ") вышел из системы");
        }

        session.invalidate();
        return "redirect:/login";
    }

    // -----------------------------------
    // REGISTER PAGE
    // -----------------------------------
    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("students", adminService.getAllStudents());
        return "register";
    }

    // -----------------------------------
    // REGISTER PROCESS
    // -----------------------------------
    @PostMapping("/register")
    public String doRegister(@RequestParam("login") String login,
                             @RequestParam("password") String password,
                             @RequestParam("role") String role,
                             @RequestParam(value = "linkedStudentId", required = false) Integer linkedStudentId,
                             Model model) {

        try {
            adminService.createUser(login, password, role, linkedStudentId);

            model.addAttribute("success", "Пользователь создан!");
            logService.add("Создан новый аккаунт: '" + login + "' (роль=" + role + ")");

        } catch (RuntimeException ex) {
            model.addAttribute("error", ex.getMessage());
        }

        model.addAttribute("students", adminService.getAllStudents());
        return "register";
    }
}
