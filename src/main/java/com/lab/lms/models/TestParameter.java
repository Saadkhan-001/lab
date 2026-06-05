package com.lab.lms.models;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import java.util.ArrayList;
import java.util.List;

public class TestParameter {
    private int id;
    private int testId;
    private StringProperty name = new SimpleStringProperty("");
    private StringProperty unit = new SimpleStringProperty("");
    private StringProperty category = new SimpleStringProperty("");
    private StringProperty nameOverride = new SimpleStringProperty(null);
    private StringProperty unitOverride = new SimpleStringProperty(null);
    private StringProperty rangeOverride = new SimpleStringProperty(null);
    
    // Lists to support multiple ranges per category
    private List<String> minRanges = new ArrayList<>();
    private List<String> maxRanges = new ArrayList<>();
    
    private List<String> minRangesMale = new ArrayList<>();
    private List<String> maxRangesMale = new ArrayList<>();
    
    private List<String> minRangesFemale = new ArrayList<>();
    private List<String> maxRangesFemale = new ArrayList<>();
    
    private List<String> minRangesKids = new ArrayList<>();
    private List<String> maxRangesKids = new ArrayList<>();
    
    private StringProperty value = new SimpleStringProperty(""); 
    private javafx.beans.property.BooleanProperty selected = new javafx.beans.property.SimpleBooleanProperty(false);
    private javafx.beans.property.IntegerProperty selectionOrder = new javafx.beans.property.SimpleIntegerProperty(0);

    public TestParameter(int id, int testId, String name, String unit) {
        this.id = id;
        this.testId = testId;
        this.name.set(name);
        this.unit.set(unit);
        this.category.set("");
    }

    public TestParameter(int id, int testId, String name, String unit, String min, String max) {
        this(id, testId, name, unit);
        parseAndFill(minRanges, min);
        parseAndFill(maxRanges, max);
    }

    public TestParameter(int id, int testId, String name, String unit, String min, String max,
                         String minM, String maxM, String minF, String maxF, String minK, String maxK) {
        this(id, testId, name, unit, min, max);
        parseAndFill(minRangesMale, minM);
        parseAndFill(maxRangesMale, maxM);
        parseAndFill(minRangesFemale, minF);
        parseAndFill(maxRangesFemale, maxF);
        parseAndFill(minRangesKids, minK);
        parseAndFill(maxRangesKids, maxK);
    }

    public TestParameter(int id, int testId, String name, String unit, String min, String max,
                         String minM, String maxM, String minF, String maxF, String minK, String maxK, String secondary) {
        this(id, testId, name, unit, min, max, minM, maxM, minF, maxF, minK, maxK);
        // Compatibility for old secondaryRangesArea
        if (secondary != null && !secondary.isEmpty()) {
            for (String p : secondary.split("\n")) {
                if (!p.trim().isEmpty()) minRanges.add(p.trim());
            }
        }
    }

    private void parseAndFill(List<String> list, String data) {
        if (data == null || data.isEmpty()) return;
        String[] parts = data.split("\n");
        for (String p : parts) {
            list.add(p);
        }
    }

    // Getters and Setters
    public int getId() { return id; }
    public int getTestId() { return testId; }
    public String getName() { return (nameOverride.get() != null) ? nameOverride.get() : name.get(); }
    public StringProperty nameProperty() { return name; }
    public String getUnit() { return (unitOverride.get() != null) ? unitOverride.get() : unit.get(); }
    public StringProperty unitProperty() { return unit; }

    public String getNameOverride() { return nameOverride.get(); }
    public StringProperty nameOverrideProperty() { return nameOverride; }
    public void setNameOverride(String v) { this.nameOverride.set(v); }

    public String getUnitOverride() { return unitOverride.get(); }
    public StringProperty unitOverrideProperty() { return unitOverride; }
    public void setUnitOverride(String v) { this.unitOverride.set(v); }

    public String getRangeOverride() { return rangeOverride.get(); }
    public StringProperty rangeOverrideProperty() { return rangeOverride; }
    public void setRangeOverride(String v) { this.rangeOverride.set(v); }

    public List<String> getMinRanges() { return minRanges; }
    public List<String> getMaxRanges() { return maxRanges; }
    public List<String> getMinRangesMale() { return minRangesMale; }
    public List<String> getMaxRangesMale() { return maxRangesMale; }
    public List<String> getMinRangesFemale() { return minRangesFemale; }
    public List<String> getMaxRangesFemale() { return maxRangesFemale; }
    public List<String> getMinRangesKids() { return minRangesKids; }
    public List<String> getMaxRangesKids() { return maxRangesKids; }

