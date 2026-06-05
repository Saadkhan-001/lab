package com.lab.lms.models;

public class Staff {
    private String staffId;
    private String name;
    private String gender;
    private String address;
    private String qualification;
    private String phone;
    private String dob;
    private String password;
    private String permissions;
    private String designation;
    private String profilePicture;

    public Staff(String staffId, String name, String gender, String address, String qualification, String phone,
            String dob, String password) {
        this.staffId = staffId;
        this.name = name;
        this.gender = gender;
        this.address = address;
        this.qualification = qualification;
        this.phone = phone;
        this.dob = dob;
        this.password = password;
        this.permissions = "";
        this.designation = "";
        this.profilePicture = "";
    }

    public Staff(String staffId, String name, String gender, String address, String qualification, String phone,
            String dob, String password, String permissions) {
        this(staffId, name, gender, address, qualification, phone, dob, password);
        this.permissions = permissions;
    }

    public Staff(String staffId, String name, String gender, String address, String qualification, String phone,
            String dob, String password, String permissions, String designation, String profilePicture) {
        this(staffId, name, gender, address, qualification, phone, dob, password, permissions);
        this.designation = designation;
        this.profilePicture = profilePicture;
    }

    public String getStaffId() {
        return staffId;
    }

    public void setStaffId(String staffId) {
        this.staffId = staffId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getQualification() {
        return qualification;
    }

    public void setQualification(String qualification) {
        this.qualification = qualification;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getDob() {
        return dob;
    }

    public void setDob(String dob) {
        this.dob = dob;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPermissions() {
        return permissions;
    }

    public void setPermissions(String permissions) {
        this.permissions = permissions;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }
}
