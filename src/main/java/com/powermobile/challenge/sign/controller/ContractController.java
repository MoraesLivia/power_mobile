package com.powermobile.challenge.sign.controller;

import com.powermobile.challenge.sign.dto.ContractDTO;
import com.powermobile.challenge.sign.service.ContractService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Contract", description = "Contract signing APIs")
@RestController
@RequestMapping("/contracts")
public class ContractController {

    private final ContractService contractService;

    public ContractController(ContractService contractService) {
        this.contractService = contractService;
    }

    @Operation(summary = "Sign contract — must follow signing order")
    @PostMapping("/{id}/sign")
    public ResponseEntity<Void> sign(
            @PathVariable Long id,
            @Parameter(description = "Email of the signer (must match current turn)") @RequestParam String email) {

        contractService.sign(id, email);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Reject contract — blocks the entire contract")
    @PostMapping("/{id}/reject")
    public ResponseEntity<Void> reject(
            @PathVariable Long id,
            @Parameter(description = "Email of the signer (must match current turn)") @RequestParam String email) {

        contractService.reject(id, email);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "List all contracts")
    @GetMapping
    public ResponseEntity<List<ContractDTO>> findAll() {
        return ResponseEntity.ok(contractService.findAll());
    }

    @Operation(summary = "Find contract by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ContractDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(contractService.findById(id));
    }
}