    // Compatibility getters for single value
    public String getMinRange() { return minRanges.isEmpty() ? "" : minRanges.get(0); }
    public String getMaxRange() { return maxRanges.isEmpty() ? "" : maxRanges.get(0); }
    public String getMinRangeMale() { return minRangesMale.isEmpty() ? "" : minRangesMale.get(0); }
    public String getMaxRangeMale() { return maxRangesMale.isEmpty() ? "" : maxRangesMale.get(0); }
    public String getMinRangeFemale() { return minRangesFemale.isEmpty() ? "" : minRangesFemale.get(0); }
    public String getMaxRangeFemale() { return maxRangesFemale.isEmpty() ? "" : maxRangesFemale.get(0); }
    public String getMinRangeKids() { return minRangesKids.isEmpty() ? "" : minRangesKids.get(0); }
    public String getMaxRangeKids() { return maxRangesKids.isEmpty() ? "" : maxRangesKids.get(0); }

    public void setMinRange(String v) { updateList(minRanges, v); }
    public void setMaxRange(String v) { updateList(maxRanges, v); }
    public void setMinRangeMale(String v) { updateList(minRangesMale, v); }
    public void setMaxRangeMale(String v) { updateList(maxRangesMale, v); }
    public void setMinRangeFemale(String v) { updateList(minRangesFemale, v); }
    public void setMaxRangeFemale(String v) { updateList(maxRangesFemale, v); }
    public void setMinRangeKids(String v) { updateList(minRangesKids, v); }
    public void setMaxRangeKids(String v) { updateList(maxRangesKids, v); }

