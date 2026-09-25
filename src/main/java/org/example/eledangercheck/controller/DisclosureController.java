package org.example.eledangercheck.controller;

import org.example.eledangercheck.entity.Disclosure;
import org.example.eledangercheck.service.DisclosureService;
import org.example.eledangercheck.service.DownloadService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/disclosures")
public class DisclosureController {

    private final DisclosureService disclosureService;
    private final DownloadService downloadService;

    public DisclosureController(DisclosureService disclosureService, DownloadService downloadService) {
        this.disclosureService = disclosureService;
        this.downloadService = downloadService;
    }

    @PostMapping
    public ResponseEntity<Disclosure> createDisclosure(@RequestBody Disclosure disclosure) {
        return ResponseEntity.ok(disclosureService.createDisclosure(disclosure));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Disclosure> getDisclosureById(@PathVariable Long id) {
        return ResponseEntity.ok(disclosureService.getDisclosureById(id));
    }

    @GetMapping("/task/{taskId}")
    public ResponseEntity<Disclosure> getDisclosureByTaskId(@PathVariable Long taskId) {
        return ResponseEntity.ok(disclosureService.getDisclosureByTaskId(taskId));
    }

    @GetMapping
    public ResponseEntity<List<Disclosure>> getAllDisclosures() {
        return ResponseEntity.ok(disclosureService.getAllDisclosures());
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Disclosure>> getDisclosuresByStatus(@PathVariable String status) {
        return ResponseEntity.ok(disclosureService.getDisclosuresByStatus(status));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Disclosure> updateDisclosure(@PathVariable Long id, @RequestBody Disclosure disclosureDetails) {
        return ResponseEntity.ok(disclosureService.updateDisclosure(id, disclosureDetails));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDisclosure(@PathVariable Long id) {
        disclosureService.deleteDisclosure(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/download/word")
    public ResponseEntity<byte[]> downloadWord(@PathVariable Long id) throws IOException {
        Disclosure disclosure = disclosureService.getDisclosureById(id);
        byte[] content = downloadService.generateWord(disclosure);
        
        String filename = URLEncoder.encode(disclosure.getTitle() + ".docx", StandardCharsets.UTF_8);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                .body(content);
    }

    @GetMapping("/{id}/download/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id) throws IOException {
        Disclosure disclosure = disclosureService.getDisclosureById(id);
        byte[] content = downloadService.generatePdf(disclosure);
        
        String filename = URLEncoder.encode(disclosure.getTitle() + ".pdf", StandardCharsets.UTF_8);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CONTENT_TYPE, "application/pdf")
                .body(content);
    }
}