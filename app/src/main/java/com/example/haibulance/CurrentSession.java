package com.example.haibulance;

public class CurrentSession {

    private static Report report = new Report();
    private static User user = new User();
    private static String requestedMonth;
    private static String requestedYear;
    private boolean onRepActivity;

    private static Boolean menuActivityFinished = false;

    public CurrentSession() {
    }
    public Report getRep() {
        return report;
    }
    public void setRep(Report rep) {
        this.report = rep;
    }
    public User getUser() {
        return user;
    }
    public void setUser(User user) {
        this.user = user;
    }
    public static String getRequestedMonth() {
        return requestedMonth;
    }
    public static void setRequestedMonth(String requestedMonth) {
        CurrentSession.requestedMonth = requestedMonth;
    }
    public static String getRequestedYear() {
        return requestedYear;
    }
    public static void setRequestedYear(String requestedYear) {
        CurrentSession.requestedYear = requestedYear;
    }
    public void setOnRepActivity(boolean onRepActivity) {
        this.onRepActivity = onRepActivity;
    }
    public boolean isOnRepActivity() {
        return onRepActivity;
    }

    public static Boolean isMenuActivityFinished() {
        return menuActivityFinished;
    }
    public static void setMenuActivityFinished(Boolean menuActivityFinished) {
        CurrentSession.menuActivityFinished = menuActivityFinished;
    }
}
