package com.lab.lms.models;

import javafx.beans.property.*;

public class PendingSample {
    private final BooleanProperty selected = new SimpleBooleanProperty(false);
    private final StringProperty sampleId = new SimpleStringProperty();
    private final StringProperty testNames = new SimpleStringProperty();
    private final StringProperty specimenInfo = new SimpleStringProperty();

    private final StringProperty status = new SimpleStringProperty();

    public PendingSample(String sampleId, String testNames, String specimenInfo, String status) {
        this.sampleId.set(sampleId);
        this.testNames.set(testNames);
        this.specimenInfo.set(specimenInfo);
        this.status.set(status);
    }

    public BooleanProperty selectedProperty() { return selected; }
    public boolean isSelected() { return selected.get(); }
    public void setSelected(boolean selected) { this.selected.set(selected); }

    public StringProperty sampleIdProperty() { return sampleId; }
    public String getSampleId() { return sampleId.get(); }

    public StringProperty testNamesProperty() { return testNames; }
    public String getTestNames() { return testNames.get(); }

    public StringProperty statusProperty() { return status; }
    public String getStatus() { return status.get(); }

    public StringProperty specimenInfoProperty() { return specimenInfo; }
    public String getSpecimenInfo() { return specimenInfo.get(); }
}
