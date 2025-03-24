package com.lgzClient.utils;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class TimeUtil {

    private static DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 将字符串转换为 LocalDateTime
     *
     * @param date 日期字符串
     * @return LocalDateTime 对象
     */
    public static LocalDateTime strToDate(String date) {
        try {
            return LocalDateTime.parse(date, dateFormat);
        } catch (DateTimeParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取当前时间的时间戳
     *
     * @return 当前时间的时间戳
     */
    public static Long getNowTime() {
        return System.currentTimeMillis();
    }

    /**
     * 获取某年某月的第一天
     *
     * @param year  年份
     * @param month 月份
     * @return 第一天的 LocalDateTime
     */
    public static LocalDateTime getFirstDayOfMonth(int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        return yearMonth.atDay(1).atStartOfDay();
    }

    /**
     * 获取某年某月的最后一天
     *
     * @param year  年份
     * @param month 月份
     * @return 最后一天的 LocalDateTime
     */
    public static LocalDateTime getLastDayOfMonth(int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        return yearMonth.atEndOfMonth().atTime(23, 59, 59);
    }

    /**
     * 获取当前时间的格式化字符串
     *
     * @return 当前时间的格式化字符串
     */
    public static String getLocalTime() {
        return LocalDateTime.now().format(dateFormat);
    }

    /**
     * 获取指定日期的开始时间（00:00:00）
     *
     * @param date 指定日期
     * @return 开始时间
     */
    public static LocalDateTime getStartOfDay(LocalDateTime date) {
        return date.toLocalDate().atStartOfDay();
    }

    /**
     * 获取指定日期的结束时间（23:59:59）
     *
     * @param date 指定日期
     * @return 结束时间
     */
    public static LocalDateTime getEndOfDay(LocalDateTime date) {
        return date.toLocalDate().atTime(23, 59, 59);
    }

    /**
     * 从日期字符串中提取年月日部分
     *
     * @param date 日期字符串
     * @return 年月日部分
     */
    public static String getYYYY_MM_DD(String date) {
        return date.split(" ")[0];
    }

    /**
     * 从日期字符串中提取时分秒部分
     *
     * @param date 日期字符串
     * @return 时分秒部分
     */
    public static String getHH_mm_ss(String date) {
        return date.split(" ")[1];
    }

    /**
     * 将时间戳格式化为日期字符串
     *
     * @param time 时间戳
     * @return 格式化后的日期字符串
     */
    public static String formatTime(Long time) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.systemDefault()).format(dateFormat);
    }

    /**
     * 将 LocalDateTime 格式化为日期字符串
     *
     * @param date LocalDateTime 对象
     * @return 格式化后的日期字符串
     */
    public static String formatTime(LocalDateTime date) {
        return date.format(dateFormat);
    }

    /**
     * 比较两个日期字符串的大小
     *
     * @param a 日期字符串 A
     * @param b 日期字符串 B
     * @return 比较结果
     */
    public static Integer compare(String a, String b) {
        if (a == null && b == null) return 0;
        else if (a == null) return -1;
        else if (b == null) return 1;
        LocalDateTime dateA = LocalDateTime.parse(a, dateFormat);
        LocalDateTime dateB = LocalDateTime.parse(b, dateFormat);
        return dateA.compareTo(dateB);
    }

    /**
     * 判断时间是否过期
     *
     * @param time       时间戳
     * @param expireTime 过期时间（秒）
     * @return 是否过期
     */
    public static boolean isExpire(Long time, Integer expireTime) {
        long c = System.currentTimeMillis() - time;
        return c / 1000 > expireTime;
    }

    /**
     * 计算两个时间字符串之间的秒数差
     *
     * @param oldTime 旧时间
     * @param newTime 新时间
     * @return 秒数差
     */
    public static long getPastSeconds(String oldTime, String newTime) {
        LocalDateTime ot = LocalDateTime.parse(oldTime, dateFormat);
        LocalDateTime nt = LocalDateTime.parse(newTime, dateFormat);
        return Duration.between(ot, nt).getSeconds();
    }

    /**
     * 计算两个日期之间的时间差
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 时间差
     */
    public static Period calculateTimeDifference(LocalDate startDate, LocalDate endDate) {
        return Period.between(startDate, endDate);
    }

    /**
     * 计算两个日期之间的年份差值
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 年份差值
     */
    public static int getYearsDifference(LocalDate startDate, LocalDate endDate) {
        return Period.between(startDate, endDate).getYears();
    }

    /**
     * 计算两个日期之间的月份差值
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 月份差值
     */
    public static int getMonthsDifference(LocalDate startDate, LocalDate endDate) {
        Period period = Period.between(startDate, endDate);
        return period.getYears() * 12 + period.getMonths();
    }
}