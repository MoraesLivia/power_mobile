package com.powermobile.challenge.crm.service;

import com.powermobile.challenge.crm.domain.Client;
import com.powermobile.challenge.crm.dto.CreateProposalDTO;
import com.powermobile.challenge.crm.dto.ProposalDTO;
import com.powermobile.challenge.crm.dto.ProposalItemDTO;
import com.powermobile.challenge.crm.domain.Proposal;
import com.powermobile.challenge.crm.domain.ProposalItem;
import com.powermobile.challenge.crm.dto.SignerDTO;
import com.powermobile.challenge.crm.event.ProposalSentEvent;
import com.powermobile.challenge.crm.repository.ClientRepository;
import com.powermobile.challenge.crm.repository.ProposalRepository;
import com.powermobile.challenge.shared.event.EventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.powermobile.challenge.crm.domain.ProposalStatus.CREATED;
import static com.powermobile.challenge.crm.domain.ProposalStatus.SENT;

@Service
public class ProposalService {

    private final ProposalRepository proposalRepository;
    private final ClientRepository clientRepository;
    private final EventPublisher eventPublisher;

    public ProposalService(ProposalRepository proposalRepository, ClientRepository clientRepository, EventPublisher eventPublisher) {
        this.proposalRepository = proposalRepository;
        this.clientRepository = clientRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public ProposalDTO createProposal(CreateProposalDTO createProposalDTO) {

        Client client = clientRepository.findById(createProposalDTO.getClientId())
                .orElseThrow(() -> new RuntimeException("Client not found with id: " + createProposalDTO.getClientId()));

        Proposal proposal = new Proposal();
        proposal.setClientId(client.getId());
        proposal.setClientName(client.getClientName());
        proposal.setClientEmail(client.getClientEmail());
        proposal.setStatus(CREATED);

        List<ProposalItem> items = createProposalDTO.getItems()
                .stream()
                .map(itemDto -> {
                    ProposalItem item = new ProposalItem();
                    item.setItemName(itemDto.getItemName());
                    item.setItemQuantity(itemDto.getItemQuantity());
                    item.setItemPrice(itemDto.getItemPrice());
                    item.setProposal(proposal);
                    return item;
                })
                .toList();

        proposal.setItems(items);
        Proposal savedProposal = proposalRepository.save(proposal);
        return toDTO(savedProposal);
    }

    public ProposalDTO findById(Long id) {
        Proposal proposal = proposalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Proposal not found"));
        return toDTO(proposal);
    }

    public List<ProposalDTO> findByClientEmail(String email) {
        return proposalRepository.findByClientEmail(email)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<ProposalDTO> findAll() {
        return proposalRepository.findAll()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional
    public ProposalItemDTO addItem(Long proposalId, ProposalItemDTO itemDTO) {

        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new RuntimeException("Proposal not found"));

        if (proposal.getStatus().equals(SENT)) {
            throw new RuntimeException("Cannot add items to a proposal that has already been sent");
        }

        ProposalItem item = new ProposalItem();
        item.setItemName(itemDTO.getItemName());
        item.setItemQuantity(itemDTO.getItemQuantity());
        item.setItemPrice(itemDTO.getItemPrice());
        item.setProposal(proposal);

        proposal.getItems().add(item);
        proposalRepository.save(proposal);

        return new ProposalItemDTO(item.getId(), item.getItemName(), item.getItemQuantity(), item.getItemPrice());
    }

    @Transactional
    public void sendProposal(Long proposalId, List<SignerDTO> signers) {

        Proposal proposal = proposalRepository.findById(proposalId).orElseThrow(() -> new RuntimeException("Proposal not found"));

        if (proposal.getItems().isEmpty()) {
            throw new RuntimeException("Cannot send proposal without items");
        }

        if (proposal.getStatus().equals(SENT)) {
            throw new RuntimeException("Proposal already sent");
        }

        validateSigners(signers);

        proposal.setStatus(SENT);
        proposalRepository.save(proposal);

        eventPublisher.publish(
                "proposal.sent",
                new ProposalSentEvent(
                        proposal.getId(),
                        proposal.getClientName(),
                        proposal.getClientEmail(),
                        proposal.getClientId(),
                        signers
                )
        );
    }

    /**
     * Validates that signers list is non-empty and has sequential orders starting from 1
     * with no gaps and no duplicates, and that all signer emails are unique.
     */
    private void validateSigners(List<SignerDTO> signers) {
        if (signers == null || signers.isEmpty()) {
            throw new RuntimeException("At least one signer is required to send the proposal");
        }

        List<Integer> orders = signers.stream()
                .map(SignerDTO::getSigningOrder)
                .sorted()
                .toList();

        Set<Integer> uniqueOrders = Set.copyOf(orders);
        if (uniqueOrders.size() != orders.size()) {
            throw new RuntimeException("Signer orders must be unique — duplicate signing order found");
        }

        List<Integer> expected = IntStream.rangeClosed(1, orders.size())
                .boxed()
                .toList();
        if (!orders.equals(expected)) {
            throw new RuntimeException("Signer orders must be sequential starting from 1 (e.g. 1, 2, 3). Received: " + orders);
        }

        long uniqueEmails = signers.stream().map(SignerDTO::getEmail).distinct().count();
        if (uniqueEmails != signers.size()) {
            throw new RuntimeException("Signer emails must be unique — duplicate email found");
        }
    }

    private ProposalDTO toDTO(Proposal proposal) {

        List<ProposalItemDTO> items = proposal.getItems() != null ? proposal.getItems().stream().map(item -> new ProposalItemDTO(
                        item.getId(),
                        item.getItemName(),
                        item.getItemQuantity(),
                        item.getItemPrice())).toList()
                :List.of();

        return new ProposalDTO(
                proposal.getId(),
                proposal.getClientId(),
                proposal.getClientName(),
                proposal.getClientEmail(),
                proposal.getStatus(),
                items
        );
    }
}
