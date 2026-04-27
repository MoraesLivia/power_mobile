package com.powermobile.challenge.sign.service;

import com.powermobile.challenge.sign.domain.Contract;
import com.powermobile.challenge.sign.domain.ContractEvent;
import com.powermobile.challenge.sign.domain.ContractSigner;
import com.powermobile.challenge.sign.dto.ContractDTO;
import com.powermobile.challenge.sign.dto.ContractParticipantDTO;
import com.powermobile.challenge.crm.dto.SignerDTO;
import com.powermobile.challenge.sign.repository.ContractEventRepository;
import com.powermobile.challenge.sign.repository.ContractRepository;
import com.powermobile.challenge.sign.repository.ContractSignerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ContractService {

    private final ContractRepository contractRepository;
    private final ContractEventRepository eventRepository;
    private final ContractSignerRepository signerRepository;

    public ContractService(ContractRepository contractRepository, ContractEventRepository eventRepository, ContractSignerRepository signerRepository) {
        this.contractRepository = contractRepository;
        this.eventRepository = eventRepository;
        this.signerRepository = signerRepository;
    }

    @Transactional
    public void createFromProposal(Long proposalId,
                                   String clientName,
                                   String clientEmail,
                                   List<SignerDTO> signersDTO) {

        if (contractRepository.existsByProposalId(proposalId)) {
            throw new RuntimeException("Contract already exists for proposal id: " + proposalId);
        }

        if (signersDTO == null || signersDTO.isEmpty()) {
            throw new RuntimeException("At least one signer is required to create a contract");
        }

        Contract contract = new Contract();
        contract.setProposalId(proposalId);
        contract.setContent(buildSimulatedContent(proposalId, clientName, clientEmail, signersDTO));
        contract.setClientName(clientName);
        contract.setClientEmail(clientEmail);
        contract.setStatus("PENDING");

        Contract savedContract = contractRepository.save(contract);

        List<ContractSigner> signers = signersDTO.stream()
                .map(dto -> {
                    ContractSigner signer = new ContractSigner();
                    signer.setContractId(savedContract.getId());
                    signer.setEmail(dto.getEmail());
                    signer.setSigningOrder(dto.getSigningOrder());
                    signer.setStatus("PENDING");
                    return signer;
                })
                .toList();

        signerRepository.saveAll(signers);

        audit("CONTRACT_CREATED", savedContract.getId(), clientEmail);
    }

    private String buildSimulatedContent(Long proposalId,
                                         String clientName,
                                         String clientEmail,
                                         List<SignerDTO> signers) {
        String participantsJson = signers.stream()
                .sorted((a, b) -> Integer.compare(a.getSigningOrder(), b.getSigningOrder()))
                .map(signer -> String.format("{\"email\":\"%s\",\"order\":%d}", signer.getEmail(), signer.getSigningOrder()))
                .collect(Collectors.joining(","));

        return String.format(
                "{\"type\":\"SIMULATED_CONTRACT_JSON\",\"proposalId\":%d,\"clientName\":\"%s\",\"clientEmail\":\"%s\",\"participants\":[%s]}",
                proposalId,
                clientName,
                clientEmail,
                participantsJson
        );
    }

    @Transactional
    public void sign(Long contractId, String email) {

        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contract not found"));

        if ("SIGNED".equals(contract.getStatus())) {
            throw new RuntimeException("Contract is already fully signed");
        }
        if ("REJECTED".equals(contract.getStatus())) {
            throw new RuntimeException("Contract has been rejected and cannot be signed");
        }

        List<ContractSigner> signers = signerRepository.findByContractIdOrderBySigningOrder(contractId);

        ContractSigner current = signers.stream()
                .filter(s -> "PENDING".equals(s.getStatus()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No pending signer found for this contract"));

        if (!current.getEmail().equalsIgnoreCase(email)) {
            throw new RuntimeException("Not your turn to sign. Current expected signer: " + current.getEmail());
        }

        current.setStatus("SIGNED");
        signerRepository.save(current);

        audit("SIGNED", contractId, email);

        boolean allSigned = signers.stream()
                .filter(s -> !s.getId().equals(current.getId()))
                .allMatch(s -> "SIGNED".equals(s.getStatus()));

        if (allSigned) {
            contract.setStatus("SIGNED");
            contractRepository.save(contract);
            audit("CONTRACT_FULLY_SIGNED", contractId, email);
        }
    }

    @Transactional
    public void reject(Long contractId, String email) {

        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contract not found"));

        if ("SIGNED".equals(contract.getStatus())) {
            throw new RuntimeException("Contract is already fully signed and cannot be rejected");
        }
        if ("REJECTED".equals(contract.getStatus())) {
            throw new RuntimeException("Contract has already been rejected");
        }

        ContractSigner current = getCurrentSigner(contractId);

        if (!current.getEmail().equalsIgnoreCase(email)) {
            throw new RuntimeException("Not your turn. Current expected signer: " + current.getEmail());
        }

        current.setStatus("REJECTED");
        signerRepository.save(current);

        contract.setStatus("REJECTED");
        contractRepository.save(contract);

        audit("REJECTED", contractId, email);
    }

    public List<ContractDTO> findAll() {
        return contractRepository.findAll()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public ContractDTO findById(Long id) {
        Contract contract = contractRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contract not found"));
        return toDTO(contract);
    }

    private ContractSigner getCurrentSigner(Long contractId) {
        return signerRepository
                .findByContractIdOrderBySigningOrder(contractId)
                .stream()
                .filter(s -> "PENDING".equals(s.getStatus()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No pending signer found for this contract"));
    }

    private void audit(String type, Long contractId, String email) {
        ContractEvent event = new ContractEvent();
        event.setContractId(contractId);
        event.setType(type);
        event.setDescription("User " + email + " performed " + type);
        event.setCreatedAt(LocalDateTime.now());
        eventRepository.save(event);
    }


    private ContractDTO toDTO(Contract contract) {
        List<ContractParticipantDTO> participants = signerRepository
                .findByContractIdOrderBySigningOrder(contract.getId())
                .stream()
                .map(signer -> new ContractParticipantDTO(
                        signer.getEmail(),
                        signer.getSigningOrder(),
                        signer.getStatus()))
                .collect(Collectors.toList());

        return new ContractDTO(
                contract.getId(),
                contract.getProposalId(),
                contract.getContent(),
                contract.getClientName(),
                contract.getClientEmail(),
                contract.getStatus(),
                participants
        );
    }
}
