package com.isdemir.isdemirdb.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.isdemir.isdemirdb.entity.Operator;
import com.isdemir.isdemirdb.service.OperatorService;

import lombok.RequiredArgsConstructor;

// The business logic lives in OperatorService; the controller only calls that service.
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class OperatorController {

    private final OperatorService operatorService;

    // Lists the active operators (the combobox next to Döküm Zamanı)
    @GetMapping("/operator")
    public List<Operator> list() {
        return operatorService.findActive();
    }
}
