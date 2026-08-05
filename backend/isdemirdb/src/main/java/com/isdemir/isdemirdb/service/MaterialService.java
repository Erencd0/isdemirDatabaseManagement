package com.isdemir.isdemirdb.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.isdemir.isdemirdb.entity.Material;
import com.isdemir.isdemirdb.repository.MaterialRepository;

import lombok.RequiredArgsConstructor;

// Material listing logic lives in this layer. The controller only calls this service.
@Service
@RequiredArgsConstructor
public class MaterialService {

    private final MaterialRepository materialRepository;

    // Lists the distinct material types (for the combobox)
    public List<String> findTypes() {
        return materialRepository.findTypes();
    }

    // Lists the active materials of the selected type
    public List<Material> findByType(String type) {
        return materialRepository.findByTypeAndActiveTrue(type);
    }
}
