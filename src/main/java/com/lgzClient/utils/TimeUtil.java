package com.lgzClient.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.util.Calendar;
import java.util.Date;

public class TimeUtil {

    public static Date convertLocalDateToDate(LocalDate localDate) {
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static Date strToDate(String date){
        try {
            return dateFormat.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }
    public static Long getNowTime(){
        return new Date().getTime();
    }
    public static Date getFirstDayOfMonth(int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        return convertLocalDateToDate(yearMonth.atDay(1));
    }
    public static Date getLastDayOfMonth(int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        return convertLocalDateToDate(yearMonth.atEndOfMonth());
    }
    public static String getLocalTime() {
        Calendar calendar = Calendar.getInstance();
        return dateFormat.format(calendar.getTime());
    }
    /**
     * 获取指定日期的开始时间（00:00:00）
     * @param date 指定日期
     * @return 开始时间
     */
    public static Date getStartOfDay(Date date) {
        LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDateTime startOfDay = localDate.atStartOfDay();
        return Date.from(startOfDay.atZone(ZoneId.systemDefault()).toInstant());
    }
    /**
     * 获取指定日期的结束时间（23:59:59）
     * @param date 指定日期
     * @return 结束时间
     */
    public static Date getEndOfDay(Date date) {
        LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDateTime endOfDay = localDate.atTime(23, 59, 59, 999999999);
        return Date.from(endOfDay.atZone(ZoneId.systemDefault()).toInstant());
    }
    public static String getYYYY_MM_DD(String date){
        return date.split(" ")[0];
    }
    public static String getHH_mm_ss(String date){
        return date.split(" ")[1];
    }
    public static String formatTime(Long time){
        Date date=new Date(time);
        return dateFormat.format(date);
    }
    public static String formatTime(Date date){
        return dateFormat.format(date);
    }
    public static Integer compare(String a, String b) throws ParseException {
        if (a == null && b == null) return 0;
        else if (a == null) return -1;
        else if (b == null) return 1;
        Date date = dateFormat.parse(a);
        Date dateB = dateFormat.parse(b);
        return date.compareTo(dateB);
    }

    public static boolean isExpire(Long time, Integer expireTime) {//单位为s
        long c = new Date().getTime() - time;
        return c / 1000 > expireTime;
    }
    public static long getPastSeconds(String oldTime,String newTime) throws ParseException {
            Long ot=dateFormat.parse(oldTime).getTime();
            Long nt=dateFormat.parse(newTime).getTime();


        return (nt-ot)/1000;//获得过去的时间
    }

    /**
     * 获取两个日期之间的时间差
     * @param startDate
     * @param endDate
     * @return
     */
    public static Period calculateTimeDifference(LocalDate startDate, LocalDate endDate) {
        return Period.between(startDate, endDate);
    }
    /**
     * 获取两个日期之间的年份差值
     *
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 年份差值
     */
    public static int getYearsDifference(LocalDate startDate, LocalDate endDate) {
        return Period.between(startDate, endDate).getYears();
    }

    /**
     * 获取两个日期之间的月份差值
     *
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 月份差值
     */
    public static int getMonthsDifference(LocalDate startDate, LocalDate endDate) {
        Period period = Period.between(startDate, endDate);
        // 计算总月份差值（包括年的月份）
        return period.getYears() * 12 + period.getMonths();
    }
}
