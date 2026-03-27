package com.downloadc.downloadc.model;

// Holds LMS username, password, and the token we get
// This is ENCAPSULATION — we keep related data together in one object.

public class MoodleConfig {

    // Fields are private to protect the data (Encapsulation)
    // We'll get token after login
    private String username;
    private String password;
    private String token;

    // The URL of the LmS thst is same for every student
    private static final String BASE_URL = "https://lms.nust.edu.pk/portal";

    // Constructor
    // Token will be there after the LogIN
    public MoodleConfig(String username, String password) {
        this.username = username;
        this.password = password;
        this.token = null;
    }

    //GETTERS
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getToken()    { return token; }
    public String getBaseUrl()  { return BASE_URL; }

    //SETTER
    public void setToken(String token) {
        this.token = token;
    }
}
