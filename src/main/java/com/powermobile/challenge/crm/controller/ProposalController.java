package com.powermobile.challenge.crm.controller;

import com.powermobile.challenge.crm.dto.CreateProposalDTO;
import com.powermobile.challenge.crm.dto.ProposalDTO;
import com.powermobile.challenge.crm.dto.ProposalItemDTO;
import com.powermobile.challenge.crm.dto.SignerDTO;
import com.powermobile.challenge.crm.service.ProposalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/proposal")
public class ProposalController {

    private final ProposalService proposalService;

    public ProposalController(ProposalService proposalService) {
        this.proposalService = proposalService;
    }

    @PostMapping
    public ResponseEntity<ProposalDTO> createProposal(@RequestBody @Valid CreateProposalDTO createProposalDTO) {
        ProposalDTO proposal = proposalService.createProposal(createProposalDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(proposal);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProposalDTO> findProposalById(@PathVariable Long id) {
        return ResponseEntity.ok(proposalService.findById(id));
    }

    @GetMapping
    public ResponseEntity<List<ProposalDTO>> findByClientMail(
            @RequestParam(required = false) String clientEmail) {

        if (clientEmail != null && !clientEmail.isEmpty()) {
            return ResponseEntity.ok(proposalService.findByClientEmail(clientEmail));
        }
        return ResponseEntity.ok(proposalService.findAll());
    }

    @PostMapping("/{id}/items")
    public ResponseEntity<ProposalItemDTO> addItem(
            @PathVariable Long id,
            @RequestBody @Valid ProposalItemDTO itemDTO) {

        ProposalItemDTO createdItem = proposalService.addItem(id, itemDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdItem);
    }

    @PostMapping("/{id}/send")
    public ResponseEntity<Void> send(
            @PathVariable Long id,
            @RequestBody @Valid List<@Valid SignerDTO> signers) {

        proposalService.sendProposal(id, signers);
        return ResponseEntity.ok().build();
    }

}
