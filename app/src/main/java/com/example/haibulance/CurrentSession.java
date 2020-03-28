package com.example.haibulance;

public class CurrentSession {

    private static Report report = new Report();
    private static User user = new User();

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
        CurrentSession.user = user;
    }
}
