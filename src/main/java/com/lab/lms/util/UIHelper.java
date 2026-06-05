package com.lab.lms.util;

import javafx.scene.image.Image;
import javafx.stage.Stage;

public class UIHelper {
    public static void setAppIcon(Stage stage) {
        try {
            stage.getIcons().add(new Image(UIHelper.class.getResourceAsStream("/images/lab_icon.png")));
        } catch (Exception e) {
            System.err.println("Could not load app icon: " + e.getMessage());
        }
    }

    /**
     * MS WORD STYLE TABLE BUILDER
     * Shows a GUI to create a table with specific rows and columns
     */
    public static void showInteractiveTableBuilder(javafx.stage.Window owner, java.util.function.Consumer<String> onGenerated) {
        // Stage 1: Dimension Picker
        javafx.scene.control.Dialog<int[]> dimDialog = new javafx.scene.control.Dialog<>();
        dimDialog.setTitle("Insert Graphical Table (MS Word Style)");
        dimDialog.setHeaderText("Choose how many rows and columns you need.");
        dimDialog.initOwner(owner);

        javafx.scene.control.ButtonType nextBtn = new javafx.scene.control.ButtonType("NEXT: Fill Table Data", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        dimDialog.getDialogPane().getButtonTypes().addAll(nextBtn, javafx.scene.control.ButtonType.CANCEL);

        javafx.scene.layout.GridPane dimGrid = new javafx.scene.layout.GridPane();
        dimGrid.setHgap(15); dimGrid.setVgap(15); dimGrid.setPadding(new javafx.geometry.Insets(20));
        
        javafx.scene.control.TextField rows = new javafx.scene.control.TextField("3");
        javafx.scene.control.TextField cols = new javafx.scene.control.TextField("3");
        dimGrid.add(new javafx.scene.control.Label("Rows Count:"), 0, 0); dimGrid.add(rows, 1, 0);
        dimGrid.add(new javafx.scene.control.Label("Columns Count:"), 0, 1); dimGrid.add(cols, 1, 1);
        dimDialog.getDialogPane().setContent(dimGrid);

        dimDialog.setResultConverter(bt -> {
            if (bt == nextBtn) {
                try { return new int[]{Integer.parseInt(rows.getText()), Integer.parseInt(cols.getText())}; }
                catch (Exception e) { return null; }
            }
            return null;
        });

        dimDialog.showAndWait().ifPresent(dims -> {
            int r = dims[0], c = dims[1];
            if (r <= 0 || c <= 0) return;

            // Stage 2: Data Entry Grid (THE REAL MS WORD EXPERIENCE)
            javafx.scene.control.Dialog<String> dataDialog = new javafx.scene.control.Dialog<>();
            dataDialog.setTitle("Complete Table Data Entry");
            dataDialog.setHeaderText("Populate the cells below. The first row will be used as Header.");
            dataDialog.initOwner(owner);

            javafx.scene.layout.GridPane tableGrid = new javafx.scene.layout.GridPane();
            tableGrid.setHgap(5); tableGrid.setVgap(5); tableGrid.setPadding(new javafx.geometry.Insets(15));
            tableGrid.setStyle("-fx-background-color: #FDFDFD;");

            javafx.scene.control.TextField[][] cells = new javafx.scene.control.TextField[r][c];
            for (int i = 0; i < r; i++) {
                for (int j = 0; j < c; j++) {
                    cells[i][j] = new javafx.scene.control.TextField();
                    cells[i][j].setPrefWidth(140);
                    if (i == 0) cells[i][j].setStyle("-fx-font-weight: bold; -fx-background-color: #E8F5E9; -fx-prompt-text-fill: green;");
                    else cells[i][j].setStyle("-fx-background-color: white;");
                    tableGrid.add(cells[i][j], j, i);
                }
            }

            javafx.scene.control.ScrollPane sp = new javafx.scene.control.ScrollPane(tableGrid);
            sp.setFitToWidth(true); sp.setPrefHeight(350); sp.setPrefWidth(700);
            sp.setStyle("-fx-background-color: transparent; -fx-border-color: #CFD8DC;");
            
            dataDialog.getDialogPane().setContent(sp);
            javafx.scene.control.ButtonType insertBtn = new javafx.scene.control.ButtonType("Insert into Note", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
            dataDialog.getDialogPane().getButtonTypes().addAll(insertBtn, javafx.scene.control.ButtonType.CANCEL);

            dataDialog.setResultConverter(bt -> {
                if (bt == insertBtn) {
                    double colWidth = 100.0 / c;
                    String widthStr = String.format("%.2f", colWidth) + "%";
                    StringBuilder sb = new StringBuilder("<br/><table border='1' style='border-collapse: collapse; width: 100%; table-layout: fixed; border: 1px solid #CFD8DC;'>");
                    
                    for (int j = 0; j < c; j++) {
                        sb.append("<col style='width: ").append(widthStr).append(";'>");
                    }

                    for (int i = 0; i < r; i++) {
                        sb.append("<tr>");
                        for (int j = 0; j < c; j++) {
                            String txt = cells[i][j].getText().trim();
                            String commonStyle = "padding: 10px; border: 1px solid #CFD8DC; width: " + widthStr + "; word-wrap: break-word; word-break: break-word; vertical-align: top;";
                            if (i == 0) {
                                sb.append("<th style='background-color: #F8F9FA; color: #1A0A0A; ").append(commonStyle).append("'><b>")
                                  .append(txt.isEmpty() ? "&nbsp;" : txt)
                                  .append("</b></th>");
                            } else {
                                sb.append("<td style='background-color: #FFFFFF; color: #333333; ").append(commonStyle).append("'>")
                                  .append(txt.isEmpty() ? "&nbsp;" : txt)
                                  .append("</td>");
                            }
                        }
                        sb.append("</tr>");
                    }
                    sb.append("</table><br/>");
                    return sb.toString();
                }
                return null;
            });

            dataDialog.showAndWait().ifPresent(onGenerated);
        });
    }
}
