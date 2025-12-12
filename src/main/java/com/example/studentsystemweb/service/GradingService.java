package com.example.studentsystemweb.service;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.studentsystemweb.repository.GradeRepository;

@Service
public class GradingService {

    private final GradeRepository gradeRepository;

    public GradingService(GradeRepository gradeRepository) {
        this.gradeRepository = gradeRepository;
    }

   
    public Map<String, Double> getComponentMap(int studentId, String subject) {
        Map<String, Double> map = gradeRepository.getComponentMap(studentId, subject);
        return map != null ? map : new LinkedHashMap<>();
    }

   
    public Map<String, Double> getJournalTable(int studentId, String subject) {

        Map<String, Double> src = gradeRepository.getComponentMap(studentId, subject);
        if (src == null) src = new LinkedHashMap<>();

        Map<String, Double> table = new LinkedHashMap<>();

        String[] types = {"lecture", "sro", "lab"};

        
        for (int week = 1; week <= 15; week++) {
            for (String t : types) {

                String key = "week" + week + "_" + t;
                String flagKey = key + "_flag";

                double flag = src.getOrDefault(flagKey, 0.0);
                double value = src.getOrDefault(key, 0.0);

                table.put(flagKey, flag);

                if (flag == 1.0) {
                    table.put(key, -1.0);  // Н
                } else if (flag == 2.0) {
                    table.put(key, -2.0);  // Н.П
                } else {
                    table.put(key, value);
                }
            }
        }

        // РК / Экзамены
        for (String k : new String[]{"rk1", "rk2", "exam"}) {
            String flagKey = k + "_flag";

            double flag = src.getOrDefault(flagKey, 0.0);
            double value = src.getOrDefault(k, 0.0);

            if (flag == 2.0) flag = 1.0; // экзамен не может быть Н.П

            table.put(flagKey, flag);

            if (flag == 1.0) {
                table.put(k, -1.0);
            } else {
                table.put(k, value);
            }
        }

        // Итоги
        for (String k : new String[]{"tk1", "tk2", "r1", "r2", "dopusk", "FINAL"}) {
            table.put(k, src.getOrDefault(k, 0.0));
        }

        return table;
    }

    // ------------------------------------------------------
    // Сохранение флага (0/1/2)
    // ------------------------------------------------------
    public void setFlagValue(int studentId, String subject, String workType, double flagValue) {
        gradeRepository.upsertComponent(studentId, subject, workType + "_flag", flagValue);
    }

    // ------------------------------------------------------
    // Сохранение значения компонента
    // ------------------------------------------------------
    public void setComponent(int studentId, String subject, String workType, Double grade) {
        if (grade == null) return;

        if (grade < 0) { // Н (-1) или Н.П (-2)
            if (grade != -1.0 && grade != -2.0) grade = -1.0;
            gradeRepository.upsertComponent(studentId, subject, workType, grade);
            return;
        }

        // Ограничиваем нормальные оценки 0..100
        double g = Math.min(100, Math.max(0, grade));
        gradeRepository.upsertComponent(studentId, subject, workType, g);
    }

    // ------------------------------------------------------
    // Пересчёт итогов 
    // ------------------------------------------------------
    public void recalcTotals(int studentId, String subject) {

        Map<String, Double> comp = gradeRepository.getComponentMap(studentId, subject);
        if (comp == null) comp = new LinkedHashMap<>();

        String[] types = {"lecture", "sro", "lab"};

        double[] weekAvg = new double[15];
        int[] cnt = new int[15];

        // Среднее за неделю
        for (int w = 1; w <= 15; w++) {
            double sum = 0;
            int c = 0;

            for (String t : types) {

                String key = "week" + w + "_" + t;
                String flagKey = key + "_flag";

                double flag = comp.getOrDefault(flagKey, 0.0);
                double value = comp.getOrDefault(key, 0.0);

                if (flag == 2.0) continue; // Н.П — исключено из расчётов

                if (flag == 1.0) { // Н — 0 баллов
                    c++;
                    continue;
                }

                if (value >= 0) {
                    sum += value;
                    c++;
                }
            }

            if (c > 0) {
                weekAvg[w - 1] = sum / c;
                cnt[w - 1] = 1;
            }
        }

        double tk1 = avgRange(weekAvg, cnt, 0, 6);
        double tk2 = avgRange(weekAvg, cnt, 7, 14);

        // РК и Экзамен
        double rk1 = prepareExamValue(comp, "rk1");
        double rk2 = prepareExamValue(comp, "rk2");
        double exam = prepareExamValue(comp, "exam");

        double r1 = tk1 * 0.6 + rk1 * 0.4;
        double r2 = tk2 * 0.6 + rk2 * 0.4;

        double dopusk = (r1 + r2) / 2.0;
        double finalScore = dopusk * 0.6 + exam * 0.4;

        // Сохранение
        gradeRepository.upsertComponent(studentId, subject, "tk1", round(tk1));
        gradeRepository.upsertComponent(studentId, subject, "tk2", round(tk2));
        gradeRepository.upsertComponent(studentId, subject, "r1", round(r1));
        gradeRepository.upsertComponent(studentId, subject, "r2", round(r2));
        gradeRepository.upsertComponent(studentId, subject, "dopusk", round(dopusk));
        gradeRepository.upsertComponent(studentId, subject, "FINAL", round(finalScore));
    }

    // ------------------------------------------------------
    // Helper расчёта экзамена / РК
    // ------------------------------------------------------
    private double prepareExamValue(Map<String, Double> comp, String key) {

        double val = comp.getOrDefault(key, 0.0);
        double flag = comp.getOrDefault(key + "_flag", 0.0);

        if (flag == 1.0 || flag == 2.0 || val < 0) return 0.0;

        return val;
    }

    // ------------------------------------------------------
    // Helpers
    // ------------------------------------------------------
    private double avgRange(double[] arr, int[] has, int from, int to) {
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

    private double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
