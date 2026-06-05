package com.lab.lms.services;

import java.util.ArrayList;
import java.util.List;
import com.lab.lms.models.Test;

public class SessionContext {
    private static String currentPatientId;
    private static String currentSampleId;
    private static Integer currentTestId;
    private static String userRole;
    private static String username;
    private static String staffId;
    private static String permissions = "";
    private static String targetTab;
    private static boolean editProfileMode = false;

    public static void setEditProfileMode(boolean mode) {
        editProfileMode = mode;
    }

    public static boolean isEditProfileMode() {
        return editProfileMode;
    }

    public static void setTargetTab(String tab) {
        targetTab = tab;
    }

    public static String getTargetTab() {
        return targetTab;
    }

    private static List<Test> selectedTests = new ArrayList<>();

    public static void setCurrentPatientId(String pid) {
        currentPatientId = pid;
    }

    public static String getCurrentPatientId() {
        return currentPatientId;
    }

    public static void setCurrentSampleId(String sid) {
        currentSampleId = sid;
    }

    public static String getCurrentSampleId() {
        return currentSampleId;
    }

    public static void setCurrentTestId(Integer tid) {
        currentTestId = tid;
    }

    public static Integer getCurrentTestId() {
        return currentTestId;
    }

    public static void setSelectedTests(List<Test> tests) {
        selectedTests = tests;
    }

    public static List<Test> getSelectedTests() {
        return selectedTests;
    }

    public static String getUserRole() {
        return userRole;
    }

    public static void setUserRole(String role) {
        userRole = role;
    }

    public static String getUsername() {
        return username;
    }

    public static void setUsername(String name) {
        username = name;
    }

    public static String getStaffId() {
        return staffId;
    }

    public static void setStaffId(String id) {
        staffId = id;
    }

    public static String getPermissions() {
        return permissions;
    }

    public static void setPermissions(String perms) {
        permissions = perms != null ? perms : "";
    }

    public static boolean hasPermission(String perm) {
        if ("ADMIN".equals(userRole)) return true;
        return permissions.contains(perm);
    }
}
