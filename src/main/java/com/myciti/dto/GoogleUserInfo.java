package com.myciti.dto;

public class GoogleUserInfo {
    private String sub;
    private String email;
    private boolean emailVerified;
    private String name;
    private String pictureUrl;

    public GoogleUserInfo() {}

    public GoogleUserInfo(String sub, String email, boolean emailVerified, String name, String pictureUrl) {
        this.sub = sub;
        this.email = email;
        this.emailVerified = emailVerified;
        this.name = name;
        this.pictureUrl = pictureUrl;
    }

    public String getSub() {
        return sub;
    }

    public void setSub(String sub) {
        this.sub = sub;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPictureUrl() {
        return pictureUrl;
    }

    public void setPictureUrl(String pictureUrl) {
        this.pictureUrl = pictureUrl;
    }
}
