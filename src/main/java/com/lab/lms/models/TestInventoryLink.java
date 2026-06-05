package com.lab.lms.models;

import javafx.beans.property.*;

public class TestInventoryLink {
    private final IntegerProperty id;
    private final IntegerProperty testId;
    private final IntegerProperty inventoryId;
    private final StringProperty itemName;
    private final DoubleProperty usageQuantity;
    private final StringProperty unit;

    public TestInventoryLink(int id, int testId, int inventoryId, String itemName, double usageQuantity, String unit) {
        this.id = new SimpleIntegerProperty(id);
        this.testId = new SimpleIntegerProperty(testId);
        this.inventoryId = new SimpleIntegerProperty(inventoryId);
        this.itemName = new SimpleStringProperty(itemName);
        this.usageQuantity = new SimpleDoubleProperty(usageQuantity);
        this.unit = new SimpleStringProperty(unit);
    }

    public int getId() { return id.get(); }
    public IntegerProperty idProperty() { return id; }
    public void setId(int id) { this.id.set(id); }

    public int getTestId() { return testId.get(); }
    public IntegerProperty testIdProperty() { return testId; }
    public void setTestId(int testId) { this.testId.set(testId); }

    public int getInventoryId() { return inventoryId.get(); }
    public IntegerProperty inventoryIdProperty() { return inventoryId; }
    public void setInventoryId(int inventoryId) { this.inventoryId.set(inventoryId); }

    public String getItemName() { return itemName.get(); }
    public StringProperty itemNameProperty() { return itemName; }
    public void setItemName(String itemName) { this.itemName.set(itemName); }

    public double getUsageQuantity() { return usageQuantity.get(); }
    public DoubleProperty usageQuantityProperty() { return usageQuantity; }
    public void setUsageQuantity(double usageQuantity) { this.usageQuantity.set(usageQuantity); }

    public String getUnit() { return unit.get(); }
    public StringProperty unitProperty() { return unit; }
    public void setUnit(String unit) { this.unit.set(unit); }
}
