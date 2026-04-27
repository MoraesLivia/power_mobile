package com.powermobile.challenge.sign.controller;

import com.powermobile.challenge.sign.dto.ContractDTO;
import com.powermobile.challenge.sign.service.ContractService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/contracts")
public class ContractController {

    private final ContractService contractService;

    public ContractController(ContractService contractService) {
        this.contractService = contractService;
    }

    @PostMapping("/{id}/sign")
    public ResponseEntity<Void> sign(
            @PathVariable Long id,
            @RequestParam String email) {

        contractService.sign(id, email);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Void> reject(
            @PathVariable Long id,
            @RequestParam String email) {

        contractService.reject(id, email);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<ContractDTO>> findAll() {
        return ResponseEntity.ok(contractService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContractDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(contractService.findById(id));
    }
}