package com.lab.lms.models;

import java.util.ArrayList;
import java.util.List;

public class Test {
    private int id;
    private String name;
    private String numericCode;
    private String alphaCode;
    private String category;
    private double price;
    private String resultTime;
    private String notes;
    private int isSpecial; // 0 normal, 1 special
    private int isMicroscopic; // 0 no, 1 yes
    private int isCulture; // 0 no, 1 yes
    private String specimen;
    private String imagePath;
    private String protocolClass; // ACTIVE or INACTIVE
    private String container;
    private String volume;
    private String fasting;
    private String growthStatus; // Positive/Negative for Culture Tests
    private String growthFindings; // Detailed findings for Culture Tests
    private List<TestParameter> parameters = new ArrayList<>();
    private boolean selected;
    private String status;
    private final String lowercaseName;

    public Test(int id, String numericCode, String alphaCode, String name, String category, double price, String resultTime, String notes, int isSpecial, int isMicroscopic, int isCulture, String specimen, String imagePath, String protocolClass, String container, String volume, String fasting) {
        this(id, numericCode, alphaCode, name, category, price, resultTime, notes, isSpecial, isMicroscopic, isCulture, specimen, imagePath, protocolClass, container, volume, fasting, "Positive", "");
    }

    public Test(int id, String numericCode, String alphaCode, String name, String category, double price, String resultTime, String notes, int isSpecial, int isMicroscopic, int isCulture, String specimen, String imagePath, String protocolClass, String container, String volume, String fasting, String growthStatus, String growthFindings) {
        this.id = id;
        this.numericCode = numericCode;
        this.alphaCode = alphaCode;
        this.name = name;
        this.lowercaseName = (name == null) ? "" : name.toLowerCase();
        this.category = category;
        this.price = price;
        this.resultTime = resultTime;
        this.notes = notes;
        this.isSpecial = isSpecial;
        this.isMicroscopic = isMicroscopic;
        this.isCulture = isCulture;
        this.specimen = specimen;
        this.imagePath = imagePath;
        this.protocolClass = protocolClass == null ? "ACTIVE" : protocolClass;
        this.container = container;
        this.volume = volume;
        this.fasting = fasting;
        this.growthStatus = growthStatus == null ? "Positive" : growthStatus;
        this.growthFindings = growthFindings;
    }

    public Test(int id, String numericCode, String alphaCode, String name, String category, double price, String resultTime, String notes, int isSpecial, int isMicroscopic, int isCulture, String specimen, String imagePath, String container, String volume, String fasting) {
        this(id, numericCode, alphaCode, name, category, price, resultTime, notes, isSpecial, isMicroscopic, isCulture, specimen, imagePath, "ACTIVE", container, volume, fasting);
    }

    public Test(int id, String numericCode, String alphaCode, String name, String category, double price, String resultTime, String notes, int isSpecial, int isMicroscopic, int isCulture, String specimen, String imagePath, String protocolClass) {
        this(id, numericCode, alphaCode, name, category, price, resultTime, notes, isSpecial, isMicroscopic, isCulture, specimen, imagePath, protocolClass, "", "", "");
    }

    public Test(int id, String numericCode, String alphaCode, String name, String category, double price, String resultTime, String notes, int isSpecial, int isMicroscopic, int isCulture, String specimen, String imagePath) {
        this(id, numericCode, alphaCode, name, category, price, resultTime, notes, isSpecial, isMicroscopic, isCulture, specimen, imagePath, "ACTIVE", "", "", "");
    }
    
    // Legacy mapping (default isCulture to 0)
    public Test(int id, String numericCode, String alphaCode, String name, String category, double price, String resultTime, String notes, int isSpecial, int isMicroscopic, String specimen, String imagePath, String protocolClass) {
        this(id, numericCode, alphaCode, name, category, price, resultTime, notes, isSpecial, isMicroscopic, 0, specimen, imagePath, protocolClass);
    }

    public Test(int id, String numericCode, String alphaCode, String name, String category, double price, String resultTime, String notes, int isSpecial, int isMicroscopic, String specimen, String imagePath) {
        this(id, numericCode, alphaCode, name, category, price, resultTime, notes, isSpecial, isMicroscopic, 0, specimen, imagePath);
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public int getIsSpecial() { return isSpecial; }
    public void setIsSpecial(int isSpecial) { this.isSpecial = isSpecial; }
    public int getIsMicroscopic() { return isMicroscopic; }
    public void setIsMicroscopic(int isMicroscopic) { this.isMicroscopic = isMicroscopic; }
    public int getIsCulture() { return isCulture; }
    public void setIsCulture(int isCulture) { this.isCulture = isCulture; }
    public String getSpecimen() { return specimen; }
    public void setSpecimen(String specimen) { this.specimen = specimen; }
    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    public String getProtocolClass() { return protocolClass; }
    public void setProtocolClass(String protocolClass) { this.protocolClass = protocolClass; }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public String getContainer() { return container; }
    public void setContainer(String container) { this.container = container; }
    public String getVolume() { return volume; }
    public void setVolume(String volume) { this.volume = volume; }
    public String getFasting() { return fasting; }
    public void setFasting(String fasting) { this.fasting = fasting; }

    public int getId() {
        return id;
    }

    public String getNumericCode() { return numericCode; }
    public void setNumericCode(String numericCode) { this.numericCode = numericCode; }
    public String getAlphaCode() { return alphaCode; }
    public void setAlphaCode(String alphaCode) { this.alphaCode = alphaCode; }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public double getPrice() {
        return price;
    }

    public String getResultTime() {
        return resultTime;
    }

    public List<TestParameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<TestParameter> parameters) {
        this.parameters = parameters;
    }

    public void addParameter(TestParameter param) {
        this.parameters.add(param);
    }

    public String getLowercaseName() {
        return lowercaseName;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getGrowthStatus() { return growthStatus; }
    public void setGrowthStatus(String growthStatus) { this.growthStatus = growthStatus; }
    public String getGrowthFindings() { return growthFindings; }
    public void setGrowthFindings(String growthFindings) { this.growthFindings = growthFindings; }

    @Override
    public String toString() {
        if (numericCode != null && !numericCode.isEmpty()) {
            return "[" + numericCode + "] " + (alphaCode != null && !alphaCode.isEmpty() ? alphaCode + " - " : "") + name;
        }
        return name;
    }
}
