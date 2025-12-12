package com.example.studentsystemweb.controller;

import com.example.studentsystemweb.service.AdminService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    private boolean isAdmin(HttpSession session) {
        Object role = session.getAttribute("role");
        return role != null && role.equals("ADMIN");
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/login";

        Map<Integer, Double> avgByStudent = adminService.averageByStudent();

        // Глобальная аналитика
        model.addAttribute("students", adminService.getAllStudents());
        model.addAttribute("avgByStudent", avgByStudent);
        model.addAttribute("top", adminService.getTopStudents(5));
        model.addAttribute("worst", adminService.getWorstStudents(5));
        model.addAttribute("globalAvg", adminService.getGlobalAverage());

        // Средний балл по группам
        model.addAttribute("groupAverages", adminService.groupAverages());

        return "admin-dashboard";
    }

    @GetMapping("/students")
    public String studentsPage(Model model, HttpSession session) {
        if (!isAdmin(session)) return "redirect:/login";

        model.addAttribute("students", adminService.getAllStudents());
        return "admin-students";
    }

    @PostMapping("/students/add")
    public String addStudent(@RequestParam String fullname,
                             @RequestParam String groupName,
                             HttpSession session) {
        if (!isAdmin(session)) return "redirect:/login";

        adminService.addStudent(fullname, groupName);
        return "redirect:/admin/students";
    }

    @PostMapping("/students/delete/{id}")
    public String deleteStudent(@PathVariable int id,
                                HttpSession session) {
        if (!isAdmin(session)) return "redirect:/login";

        adminService.deleteStudent(id);
        return "redirect:/admin/students";
    }
}
