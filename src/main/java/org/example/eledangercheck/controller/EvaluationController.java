package org.example.eledangercheck.controller;

import org.example.eledangercheck.entity.Evaluation;
import org.example.eledangercheck.service.EvaluationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/evaluation")
public class EvaluationController {

    private final EvaluationService evaluationService;

    public EvaluationController(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @PostMapping("/disclosure/{disclosureId}")
    public ResponseEntity<Evaluation> evaluate(@PathVariable Long disclosureId) {
        Evaluation evaluation = evaluationService.evaluate(disclosureId);
        return ResponseEntity.ok(evaluation);
    }

    @GetMapping("/disclosure/{disclosureId}")
    public ResponseEntity<Evaluation> getEvaluationByDisclosure(@PathVariable Long disclosureId) {
        Evaluation evaluation = evaluationService.getEvaluationByDisclosureId(disclosureId);
        if (evaluation == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(evaluation);
    }

    @GetMapping
    public ResponseEntity<List<Evaluation>> getAllEvaluations() {
        List<Evaluation> evaluations = evaluationService.getAllEvaluations();
        return ResponseEntity.ok(evaluations);
    }
}