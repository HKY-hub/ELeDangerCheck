package org.example.eledangercheck.controller;

import org.example.eledangercheck.config.SimulationConfig;
import org.example.eledangercheck.service.TtsService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.Map;

@RestController
@RequestMapping("/api/tts")
public class TtsController {

    private final TtsService ttsService;
    private final SimulationConfig simulationConfig;

    public TtsController(TtsService ttsService, SimulationConfig simulationConfig) {
        this.ttsService = ttsService;
        this.simulationConfig = simulationConfig;
    }

    @PostMapping("/synthesize")
    public ResponseEntity<Map<String, Object>> synthesize(@RequestBody Map<String, String> request) {
        String text = request.get("text");
        Map<String, Object> result = ttsService.synthesize(text);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/speak")
    public ResponseEntity<byte[]> speak(@RequestBody Map<String, String> request) {
        String text = request.get("text");
        
        byte[] audioData = generateMockAudioData(text);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "audio/wav")
                .body(audioData);
    }

    private byte[] generateMockAudioData(String text) {
        int sampleRate = 8000;
        int durationMs = Math.min(text.length() * 200, 5000);
        int samples = sampleRate * durationMs / 1000;
        byte[] data = new byte[samples * 2];
        
        for (int i = 0; i < samples; i++) {
            double t = (double) i / sampleRate;
            double amp = 0.3 * (1 - t / (durationMs / 1000.0));
            double wave = amp * Math.sin(2 * Math.PI * 440 * t) 
                       + amp * 0.5 * Math.sin(2 * Math.PI * 880 * t);
            short sample = (short) (wave * Short.MAX_VALUE);
            data[i * 2] = (byte) (sample & 0xff);
            data[i * 2 + 1] = (byte) ((sample >> 8) & 0xff);
        }
        
        return data;
    }
}