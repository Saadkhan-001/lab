package com.lab.lms.services;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class NavigationService {
    private static StackPane contentArea;
    private static final Map<String, ViewData> viewCache = new HashMap<>();

    public static class ViewData {
        public final Node node;
        public final Object controller;
        public ViewData(Node node, Object controller) {
            this.node = node;
            this.controller = controller;
        }
    }

    public static void setContentArea(StackPane area) {
        contentArea = area;
    }

    public static void switchView(String fxmlPath) {
        try {
            // High-Performance Dynamic Reload: Bypass cache for switchView to ensure fresh data
            FXMLLoader loader = new FXMLLoader(NavigationService.class.getResource(fxmlPath));
            Node node = loader.load();
            if (contentArea != null && node != null) {
                contentArea.getChildren().setAll(node);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Node getView(String fxmlPath) throws IOException {
        // Clinical Data Integrity: Always load fresh FXML and controllers
        FXMLLoader loader = new FXMLLoader(NavigationService.class.getResource(fxmlPath));
        Node node = loader.load();
        Object controller = loader.getController();
        
        // Maintain a weak reference for getController if needed, but for now we prioritize fresh loads
        viewCache.put(fxmlPath, new ViewData(node, controller));
        
        return node;
    }

    public static void preLoad(String fxmlPath) {
        javafx.application.Platform.runLater(() -> {
            try {
                getView(fxmlPath);
            } catch (IOException e) {
                System.err.println("Pre-load failed for: " + fxmlPath);
            }
        });
    }

    public static Object getController(String fxmlPath) {
        ViewData data = viewCache.get(fxmlPath);
        return (data != null) ? data.controller : null;
    }

    public static void clearCache() {
        viewCache.clear();
    }
}