    public String getSecondaryRanges() { 
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < minRanges.size(); i++) {
            sb.append(minRanges.get(i)).append("\n");
        }
        return sb.toString().trim();
    }
    public void setSecondaryRanges(String v) {
        while(minRanges.size() > 1) minRanges.remove(1);
        if (v != null && !v.isEmpty()) {
            for (String p : v.split("\n")) {
                if (!p.trim().isEmpty()) minRanges.add(p.trim());
            }
        }
    }

    private void updateList(List<String> list, String val) {
        if (list.isEmpty()) {
            if (val != null) list.add(val);
        } else {
            list.set(0, val != null ? val : "");
        }
    }

    public void setName(String name) { this.name.set(name); }
    public void setUnit(String unit) { this.unit.set(unit); }
    public String getCategory() { return category.get() == null ? "" : category.get(); }
    public StringProperty categoryProperty() { return category; }
    public void setCategory(String category) { this.category.set(category == null ? "" : category); }

    public String getValue() { return value.get(); }
    public void setValue(String val) { this.value.set(val); }

    public javafx.beans.property.BooleanProperty selectedProperty() { return selected; }
    public boolean isSelected() { return selected.get(); }
    public void setSelected(boolean sel) { this.selected.set(sel); }

    public javafx.beans.property.IntegerProperty selectionOrderProperty() { return selectionOrder; }
    public int getSelectionOrder() { return selectionOrder.get(); }
    public void setSelectionOrder(int order) { this.selectionOrder.set(order); }

    public StringProperty valueProperty() { return value; }

    public String getRange() { return getRange(null, null); }
    public String getRange(String gender, String ageStr) {
        if (rangeOverride.get() != null && !rangeOverride.get().isEmpty()) {
            return rangeOverride.get();
        }
        List<String> targetMins = minRanges;
        List<String> targetMaxs = maxRanges;

        if (ageStr != null && !ageStr.isEmpty()) {
            try {
                double age = Double.parseDouble(ageStr.replaceAll("[^0-9.]", ""));
                // Clinic logic: treat exactly 0 (testing/unassigned) as regular
                if (age < 10 && age > 0.001 && !minRangesKids.isEmpty()) {
                    targetMins = minRangesKids;
                    targetMaxs = maxRangesKids;
                } else if (gender != null) {
                    if (gender.toUpperCase().contains("MALE") && !gender.toUpperCase().contains("FEMALE") && !minRangesMale.isEmpty()) {
                        targetMins = minRangesMale;
                        targetMaxs = maxRangesMale;
                    } else if (gender.toUpperCase().contains("FEMALE") && !minRangesFemale.isEmpty()) {
                        targetMins = minRangesFemale;
                        targetMaxs = maxRangesFemale;
                    }
                }
            } catch (Exception e) {}
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < targetMins.size(); i++) {
            String mn = targetMins.get(i);
            String mx = (i < targetMaxs.size()) ? targetMaxs.get(i) : "";
            
            if (mn == null || mn.isEmpty()) {
                if (mx != null && !mx.isEmpty()) sb.append(mx);
                else if (i == 0 && targetMins.size() == 1) sb.append("-"); 
            } else if (mx == null || mx.isEmpty()) {
                sb.append(mn);
            } else {
                sb.append(mn).append(" - ").append(mx);
            }
            if (i < targetMins.size() - 1) sb.append("\n");
        }

        String res = sb.toString().trim();
        return res.isEmpty() ? "-" : res;
    }

    public String getDisclosure() {
        StringBuilder sb = new StringBuilder();
        appendRanges(sb, "", minRanges, maxRanges, true); // Primary range: show even if empty
        appendRanges(sb, "Kids: ", minRangesKids, maxRangesKids, false);
        appendRanges(sb, "Male: ", minRangesMale, maxRangesMale, false);
        appendRanges(sb, "Female: ", minRangesFemale, maxRangesFemale, false);
        String d = sb.toString().trim();
        return d.isEmpty() ? "-" : d;
    }

    private void appendRanges(StringBuilder sb, String prefix, List<String> mins, List<String> maxs, boolean isPrimary) {
        for (int i = 0; i < mins.size(); i++) {
            String mn = mins.get(i);
            String mx = (i < maxs.size()) ? maxs.get(i) : "";
            
            // Intelligence Layer: Skip ranges that are effectively empty/null for non-primary demographic categories
            boolean hasMin = mn != null && !mn.isEmpty() && !mn.equals("-") && !mn.equals("0");
            boolean hasMax = mx != null && !mx.isEmpty() && !mx.equals("-") && !mx.equals("0");
            
            if (isPrimary || hasMin || hasMax) {
                sb.append(prefix);
                if (mn != null && !mn.isEmpty()) sb.append(mn);
                if (mx != null && !mx.isEmpty()) {
                    if (mn != null && !mn.isEmpty()) sb.append(" - ");
                    sb.append(mx);
                }
                sb.append("\n");
            }
        }
    }

    public boolean isAbnormal() { return isAbnormal(null, null); }
    public boolean isAbnormal(String gender, String ageStr) {
        String valText = getValue();
        if (valText == null || valText.isEmpty()) return false;

        List<String> targetMins = minRanges;
        List<String> targetMaxs = maxRanges;

        if (ageStr != null && !ageStr.isEmpty()) {
            try {
                double age = Double.parseDouble(ageStr.replaceAll("[^0-9.]", ""));
                if (age < 10 && age > 0.001 && !minRangesKids.isEmpty()) {
                    targetMins = minRangesKids;
                    targetMaxs = maxRangesKids;
                } else if (gender != null) {
                    if (gender.toUpperCase().contains("MALE") && !gender.toUpperCase().contains("FEMALE") && !minRangesMale.isEmpty()) {
                        targetMins = minRangesMale;
                        targetMaxs = maxRangesMale;
                    } else if (gender.toUpperCase().contains("FEMALE") && !minRangesFemale.isEmpty()) {
                        targetMins = minRangesFemale;
                        targetMaxs = maxRangesFemale;
                    }
                }
            } catch (Exception e) {}
        }

        boolean anyNumeric = false;
        boolean withinNumerical = false;

        for (int i = 0; i < targetMins.size(); i++) {
            String tm = targetMins.get(i);
            String tx = (i < targetMaxs.size()) ? targetMaxs.get(i) : "";
            
            try {
                double val = Double.parseDouble(valText);
                double min = Double.parseDouble(tm);
                double max = Double.parseDouble(tx);
                anyNumeric = true;
                if (val >= min && val <= max) {
                    withinNumerical = true;
                    break;
                }
            } catch (Exception e) {
                String v = valText.trim().toUpperCase();
                String mmin = (tm != null) ? tm.trim().toUpperCase() : "";
                String mmax = (tx != null) ? tx.trim().toUpperCase() : "";
                
                if (!mmin.isEmpty() && v.equals(mmin)) return false;
                if (!mmax.isEmpty() && v.equals(mmax)) return false;
                
                if ((mmin.contains("NEGATIVE") || mmax.contains("NEGATIVE")) && 
                    (v.contains("POSITIVE") || v.contains("REACTIVE"))) return true;
            }
        }

        if (anyNumeric) return !withinNumerical;
        return false;
    }

    public String getAbnormalFlag() { return getAbnormalFlag(null, null); }
    public String getAbnormalFlag(String gender, String ageStr) {
        String valText = getValue();
        if (valText == null || valText.isEmpty()) return "";

        List<String> targetMins = minRanges;
        List<String> targetMaxs = maxRanges;

        if (ageStr != null && !ageStr.isEmpty()) {
            try {
                double age = Double.parseDouble(ageStr.replaceAll("[^0-9.]", ""));
                if (age < 10 && age > 0.001 && !minRangesKids.isEmpty()) {
                    targetMins = minRangesKids;
                    targetMaxs = maxRangesKids;
                } else if (gender != null) {
                    if (gender.toUpperCase().contains("MALE") && !gender.toUpperCase().contains("FEMALE") && !minRangesMale.isEmpty()) {
                        targetMins = minRangesMale;
                        targetMaxs = maxRangesMale;
                    } else if (gender.toUpperCase().contains("FEMALE") && !minRangesFemale.isEmpty()) {
                        targetMins = minRangesFemale;
                        targetMaxs = maxRangesFemale;
                    }
                }
            } catch (Exception e) {}
        }

        for (int i = 0; i < targetMins.size(); i++) {
            String tm = targetMins.get(i);
            String tx = (i < targetMaxs.size()) ? targetMaxs.get(i) : "";
            
            try {
                double val = Double.parseDouble(valText);
                double min = Double.parseDouble(tm);
                double max = Double.parseDouble(tx);
                if (val < min) return "L";
                if (val > max) return "H";
            } catch (Exception e) {}
        }

        if (isAbnormal(gender, ageStr)) return "H";
        return "";
    }
}
