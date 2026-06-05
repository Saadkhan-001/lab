package com.lab.lms.models;

public class LabBranding {
    public final String name;
    public final String address;
    public final String contact;
    public final String email;
    public final String website;
    public final String tagline;
    public final String logoPath;
    public final String policies;

    public LabBranding(String name, String address, String contact, String email, String website, String tagline, String logoPath, String policies) {
        this.name = name;
        this.address = address;
        this.contact = contact;
        this.email = email;
        this.website = website;
        this.tagline = tagline;
        this.logoPath = logoPath;
        this.policies = policies;
    }
}
