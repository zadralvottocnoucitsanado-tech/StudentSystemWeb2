package com.example.studentsystemweb.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.studentsystemweb.model.Grade;
import com.example.studentsystemweb.model.Student;
import com.example.studentsystemweb.repository.GradeRepository;
import com.example.studentsystemweb.repository.StudentRepository;

@Service
public class TeacherService {

    private final StudentRepository studentRepository;
    private final GradeRepository gradeRepository;

    // cache: studentId -> subject -> (workType -> grade)
    private Map<Integer, Map<String, Map<String, Double>>> cache = null;

    public TeacherService(StudentRepository studentRepository,
                          GradeRepository gradeRepository) {
        this.studentRepository = studentRepository;
        this.gradeRepository = gradeRepository;
    }

    // Build cache lazily
    private void buildCacheIfNeeded() {
        if (cache != null) return;

        cache = new HashMap<>();
        List<Grade> all = gradeRepository.findAll();

        for (Grade g : all) {
            // defensive nulls (subject/workType may be null in DB)
            String subject = g.getSubject() == null ? "" : g.getSubject();
            String workType = g.getWorkType() == null ? "" : g.getWorkType();

            cache
                .computeIfAbsent(g.getStudentId(), x -> new HashMap<>())
                .computeIfAbsent(subject, x -> new HashMap<>())
                .put(workType, g.getGrade());
        }
    }

    // ================= STUDENTS =================

    public List<Student> getStudents() {
        return studentRepository.findAll();
    }

    public Student getStudent(int id) {
        return studentRepository.findById(id);
    }

