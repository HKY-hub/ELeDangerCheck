package org.example.eledangercheck.controller;

import org.example.eledangercheck.service.ImageAnalysisService;
import org.example.eledangercheck.service.OpenCvDetectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/vision")
public class ImageAnalysisController {

    private final ImageAnalysisService imageAnalysisService;
    private final OpenCvDetectionService openCvDetectionService;

    public ImageAnalysisController(ImageAnalysisService imageAnalysisService, OpenCvDetectionService openCvDetectionService) {
        this.imageAnalysisService = imageAnalysisService;
        this.openCvDetectionService = openCvDetectionService;
    }

    @PostMapping("/analyze-scene")
    public ResponseEntity<Map<String, Object>> analyzeScene(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = imageAnalysisService.analyzeSceneImage(file);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/preprocess")
    public ResponseEntity<Map<String, Object>> preprocess(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = imageAnalysisService.preprocessWithOpenCV(file);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/detect")
    public ResponseEntity<Map<String, Object>> detect(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = openCvDetectionService.detectAll(file);
        return ResponseEntity.ok(result);
    }
}
