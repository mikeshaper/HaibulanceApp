package com.example.haibulance;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.time.Duration;
import java.time.LocalDateTime;

public class RepTime {

    private int minute;
    private int hour;
    private int day;
    private int month;
    private int year;

    public RepTime() {
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public RepTime(LocalDateTime now) {
        this.minute = now.getMinute();
        this.hour = now.getHour();
        this.day = now.getDayOfMonth();
        this.month = now.getMonthValue();
        this.year = now.getYear();
    }

    public void setMinute(int minute) {
        this.minute = minute;
    }
    public void setHour(int hour) {
        this.hour = hour;
    }
    public void setDay(int day) {
        this.day = day;
    }
    public void setMonth(int month) {
        this.month = month;
    }
    public void setYear(int year) {
        this.year = year;
    }
    public int getMinute() {
        return minute;
    }
    public int getHour() {
        return hour;
    }
    public int getDay() {
        return day;
    }
    public int getMonth() {
        return month;
    }
    public int getYear() {
        return year;
    }

    public String ToString(){
        String minstr = String.valueOf(minute);
        if (minute < 10){
            minstr = "0" + minute;
        }
        return String.format("%s/%s/%s, %s:%s", day,month,year,hour,minstr);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public float ageInHrs(){
        LocalDateTime other = LocalDateTime.now();
        LocalDateTime repTime = LocalDateTime.of(year, month, day, hour, minute);
        Duration duration = Duration.between(other, repTime);
        long diff = Math.abs(duration.toHours());
        return diff;
    }

}
