package com.lab.lms.models;

import javafx.beans.property.*;

public class InventoryItem {
    private final IntegerProperty id;
    private final StringProperty itemName;
    private final DoubleProperty quantity;
    private final StringProperty unit;
    private final DoubleProperty minStockLevel;
    private final StringProperty category;

    public InventoryItem(int id, String itemName, double quantity, String unit, double minStockLevel, String category) {
        this.id = new SimpleIntegerProperty(id);
        this.itemName = new SimpleStringProperty(itemName);
        this.quantity = new SimpleDoubleProperty(quantity);
        this.unit = new SimpleStringProperty(unit);
        this.minStockLevel = new SimpleDoubleProperty(minStockLevel);
        this.category = new SimpleStringProperty(category);
    }

    public int getId() { return id.get(); }
    public IntegerProperty idProperty() { return id; }
    public void setId(int id) { this.id.set(id); }

    public String getItemName() { return itemName.get(); }
    public StringProperty itemNameProperty() { return itemName; }
    public void setItemName(String itemName) { this.itemName.set(itemName); }

    public double getQuantity() { return quantity.get(); }
    public DoubleProperty quantityProperty() { return quantity; }
    public void setQuantity(double quantity) { this.quantity.set(quantity); }

    public String getUnit() { return unit.get(); }
    public StringProperty unitProperty() { return unit; }
    public void setUnit(String unit) { this.unit.set(unit); }

    public double getMinStockLevel() { return minStockLevel.get(); }
    public DoubleProperty minStockLevelProperty() { return minStockLevel; }
    public void setMinStockLevel(double minStockLevel) { this.minStockLevel.set(minStockLevel); }

    public String getCategory() { return category.get(); }
    public StringProperty categoryProperty() { return category; }
    public void setCategory(String category) { this.category.set(category); }

    public String getStatus() {
        if (getQuantity() <= 0) return "OUT OF STOCK";
        if (getQuantity() <= getMinStockLevel()) return "LOW STOCK";
        return "AVAILABLE";
    }

    @Override
    public String toString() {
        return getItemName() + " (" + getQuantity() + " " + getUnit() + ")";
    }
}
