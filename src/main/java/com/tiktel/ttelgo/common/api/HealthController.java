package com.tiktel.ttelgo.common.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    @Autowired
    private DataSource dataSource;

    @GetMapping("/db")
    public ResponseEntity<Map<String, Object>> checkDatabase() {
        Map<String, Object> response = new HashMap<>();
        
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            
            response.put("status", "UP");
            response.put("database", metaData.getDatabaseProductName());
            response.put("version", metaData.getDatabaseProductVersion());
            response.put("url", metaData.getURL());
            response.put("username", metaData.getUserName());
            response.put("driverName", metaData.getDriverName());
            response.put("driverVersion", metaData.getDriverVersion());
            response.put("connected", true);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "DOWN");
            response.put("error", e.getMessage());
            response.put("connected", false);
            return ResponseEntity.status(503).body(response);
        }
    }
}

