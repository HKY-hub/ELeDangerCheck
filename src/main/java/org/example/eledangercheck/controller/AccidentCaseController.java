package org.example.eledangercheck.controller;

import org.example.eledangercheck.entity.AccidentCase;
import org.example.eledangercheck.service.AccidentCaseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accident-cases")
public class AccidentCaseController {

    private final AccidentCaseService accidentCaseService;

    public AccidentCaseController(AccidentCaseService accidentCaseService) {
        this.accidentCaseService = accidentCaseService;
    }

    @PostMapping
    public ResponseEntity<AccidentCase> createAccidentCase(@RequestBody AccidentCase accidentCase) {
        return ResponseEntity.ok(accidentCaseService.createAccidentCase(accidentCase));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccidentCase> getAccidentCaseById(@PathVariable Long id) {
        return ResponseEntity.ok(accidentCaseService.getAccidentCaseById(id));
    }

    @GetMapping
    public ResponseEntity<List<AccidentCase>> getAllAccidentCases() {
        return ResponseEntity.ok(accidentCaseService.getAllAccidentCases());
    }

    @GetMapping("/type/{accidentType}")
    public ResponseEntity<List<AccidentCase>> getCasesByType(@PathVariable String accidentType) {
        return ResponseEntity.ok(accidentCaseService.getCasesByType(accidentType));
    }

    @GetMapping("/work-type/{workType}")
    public ResponseEntity<List<AccidentCase>> getCasesByWorkType(@PathVariable String workType) {
        return ResponseEntity.ok(accidentCaseService.getCasesByWorkType(workType));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AccidentCase> updateAccidentCase(@PathVariable Long id, @RequestBody AccidentCase caseDetails) {
        return ResponseEntity.ok(accidentCaseService.updateAccidentCase(id, caseDetails));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccidentCase(@PathVariable Long id) {
        accidentCaseService.deleteAccidentCase(id);
        return ResponseEntity.noContent().build();
    }
}