    public List<String> getAllGroups() {
        // prefer repository method if exists
        try {
            return studentRepository.findAllGroups();
        } catch (Exception e) {
            // fallback: compute from students
            return studentRepository.findAll().stream()
                    .map(Student::getGroupName)
                    .filter(g -> g != null && !g.isBlank())
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());
        }
    }

    public List<Student> getStudentsByGroup(String group) {
        try {
            return studentRepository.findByGroup(group);
        } catch (Exception e) {
            // fallback: filter in-memory
            return studentRepository.findAll().stream()
                    .filter(s -> group == null ? (s.getGroupName() == null) : group.equals(s.getGroupName()))
                    .collect(Collectors.toList());
        }
    }

    // ================= SUBJECTS =================

    public List<String> getSubjectsForStudent(int studentId) {
        buildCacheIfNeeded();
        Map<String, Map<String, Double>> m = cache.get(studentId);
        if (m == null) return List.of();
        return m.keySet().stream().sorted().toList();
    }

    // ================= FINAL GRADES =================

    public Map<String, Double> getFinalGradesForStudent(int studentId) {
        buildCacheIfNeeded();
        Map<String, Map<String, Double>> m = cache.get(studentId);
        if (m == null) return Map.of();

        Map<String, Double> finals = new LinkedHashMap<>();

        for (String subject : m.keySet()) {
            Double fin = m.get(subject).get("FINAL");
            finals.put(subject, fin == null ? null : round2(fin));
        }
        return finals;
    }

    public Double getFinalAverageForStudent(int studentId) {
        buildCacheIfNeeded();
        Map<String, Map<String, Double>> m = cache.get(studentId);
        if (m == null) return null;

        double sum = 0;
        int count = 0;

        for (String subject : m.keySet()) {
            Double fin = m.get(subject).get("FINAL");
            // treat null or non-positive as "no final" -> student has no average
            if (fin == null || fin <= 0) return null;
            sum += fin;
            count++;
        }

        return count == 0 ? null : round2(sum / count);
    }

    public Map<Integer, Double> getFinalAveragesForAllStudents() {
        buildCacheIfNeeded();
        Map<Integer, Double> out = new LinkedHashMap<>();

        for (Student s : studentRepository.findAll()) {
            out.put(s.getId(), getFinalAverageForStudent(s.getId()));
        }
        return out;
    }

    // ================= ANALYTICS =================

    public boolean hasFinals() {
        for (Student s : studentRepository.findAll()) {
            if (getFinalAverageForStudent(s.getId()) != null) return true;
        }
        return false;
    }

    public Double getOverallAverage() {
        double sum = 0;
        int count = 0;

        for (Student s : studentRepository.findAll()) {
            Double avg = getFinalAverageForStudent(s.getId());
            if (avg != null) {
                sum += avg;
                count++;
            }
        }
        return count == 0 ? null : round2(sum / count);
    }

    public int countStudentsWithFinals() {
        int c = 0;
        for (Student s : studentRepository.findAll()) {
            if (getFinalAverageForStudent(s.getId()) != null) c++;
        }
        return c;
    }

    public int getSubjectsCount() {
        buildCacheIfNeeded();
        Set<String> set = new HashSet<>();

        for (Map<String, Map<String, Double>> st : cache.values()) {
            set.addAll(st.keySet());
        }
        return set.size();
    }

    public List<Integer> getPieData() {
        int a = 0, b = 0, c = 0, d = 0;

        for (Student s : studentRepository.findAll()) {
            Double avg = getFinalAverageForStudent(s.getId());
            if (avg == null) continue;

            if (avg <= 50) a++;
            else if (avg <= 70) b++;
            else if (avg <= 85) c++;
            else d++;
        }

        return List.of(a, b, c, d);
    }

    public List<String> getPieLabels() {
        return List.of("0–50", "50–70", "70–85", "85–100");
    }

    public List<String> getSubjectsLabels() {
        buildCacheIfNeeded();
        return cache.values().stream()
                .flatMap(m -> m.keySet().stream())
                .distinct()
                .sorted()
                .toList();
    }

    public List<Double> getSubjectsValues() {
        buildCacheIfNeeded();
        List<String> subjects = getSubjectsLabels();
        List<Double> out = new ArrayList<>();

        for (String sub : subjects) {
            double sum = 0;
            int count = 0;

            for (Map<String, Map<String, Double>> st : cache.values()) {
                Double fin = st.getOrDefault(sub, Map.of()).get("FINAL");
                if (fin != null && fin > 0) {
                    sum += fin;
                    count++;
                }
            }
            out.add(count == 0 ? null : round2(sum / count));
        }
        return out;
    }

    // ================= TOP & LOW STUDENTS =================

    public List<Map<String, Object>> getTopStudents() {
        List<Map<String, Object>> list = new ArrayList<>();

        for (Student s : getStudents()) {
            Double avg = getFinalAverageForStudent(s.getId());
            if (avg == null) continue;

            list.add(Map.of(
                    "fullname", s.getFullname(),
                    "groupName", s.getGroupName(),
                    "finalScore", avg
            ));
        }

        return list.stream()
                .sorted((a, b) -> Double.compare(
                        (double) b.get("finalScore"),
                        (double) a.get("finalScore")))
                .limit(5)
                .toList();
    }

    public List<Map<String, Object>> getLowStudentsList() {
        List<Map<String, Object>> list = new ArrayList<>();

        for (Student s : getStudents()) {
            Double avg = getFinalAverageForStudent(s.getId());
            if (avg == null) continue;

            list.add(Map.of(
                    "fullname", s.getFullname(),
                    "groupName", s.getGroupName(),
                    "finalScore", avg
            ));
        }

        return list.stream()
                .sorted(Comparator.comparingDouble(a -> (double) a.get("finalScore")))
                .limit(5)
                .toList();
    }

    // ================= ABSENT STUDENTS =================
    // Count only "Н" marks which are represented as -1
    public List<Map<String, Object>> getAbsentLeaders() {
        buildCacheIfNeeded();

        List<Map<String, Object>> out = new ArrayList<>();

        for (Student s : getStudents()) {
            int abs = 0;

            Map<String, Map<String, Double>> st = cache.get(s.getId());
            if (st == null) continue;

            for (Map<String, Double> comp : st.values()) {
                for (Double dv : comp.values()) {
                    if (dv == null) continue;
                    if (Double.compare(dv, -1.0) == 0) abs++;
                }
            }

            out.add(Map.of(
                    "fullname", s.getFullname(),
                    "groupName", s.getGroupName(),
                    "abs", abs
            ));
        }

        return out.stream()
                .sorted((a, b) -> Integer.compare(
                        (int) b.get("abs"),
                        (int) a.get("abs")))
                .limit(5)
                .toList();
    }

    // ================= GROUP-LEVEL ANALYTICS (NEW) =================
    // Labels (group names) in consistent order
    public List<String> getGroupLabels() {
        return getAllGroups();
    }

    // Average FINAL per group (null when no finals in group)
    public List<Double> getGroupValues() {
        List<String> groups = getGroupLabels();
        List<Double> out = new ArrayList<>();

        for (String g : groups) {
            List<Student> students = getStudentsByGroup(g);
            double sum = 0;
            int count = 0;
            for (Student s : students) {
                Double avg = getFinalAverageForStudent(s.getId());
                if (avg != null) {
                    sum += avg;
                    count++;
                }
            }
            out.add(count == 0 ? null : round2(sum / count));
        }
        return out;
    }

    // Best group (label) by average, returns null if none
    public String getBestGroupLabel() {
        List<String> labels = getGroupLabels();
        List<Double> vals = getGroupValues();
        double best = Double.NEGATIVE_INFINITY;
        String bestLabel = null;
        for (int i = 0; i < labels.size(); i++) {
            Double v = vals.get(i);
            if (v != null && v > best) {
                best = v;
                bestLabel = labels.get(i);
            }
        }
        return bestLabel;
    }

    public Double getBestGroupValue() {
        String label = getBestGroupLabel();
        if (label == null) return null;
        List<String> labels = getGroupLabels();
        List<Double> vals = getGroupValues();
        for (int i = 0; i < labels.size(); i++) {
            if (label.equals(labels.get(i))) return vals.get(i);
        }
        return null;
    }

    // Absences by group (total N counts per group)
    public List<Integer> getAbsByGroupValues() {
        buildCacheIfNeeded();
        List<String> groups = getGroupLabels();
        List<Integer> out = new ArrayList<>();

        for (String g : groups) {
            int total = 0;
            List<Student> students = getStudentsByGroup(g);
            for (Student s : students) {
                Map<String, Map<String, Double>> st = cache.get(s.getId());
                if (st == null) continue;
                for (Map<String, Double> comp : st.values()) {
                    for (Double dv : comp.values()) {
                        if (dv == null) continue;
                        if (Double.compare(dv, -1.0) == 0) total++;
                    }
                }
            }
            out.add(total);
        }
        return out;
    }

    public List<String> getAbsByGroupLabels() {
        return getGroupLabels();
    }

    // ================= HISTOGRAM / BUCKETS =================
    // Simple histogram of final averages (useful for distribution chart)
    public List<String> getHistogramLabels() {
        return List.of("0–50", "50–60", "60–70", "70–80", "80–90", "90–100");
    }

    public List<Integer> getHistogramValues() {
        buildCacheIfNeeded();
        int a = 0, b = 0, c = 0, d = 0, e = 0, f = 0;

        for (Student s : studentRepository.findAll()) {
            Double avg = getFinalAverageForStudent(s.getId());
            if (avg == null) continue;

            if (avg < 50) a++;
            else if (avg < 60) b++;
            else if (avg < 70) c++;
            else if (avg < 80) d++;
            else if (avg < 90) e++;
            else f++;
        }

        return List.of(a, b, c, d, e, f);
    }

    // ================= UTIL =================
    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
