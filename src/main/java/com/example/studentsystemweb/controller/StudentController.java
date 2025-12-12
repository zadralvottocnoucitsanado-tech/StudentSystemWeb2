package com.example.studentsystemweb.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.studentsystemweb.model.Attendance;
import com.example.studentsystemweb.model.Grade;
import com.example.studentsystemweb.model.ProgressRecord;
import com.example.studentsystemweb.model.Student;
import com.example.studentsystemweb.service.GradingService;
import com.example.studentsystemweb.service.StudentService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/student")
public class StudentController {

    private final StudentService studentService;
    private final GradingService gradingService;

    public StudentController(StudentService studentService,
                             GradingService gradingService) {
        this.studentService = studentService;
        this.gradingService = gradingService;
    }

    private Integer getStudentId(HttpSession session) {
        Object id = session.getAttribute("studentId");

        if (id instanceof Integer i) return i;
        if (id instanceof Long l) return l.intValue();

        return null;
    }

    private String formatDateTime(LocalDateTime dt) {
        if (dt == null) dt = LocalDateTime.now();

        LocalDate today = LocalDate.now();
        LocalDate date = dt.toLocalDate();

        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");
        DateTimeFormatter fullFmt =
                DateTimeFormatter.ofPattern("dd MMMM yyyy, HH:mm", new Locale("ru"));

        if (date.equals(today))
            return "Сегодня, " + dt.format(timeFmt);

        if (date.equals(today.minusDays(1)))
            return "Вчера, " + dt.format(timeFmt);

        return dt.format(fullFmt);
    }

    // ==========================================
    // DASHBOARD
    // ==========================================
    @GetMapping("/dashboard")
    public String dashboard(Model model, HttpSession session) {

        if (!"STUDENT".equals(session.getAttribute("role")))
            return "redirect:/login";

        Integer studentId = getStudentId(session);
        if (studentId == null)
            return "redirect:/login";

        Student student = studentService.getStudent(studentId);

        List<Grade> grades = studentService.getGrades(studentId);
        List<ProgressRecord> progress = studentService.getProgress(studentId);
        List<Attendance> attendance = studentService.getAttendance(studentId);

        Map<String, Double> subjectAverages = studentService.getSubjectAverages(studentId);

        Double avgRaw = studentService.getAverage(studentId);

        String avgFormatted = avgRaw == null
                ? "0.00"
                : String.format(Locale.US, "%.2f", avgRaw);

        int present = studentService.countPresent(studentId);
        int absent = studentService.countAbsent(studentId);

        int attendancePercent =
                (present + absent == 0) ? 0 :
                        (int) Math.round(present * 100.0 / (present + absent));

        int gradesCount = grades.size();
        int progressCount = progress.size();

        LocalDateTime lastLogin = (LocalDateTime)
                session.getAttribute("lastLogin");

        if (lastLogin == null) lastLogin = LocalDateTime.now();

        session.setAttribute("lastLogin", lastLogin);
        String formattedLogin = formatDateTime(lastLogin);

        // -------- MODEL --------
        model.addAttribute("student", student);

        model.addAttribute("avg", avgFormatted);
        model.addAttribute("gradesCount", gradesCount);
        model.addAttribute("progressCount", progressCount);

        model.addAttribute("subjectAverages", subjectAverages);
        model.addAttribute("subjectLabels", subjectAverages.keySet());
        model.addAttribute("subjectValues", subjectAverages.values());

        model.addAttribute("progress", progress);
        model.addAttribute("grades", grades);

        model.addAttribute("present", present);
        model.addAttribute("absent", absent);
        model.addAttribute("attendancePercent", attendancePercent);

        model.addAttribute("lastLogin", formattedLogin);

        return "student-dashboard";
    }


    // ==========================================
    // JOURNAL
    // ==========================================
    @GetMapping("/journal")
    public String studentJournal(Model model,
                                 HttpSession session,
                                 @RequestParam(required = false) String subject) {

        if (!"STUDENT".equals(session.getAttribute("role")))
            return "redirect:/login";

        Integer studentId = getStudentId(session);
        if (studentId == null)
            return "redirect:/login";

        Student student = studentService.getStudent(studentId);

        List<String> subjects = studentService.getJournalSubjects(studentId);
        if (subjects == null || subjects.isEmpty()) {
            model.addAttribute("student", student);
            model.addAttribute("subjects", List.of());
            model.addAttribute("subject", null);
            model.addAttribute("table", Map.of());
            model.addAttribute("avg", "0.00");
            return "student-journal";
        }

        if (subject == null || !subjects.contains(subject))
            subject = subjects.get(0);

        Map<String, Double> table = gradingService.getJournalTable(studentId, subject);
        if (table == null) table = new LinkedHashMap<>();

        Double avgRaw = studentService.getAverage(studentId);
        String avgFormatted = avgRaw == null
                ? "0.00"
                : String.format(Locale.US, "%.2f", avgRaw);

        model.addAttribute("student", student);
        model.addAttribute("subjects", subjects);
        model.addAttribute("subject", subject);
        model.addAttribute("table", table);
        model.addAttribute("avg", avgFormatted);

        return "student-journal";
    }

    // ==========================================
    // REPORT
    // ==========================================
    @GetMapping("/report")
    public String report(Model model, HttpSession session) {

        if (!"STUDENT".equals(session.getAttribute("role")))
            return "redirect:/login";

        Integer studentId = getStudentId(session);
        if (studentId == null)
            return "redirect:/login";

        Student stu = studentService.getStudent(studentId);

        Double avgRaw = studentService.getAverage(studentId);
        String avgFormatted = avgRaw == null
                ? "0.00"
                : String.format(Locale.US, "%.2f", avgRaw);

        Map<String, Double> subjectAverages = studentService.getSubjectAverages(studentId);
        List<Grade> grades = studentService.getGrades(studentId);
        List<Attendance> att = studentService.getAttendance(studentId);

        int present = studentService.countPresent(studentId);
        int absent = studentService.countAbsent(studentId);

        model.addAttribute("student", stu);
        model.addAttribute("avg", avgFormatted);
        model.addAttribute("subjectAverages", subjectAverages);
        model.addAttribute("grades", grades);
        model.addAttribute("attendance", att);
        model.addAttribute("present", present);
        model.addAttribute("absent", absent);

        return "student-report";
    }
}
