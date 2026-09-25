package org.example.eledangercheck.controller;

import org.example.eledangercheck.entity.HazardDict;
import org.example.eledangercheck.entity.MeasureDict;
import org.example.eledangercheck.service.DictService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dict")
public class DictController {

    private final DictService dictService;

    public DictController(DictService dictService) {
        this.dictService = dictService;
    }

    @PostMapping("/hazard")
    public ResponseEntity<HazardDict> createHazardDict(@RequestBody HazardDict hazardDict) {
        return ResponseEntity.ok(dictService.createHazardDict(hazardDict));
    }

    @GetMapping("/hazard/{id}")
    public ResponseEntity<HazardDict> getHazardDictById(@PathVariable Long id) {
        return ResponseEntity.ok(dictService.getHazardDictById(id));
    }

    @GetMapping("/hazard/code/{code}")
    public ResponseEntity<HazardDict> getHazardDictByCode(@PathVariable String code) {
        return ResponseEntity.ok(dictService.getHazardDictByCode(code));
    }

    @GetMapping("/hazard")
    public ResponseEntity<List<HazardDict>> getAllHazardDicts() {
        return ResponseEntity.ok(dictService.getAllHazardDicts());
    }

    @GetMapping("/hazard/level/{level}")
    public ResponseEntity<List<HazardDict>> getHazardDictsByLevel(@PathVariable String level) {
        return ResponseEntity.ok(dictService.getHazardDictsByLevel(level));
    }

    @DeleteMapping("/hazard/{id}")
    public ResponseEntity<Void> deleteHazardDict(@PathVariable Long id) {
        dictService.deleteHazardDict(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/hazard/{id}")
    public ResponseEntity<HazardDict> updateHazardDict(@PathVariable Long id, @RequestBody HazardDict hazardDict) {
        return ResponseEntity.ok(dictService.updateHazardDict(id, hazardDict));
    }

    @PostMapping("/measure")
    public ResponseEntity<MeasureDict> createMeasureDict(@RequestBody MeasureDict measureDict) {
        return ResponseEntity.ok(dictService.createMeasureDict(measureDict));
    }

    @GetMapping("/measure/{id}")
    public ResponseEntity<MeasureDict> getMeasureDictById(@PathVariable Long id) {
        return ResponseEntity.ok(dictService.getMeasureDictById(id));
    }

    @GetMapping("/measure/hazard/{hazardCode}")
    public ResponseEntity<List<MeasureDict>> getMeasuresByHazardCode(@PathVariable String hazardCode) {
        return ResponseEntity.ok(dictService.getMeasuresByHazardCode(hazardCode));
    }

    @GetMapping("/measure")
    public ResponseEntity<List<MeasureDict>> getAllMeasureDicts() {
        return ResponseEntity.ok(dictService.getAllMeasureDicts());
    }

    @DeleteMapping("/measure/{id}")
    public ResponseEntity<Void> deleteMeasureDict(@PathVariable Long id) {
        dictService.deleteMeasureDict(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/measure/{id}")
    public ResponseEntity<MeasureDict> updateMeasureDict(@PathVariable Long id, @RequestBody MeasureDict measureDict) {
        return ResponseEntity.ok(dictService.updateMeasureDict(id, measureDict));
    }
}