package com.isdemir.isdemirdb.controller;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.isdemir.isdemirdb.entity.Material;
import com.isdemir.isdemirdb.service.MaterialService;

import lombok.RequiredArgsConstructor;

// The business logic lives in MaterialService; the controller only calls that service.
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = { "http://localhost:5173", "http://localhost:5174" })
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService materialService;

    // Lists the distinct material types (for the combobox)
    @GetMapping("/malzeme/turler")
    public List<String> types() {
        return materialService.findTypes();
    }

    // Lists the active materials of the selected type
    @GetMapping("/malzeme")
    public List<Material> list(@RequestParam("tur") String type) {
        return materialService.findByType(type);
    }
}
