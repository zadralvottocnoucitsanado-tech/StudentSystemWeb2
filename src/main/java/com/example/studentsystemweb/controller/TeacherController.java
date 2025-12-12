package com.example.studentsystemweb.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.studentsystemweb.model.Student;
import com.example.studentsystemweb.service.GradingService;
import com.example.studentsystemweb.service.TeacherService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/teacher")
public class TeacherController {

    private final TeacherService teacherService;
    private final GradingService gradingService;

    public TeacherController(TeacherService teacherService,
                             GradingService gradingService) {
        this.teacherService = teacherService;
        this.gradingService = gradingService;
    }

    private boolean isTeacher(HttpSession session) {
        Object role = session.getAttribute("role");
        return role != null && (role.equals("TEACHER") || role.equals("ADMIN"));
    }

    // ======================
    // DASHBOARD
    // ======================
    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(required = false) String group,
                            Model model,
                            HttpSession session) {

        if (!isTeacher(session)) return "redirect:/login";

        List<String> groups = teacherService.getAllGroups();
        model.addAttribute("groups", groups);
        model.addAttribute("selectedGroup", group == null ? "" : group);

        List<Student> students =
                (group == null || group.isBlank())
                        ? teacherService.getStudents()
                        : teacherService.getStudentsByGroup(group);

        model.addAttribute("students", students);

        Map<Integer, Double> allAvgs = teacherService.getFinalAveragesForAllStudents();
        Set<Integer> visibleIds = students.stream().map(Student::getId).collect(Collectors.toSet());

        Map<Integer, Double> avgByStudent = allAvgs.entrySet().stream()
                .filter(e -> e.getValue() != null && visibleIds.contains(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        model.addAttribute("avgByStudent", avgByStudent);

        model.addAttribute("overallAvg", teacherService.getOverallAverage());
        model.addAttribute("low", teacherService.getLowStudentsList());
        model.addAttribute("absentLeaders", teacherService.getAbsentLeaders());

        return "teacher-dashboard";
    }


    // ======================
    // JOURNAL PAGE
    // ======================
    @GetMapping("/students/{id}/journal")
    public String journalPage(@PathVariable int id,
                              @RequestParam(required = false) String subject,
                              Model model,
                              HttpSession session) {

        if (!isTeacher(session)) return "redirect:/login";

        Student student = teacherService.getStudent(id);
        List<String> subjects = teacherService.getSubjectsForStudent(id);

        if (subjects.isEmpty()) {
            model.addAttribute("error", "У студента нет предметов");
            return "teacher-journal";
        }

        if (subject == null || !subjects.contains(subject)) {
            subject = subjects.get(0);
        }

        Map<String, Double> table = gradingService.getJournalTable(id, subject);

        model.addAttribute("student", student);
        model.addAttribute("subjects", subjects);
        model.addAttribute("subject", subject);
        model.addAttribute("table", table);

        return "teacher-journal";
    }

    // ======================
    // SAVE JOURNAL
    // ======================
    @PostMapping("/students/{id}/journal/save")
    public String saveJournal(@PathVariable int id,
                              @RequestParam String subject,
                              @RequestParam Map<String, String> form,
                              HttpSession session) {

        if (!isTeacher(session)) return "redirect:/login";

        String[] types = {"lecture", "sro", "lab"};

        for (int w = 1; w <= 15; w++) {
            for (String t : types) {
                String key = "week" + w + "_" + t;
                process(id, subject, key, form.get(key), form.get(key + "_flag"));
            }
        }

        for (String comp : new String[]{"rk1", "rk2", "exam"}) {
            process(id, subject, comp, form.get(comp), form.get(comp + "_flag"));
        }

        gradingService.recalcTotals(id, subject);

        try {
            var field = teacherService.getClass().getDeclaredField("cache");
            field.setAccessible(true);
            field.set(teacherService, null);
        } catch (Exception ignored) {}

        return "redirect:/teacher/students/" + id + "/journal?subject=" +
                URLEncoder.encode(subject, StandardCharsets.UTF_8);
    }

    private void process(int id, String subject, String workType,
                         String raw, String flag) {

        if (flag == null) flag = "0";
        flag = flag.trim();

        double flagVal;
        try {
            flagVal = Double.parseDouble(flag);
        } catch (Exception e) {
            flagVal = 0.0;
        }

        gradingService.setFlagValue(id, subject, workType, flagVal);

        if (flagVal == 1.0) { gradingService.setComponent(id, subject, workType, -1.0); return; }
        if (flagVal == 2.0) { gradingService.setComponent(id, subject, workType, -2.0); return; }

        try {
            double v = (raw == null || raw.isBlank())
                    ? -1.0
                    : Double.parseDouble(raw.replace(",", "."));
            gradingService.setComponent(id, subject, workType, v);
        } catch (Exception ignore) {
            gradingService.setComponent(id, subject, workType, -1.0);
        }
    }


    // ======================
    // ANALYTICS
    // ======================
    @GetMapping("/analytics")
    public String analytics(HttpSession session, Model model) {

        if (!isTeacher(session)) return "redirect:/login";

        boolean hasFinals = teacherService.hasFinals();
        model.addAttribute("hasFinals", hasFinals);

        if (!hasFinals) return "teacher-analytics";

        // общие данные
        model.addAttribute("groupAvg", teacherService.getOverallAverage());
        model.addAttribute("studentsWithFinals", teacherService.countStudentsWithFinals());
        model.addAttribute("subjectsCount", teacherService.getSubjectsCount());

        // pie chart
        model.addAttribute("pieData", teacherService.getPieData());
        model.addAttribute("pieLabels", teacherService.getPieLabels());

        // subject chart
        model.addAttribute("subjectsLabels", teacherService.getSubjectsLabels());
        model.addAttribute("subjectsValues", teacherService.getSubjectsValues());

        // NEW — group charts
        model.addAttribute("groupLabels", teacherService.getGroupLabels());
        model.addAttribute("groupValues", teacherService.getGroupValues());

        model.addAttribute("absByGroupLabels", teacherService.getAbsByGroupLabels());
        model.addAttribute("absByGroupValues", teacherService.getAbsByGroupValues());

        // NEW — best group
        model.addAttribute("bestGroup", teacherService.getBestGroupLabel());
        model.addAttribute("bestGroupScore", teacherService.getBestGroupValue());

        // histogram (distribution)
        model.addAttribute("histLabels", teacherService.getHistogramLabels());
        model.addAttribute("histValues", teacherService.getHistogramValues());

        // top / low students
        model.addAttribute("topStudents", teacherService.getTopStudents());
        model.addAttribute("lowStudents", teacherService.getLowStudentsList());

        // absents
        model.addAttribute("absentLeaders", teacherService.getAbsentLeaders());

        return "teacher-analytics";
    }

    // ======================
    // REPORT
    // ======================
    @GetMapping("/students/{id}/report")
    public String studentReport(@PathVariable int id,
                                HttpSession session,
                                Model model) {

        if (!isTeacher(session)) return "redirect:/login";

        Student student = teacherService.getStudent(id);
        Map<String, Double> finals = teacherService.getFinalGradesForStudent(id);

        model.addAttribute("student", student);
        model.addAttribute("finals", finals);

        return "teacher-student-report";
    }

    // ======================
    // SETTINGS
    // ======================
    @GetMapping("/settings")
    public String settings(HttpSession session) {
        if (!isTeacher(session)) return "redirect:/login";
        return "teacher-settings";
    }
}
