package com.myciti.dto;

import com.myciti.model.AccountType;
import com.myciti.model.Role;
import com.myciti.model.UserStatus;

public class UserDto {
    private Long id;
    private String email;
    private String name;
    private String pictureUrl;
    private AccountType accountType;
    private String businessName;
    private String phone;
    private Role role;
    private UserStatus status;
    private boolean profileComplete;

    public UserDto() {}

    public UserDto(Long id, String email, String name, String pictureUrl, AccountType accountType,
                   String businessName, String phone, Role role, UserStatus status, boolean profileComplete) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.pictureUrl = pictureUrl;
        this.accountType = accountType;
        this.businessName = businessName;
        this.phone = phone;
        this.role = role;
        this.status = status;
        this.profileComplete = profileComplete;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPictureUrl() { return pictureUrl; }
    public void setPictureUrl(String pictureUrl) { this.pictureUrl = pictureUrl; }

    public AccountType getAccountType() { return accountType; }
    public void setAccountType(AccountType accountType) { this.accountType = accountType; }

    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }

    public boolean isProfileComplete() { return profileComplete; }
    public void setProfileComplete(boolean profileComplete) { this.profileComplete = profileComplete; }
}
