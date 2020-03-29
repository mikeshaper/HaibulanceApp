package com.example.haibulance;

public class CurrentSession {

    private static Report report = new Report();
    private static User user = new User();

    private static Boolean menuActivityFinished = false;

    public CurrentSession() {
    }
    public Report getRep() {
        return report;
    }
    public void setRep(Report rep) {
        this.report = rep;//new Report(rep);
    }
    public User getUser() {
        return user;
    }
    public void setUser(User user) {
        this.user = user;
    }


    public static Boolean isMenuActivityFinished() {
        return menuActivityFinished;
    }
    public static void setMenuActivityFinished(Boolean menuActivityFinished) {
        CurrentSession.menuActivityFinished = menuActivityFinished;
    }
}
