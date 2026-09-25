package org.example.eledangercheck.controller;

import org.example.eledangercheck.entity.SafetyMeasure;
import org.example.eledangercheck.service.SafetyMeasureService;
import org.example.eledangercheck.exception.BusinessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/safety-measures")
public class SafetyMeasureController {

    private final SafetyMeasureService safetyMeasureService;

    public SafetyMeasureController(SafetyMeasureService safetyMeasureService) {
        this.safetyMeasureService = safetyMeasureService;
    }

    @PostMapping
    public ResponseEntity<SafetyMeasure> createSafetyMeasure(@RequestBody SafetyMeasure measure) {
        return ResponseEntity.ok(safetyMeasureService.createSafetyMeasure(measure));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SafetyMeasure> getSafetyMeasureById(@PathVariable Long id) {
        SafetyMeasure measure = safetyMeasureService.getSafetyMeasureById(id);
        if (measure == null) {
            throw new BusinessException("安全措施不存在");
        }
        return ResponseEntity.ok(measure);
    }

    @GetMapping("/hazard/{hazardId}")
    public ResponseEntity<List<SafetyMeasure>> getMeasuresByHazardId(@PathVariable Long hazardId) {
        return ResponseEntity.ok(safetyMeasureService.getMeasuresByHazardId(hazardId));
    }

    @GetMapping("/task/{taskId}")
    public ResponseEntity<List<SafetyMeasure>> getMeasuresByTaskId(@PathVariable Long taskId) {
        return ResponseEntity.ok(safetyMeasureService.getMeasuresByTaskId(taskId));
    }

    @GetMapping
    public ResponseEntity<List<SafetyMeasure>> getAllMeasures() {
        return ResponseEntity.ok(safetyMeasureService.getAllMeasures());
    }

    @PutMapping("/{id}")
    public ResponseEntity<SafetyMeasure> updateSafetyMeasure(@PathVariable Long id, @RequestBody SafetyMeasure measureDetails) {
        return ResponseEntity.ok(safetyMeasureService.updateSafetyMeasure(id, measureDetails));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSafetyMeasure(@PathVariable Long id) {
        safetyMeasureService.deleteSafetyMeasure(id);
        return ResponseEntity.noContent().build();
    }
}