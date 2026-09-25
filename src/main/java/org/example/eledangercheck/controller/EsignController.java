package org.example.eledangercheck.controller;

import org.example.eledangercheck.service.EsignService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/esign")
public class EsignController {

    private final EsignService esignService;

    public EsignController(EsignService esignService) {
        this.esignService = esignService;
    }

    @PostMapping("/sign")
    public ResponseEntity<Map<String, Object>> sign(@RequestBody Map<String, String> request) {
        String disclosureId = request.get("disclosureId");
        String userId = request.get("userId");
        String signImageBase64 = request.get("signImageBase64");
        Map<String, Object> result = esignService.createSignature(disclosureId, userId, signImageBase64);
        return ResponseEntity.ok(result);
    }
}