package org.example.eledangercheck.controller;

import org.example.eledangercheck.entity.Disclosure;
import org.example.eledangercheck.entity.Hazard;
import org.example.eledangercheck.entity.SafetyMeasure;
import org.example.eledangercheck.entity.Task;
import org.example.eledangercheck.service.DocumentService;
import org.example.eledangercheck.service.DisclosureService;
import org.example.eledangercheck.service.HazardService;
import org.example.eledangercheck.service.SafetyMeasureService;
import org.example.eledangercheck.service.TaskService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/document")
public class DocumentController {

    private final DocumentService documentService;
    private final TaskService taskService;
    private final HazardService hazardService;
    private final SafetyMeasureService safetyMeasureService;
    private final DisclosureService disclosureService;

    public DocumentController(DocumentService documentService, TaskService taskService,
                             HazardService hazardService, SafetyMeasureService safetyMeasureService,
                             DisclosureService disclosureService) {
        this.documentService = documentService;
        this.taskService = taskService;
        this.hazardService = hazardService;
        this.safetyMeasureService = safetyMeasureService;
        this.disclosureService = disclosureService;
    }

    @GetMapping("/disclosure/{disclosureId}/word")
    public ResponseEntity<byte[]> downloadWordDisclosure(@PathVariable Long disclosureId) throws IOException {
        Disclosure disclosure = disclosureService.getDisclosureById(disclosureId);
        Task task = taskService.getTaskById(disclosure.getTaskId());
        List<Hazard> hazards = hazardService.getHazardsByTaskId(disclosure.getTaskId());
        List<SafetyMeasure> measures = safetyMeasureService.getMeasuresByTaskId(disclosure.getTaskId());

        byte[] content = documentService.generateWordDisclosureCard(disclosure, task, hazards, measures);

        String fileName = "交底卡_" + disclosure.getDisclosureNo() + ".docx";
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
        headers.setContentDispositionFormData("attachment", encodedFileName);
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName);

        return ResponseEntity.ok()
                .headers(headers)
                .body(content);
    }

    @GetMapping("/disclosure/{disclosureId}/pdf")
    public ResponseEntity<byte[]> downloadPdfDisclosure(@PathVariable Long disclosureId) throws IOException {
        Disclosure disclosure = disclosureService.getDisclosureById(disclosureId);
        Task task = taskService.getTaskById(disclosure.getTaskId());
        List<Hazard> hazards = hazardService.getHazardsByTaskId(disclosure.getTaskId());
        List<SafetyMeasure> measures = safetyMeasureService.getMeasuresByTaskId(disclosure.getTaskId());

        byte[] content = documentService.generatePdfDisclosureCard(disclosure, task, hazards, measures);

        String fileName = "交底卡_" + disclosure.getDisclosureNo() + ".pdf";
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", encodedFileName);
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName);

        return ResponseEntity.ok()
                .headers(headers)
                .body(content);
    }

    @PostMapping("/disclosure/{disclosureId}/save-word")
    public ResponseEntity<String> saveWordDisclosure(@PathVariable Long disclosureId) throws IOException {
        Disclosure disclosure = disclosureService.getDisclosureById(disclosureId);
        Task task = taskService.getTaskById(disclosure.getTaskId());
        List<Hazard> hazards = hazardService.getHazardsByTaskId(disclosure.getTaskId());
        List<SafetyMeasure> measures = safetyMeasureService.getMeasuresByTaskId(disclosure.getTaskId());

        String filePath = documentService.saveWordDisclosureCard(disclosure, task, hazards, measures);

        return ResponseEntity.ok("Word 文件已保存：" + filePath);
    }

    @PostMapping("/disclosure/{disclosureId}/save-pdf")
    public ResponseEntity<String> savePdfDisclosure(@PathVariable Long disclosureId) throws IOException {
        Disclosure disclosure = disclosureService.getDisclosureById(disclosureId);
        Task task = taskService.getTaskById(disclosure.getTaskId());
        List<Hazard> hazards = hazardService.getHazardsByTaskId(disclosure.getTaskId());
        List<SafetyMeasure> measures = safetyMeasureService.getMeasuresByTaskId(disclosure.getTaskId());

        String filePath = documentService.savePdfDisclosureCard(disclosure, task, hazards, measures);

        return ResponseEntity.ok("PDF 文件已保存：" + filePath);
    }
}