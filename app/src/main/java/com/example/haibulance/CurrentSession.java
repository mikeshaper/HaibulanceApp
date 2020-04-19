package com.example.haibulance;

public class CurrentSession {

    private static Report report = new Report(); // the current report to refer to
    private static User user = new User(); // the current user to refer to
    private static String requestedMonth; //the month to refer to in the Analysis
    private static String requestedYear; //the year to refer to in the Analysis
    private boolean onRepActivity; // helps to get control if the report activity is on (otherwise the "ondatachanged" func in the report activity makes problems...)
    private static Boolean menuActivityFinished = false; // helps to know if the home button is clicked

    /**
     * no params constructor
     */
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

    /**
     * the month to refer to in the Analysis
     */
    public static String getRequestedMonth() {
        return requestedMonth;
    }
    public static void setRequestedMonth(String requestedMonth) {
        CurrentSession.requestedMonth = requestedMonth;
    }
    /**
     * the year to refer to in the Analysis
     */
    public static String getRequestedYear() {
        return requestedYear;
    }
    public static void setRequestedYear(String requestedYear) {
        CurrentSession.requestedYear = requestedYear;
    }

    /**
     * helps to get control if the report activity is on (otherwise the "ondatachanged" func in the report activity makes problems...)
     */
    public void setOnRepActivity(boolean onRepActivity) {
        this.onRepActivity = onRepActivity;
    }
    public boolean isOnRepActivity() {
        return onRepActivity;
    }

    /**
     * helps to know if the home button is clicked
     */
    public static Boolean isMenuActivityFinished() {
        return menuActivityFinished;
    }
    public static void setMenuActivityFinished(Boolean menuActivityFinished) {
        CurrentSession.menuActivityFinished = menuActivityFinished;
    }
}
