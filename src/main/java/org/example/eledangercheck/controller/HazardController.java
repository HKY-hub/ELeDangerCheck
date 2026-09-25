package org.example.eledangercheck.controller;

import org.example.eledangercheck.entity.Hazard;
import org.example.eledangercheck.service.HazardService;
import org.example.eledangercheck.exception.BusinessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hazards")
public class HazardController {

    private final HazardService hazardService;

    public HazardController(HazardService hazardService) {
        this.hazardService = hazardService;
    }

    @PostMapping
    public ResponseEntity<Hazard> createHazard(@RequestBody Hazard hazard) {
        return ResponseEntity.ok(hazardService.createHazard(hazard));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Hazard> getHazardById(@PathVariable Long id) {
        Hazard hazard = hazardService.getHazardById(id);
        if (hazard == null) {
            throw new BusinessException("危险点不存在");
        }
        return ResponseEntity.ok(hazard);
    }

    @GetMapping("/task/{taskId}")
    public ResponseEntity<List<Hazard>> getHazardsByTaskId(@PathVariable Long taskId) {
        return ResponseEntity.ok(hazardService.getHazardsByTaskId(taskId));
    }

    @GetMapping
    public ResponseEntity<List<Hazard>> getAllHazards() {
        return ResponseEntity.ok(hazardService.getAllHazards());
    }

    @GetMapping("/level/{level}")
    public ResponseEntity<List<Hazard>> getHazardsByLevel(@PathVariable String level) {
        return ResponseEntity.ok(hazardService.getHazardsByLevel(level));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Hazard> updateHazard(@PathVariable Long id, @RequestBody Hazard hazardDetails) {
        return ResponseEntity.ok(hazardService.updateHazard(id, hazardDetails));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHazard(@PathVariable Long id) {
        hazardService.deleteHazard(id);
        return ResponseEntity.noContent().build();
    }
}