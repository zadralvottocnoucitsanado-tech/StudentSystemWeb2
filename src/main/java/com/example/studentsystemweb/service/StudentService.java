package com.example.studentsystemweb.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.studentsystemweb.model.Attendance;
import com.example.studentsystemweb.model.Grade;
import com.example.studentsystemweb.model.ProgressRecord;
import com.example.studentsystemweb.model.Student;
import com.example.studentsystemweb.repository.AttendanceRepository;
import com.example.studentsystemweb.repository.GradeRepository;
import com.example.studentsystemweb.repository.ProgressRepository;
import com.example.studentsystemweb.repository.StudentRepository;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final GradeRepository gradeRepository;
    private final ProgressRepository progressRepository;
    private final AttendanceRepository attendanceRepository;

    public StudentService(StudentRepository studentRepository,
                          GradeRepository gradeRepository,
                          ProgressRepository progressRepository,
                          AttendanceRepository attendanceRepository) {
        this.studentRepository = studentRepository;
        this.gradeRepository = gradeRepository;
        this.progressRepository = progressRepository;
        this.attendanceRepository = attendanceRepository;
    }

    // ---------------- BASIC ----------------

    public Student getStudent(int id) {
        return studentRepository.findById(id);
    }

    public List<Grade> getRawComponents(int studentId) {
        return gradeRepository.findByStudent(studentId);
    }

    // ---------------- FINAL GRADES ----------------

    public List<Grade> getGrades(int studentId) {

        List<String> subjects = gradeRepository.findSubjectsForStudent(studentId);
        if (subjects.isEmpty()) return Collections.emptyList();

        List<Grade> finals = new ArrayList<>();

        for (String subject : subjects) {

            Map<String, Double> map = gradeRepository.getComponentMap(studentId, subject);

            if (map == null) map = new LinkedHashMap<>();

            double[] weekAvg = new double[15];
            int[] has = new int[15];

            for (int w = 1; w <= 15; w++) {

                double sum = 0;
                int cnt = 0;

                for (String t : new String[]{"lecture", "sro", "lab"}) {

                    String key = "week" + w + "_" + t;
                    if (!map.containsKey(key)) continue;

                    double v = map.get(key);

                    if (v >= 0) {
                        sum += v;
                        cnt++;
                    }
                }

                if (cnt > 0) {
                    weekAvg[w - 1] = sum / cnt;
                    has[w - 1] = 1;
                }
            }

            double tk1 = averageRange(weekAvg, has, 0, 6);
            double tk2 = averageRange(weekAvg, has, 7, 14);

            double rk1 = safe(map.getOrDefault("rk1", 0.0));
            double rk2 = safe(map.getOrDefault("rk2", 0.0));
            double exam = safe(map.getOrDefault("exam", 0.0));

            double r1 = tk1 * 0.6 + rk1 * 0.4;
            double r2 = tk2 * 0.6 + rk2 * 0.4;

            double dopusk = (r1 + r2) / 2;

            double finalScore = dopusk * 0.6 + exam * 0.4;

            Grade g = new Grade();
            g.setStudentId(studentId);
            g.setSubject(subject);
            g.setWorkType("FINAL");
            g.setGrade(round2(finalScore));

            finals.add(g);
        }

        return finals;
    }

    // ---------------- AVERAGE ----------------

    public Double getAverage(int studentId) {
        List<Grade> finals = getGrades(studentId);

        if (finals.isEmpty()) return 0.0;

        return round2(
                finals.stream().mapToDouble(Grade::getGrade).sum() / finals.size()
        );
    }

    // ---------------- PROGRESS ----------------

    public List<ProgressRecord> getProgress(int studentId) {
        return progressRepository.findByStudent(studentId);
    }

    public List<Attendance> getAttendance(int studentId) {
        return attendanceRepository.findByStudentId(studentId);
    }

    public int countPresent(int studentId) {
        return attendanceRepository.countPresent(studentId);
    }

    public int countAbsent(int studentId) {
        return attendanceRepository.countAbsent(studentId);
    }

    // ---------------- SUBJECT AVERAGES ----------------

    public Map<String, Double> getSubjectAverages(int studentId) {

        List<Grade> finals = getGrades(studentId);
        Map<String, Double> map = new LinkedHashMap<>();

        for (Grade g : finals) {
            map.put(g.getSubject(), g.getGrade());
        }

        return map;
    }

    public List<String> getSubjectLabels(int studentId) {
        return new ArrayList<>(getSubjectAverages(studentId).keySet());
    }

    public List<Double> getSubjectValues(int studentId) {
        return new ArrayList<>(getSubjectAverages(studentId).values());
    }

    // ---------------- FULL JOURNAL ----------------

    public Map<String, Map<String, Object>> generateFullJournal(int studentId) {

        List<String> subjects = gradeRepository.findSubjectsForStudent(studentId);
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();

        for (String subject : subjects) {

            Map<String, Double> db = gradeRepository.getComponentMap(studentId, subject);
            if (db == null) db = new LinkedHashMap<>();

            Map<String, Object> row = new LinkedHashMap<>();

            for (String t : new String[]{"lecture", "sro", "lab"}) {
                for (int i = 1; i <= 15; i++) {
                    String key = "week" + i + "_" + t;
                    row.put(key, db.getOrDefault(key, null));
                }
            }

            row.put("tk1", db.getOrDefault("tk1", null));
            row.put("rk1", db.getOrDefault("rk1", null));
            row.put("r1", db.getOrDefault("r1", null));

            row.put("tk2", db.getOrDefault("tk2", null));
            row.put("rk2", db.getOrDefault("rk2", null));
            row.put("r2", db.getOrDefault("r2", null));

            row.put("dopusk", db.getOrDefault("dopusk", null));
            row.put("exam", db.getOrDefault("exam", null));
            row.put("FINAL", db.getOrDefault("FINAL", null));

            result.put(subject, row);
        }

        return result;
    }

    public List<String> getJournalSubjects(int studentId) {
        return gradeRepository.findSubjectsForStudent(studentId);
    }

    // ---------------- UTIL ----------------

    private double safe(double v) {
        return v < 0 ? 0 : v;
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private double averageRange(double[] arr, int[] has, int from, int to) {
        double sum = 0;
        int cnt = 0;

        for (int i = from; i <= to; i++) {
            if (has[i] == 1) {
                sum += arr[i];
                cnt++;
            }
        }

        return cnt == 0 ? 0.0 : sum / cnt;
    }
}
