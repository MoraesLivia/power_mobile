package com.powermobile.challenge.crm.service;

import com.powermobile.challenge.crm.domain.Client;
import com.powermobile.challenge.crm.domain.Proposal;
import com.powermobile.challenge.crm.domain.ProposalItem;
import com.powermobile.challenge.crm.domain.ProposalStatus;
import com.powermobile.challenge.crm.dto.CreateProposalDTO;
import com.powermobile.challenge.crm.dto.ProposalDTO;
import com.powermobile.challenge.crm.dto.ProposalItemDTO;
import com.powermobile.challenge.crm.dto.SignerDTO;
import com.powermobile.challenge.crm.event.ProposalSentEvent;
import com.powermobile.challenge.crm.repository.ClientRepository;
import com.powermobile.challenge.crm.repository.ProposalRepository;
import com.powermobile.challenge.shared.event.EventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProposalServiceTest {

    @Mock
    private ProposalRepository proposalRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private ProposalService proposalService;

    private Client client;

    @BeforeEach
    void setUp() {
        client = new Client();
        client.setId(10L);
        client.setClientName("Larissa");
        client.setClientEmail("larissa@email.com");
    }

    @Test
    void createProposal_shouldCreateProposalWithClientData() {
        CreateProposalDTO dto = new CreateProposalDTO();
        dto.setClientId(10L);
        dto.setItems(List.of(new ProposalItemDTO(null, "Plan A", 1, new BigDecimal("199.90"))));

        when(clientRepository.findById(10L)).thenReturn(Optional.of(client));
        when(proposalRepository.save(any(Proposal.class))).thenAnswer(invocation -> {
            Proposal proposal = invocation.getArgument(0);
            proposal.setId(1L);
            return proposal;
        });

        ProposalDTO result = proposalService.createProposal(dto);

        assertEquals(1L, result.getId());
        assertEquals(10L, result.getClientId());
        assertEquals("Larissa", result.getClientName());
        assertEquals(ProposalStatus.CREATED, result.getStatus());
        assertEquals(1, result.getItems().size());
        verify(proposalRepository).save(any(Proposal.class));
    }

    @Test
    void createProposal_shouldThrowWhenClientNotFound() {
        CreateProposalDTO dto = new CreateProposalDTO();
        dto.setClientId(999L);
        dto.setItems(List.of(new ProposalItemDTO(null, "Plan A", 1, BigDecimal.TEN)));

        when(clientRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> proposalService.createProposal(dto));

        assertTrue(ex.getMessage().contains("Client not found"));
        verify(proposalRepository, never()).save(any(Proposal.class));
    }

    @Test
    void addItem_shouldThrowWhenProposalAlreadySent() {
        Proposal proposal = new Proposal();
        proposal.setId(1L);
        proposal.setStatus(ProposalStatus.SENT);
        proposal.setItems(new ArrayList<>());

        when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> proposalService.addItem(1L, new ProposalItemDTO(null, "Item", 1, BigDecimal.ONE)));

        assertTrue(ex.getMessage().contains("already been sent"));
    }

    @Test
    void sendProposal_shouldThrowWhenNoItems() {
        Proposal proposal = new Proposal();
        proposal.setId(1L);
        proposal.setClientId(10L);
        proposal.setClientName("Larissa");
        proposal.setClientEmail("larissa@email.com");
        proposal.setStatus(ProposalStatus.CREATED);
        proposal.setItems(new ArrayList<>());

        when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> proposalService.sendProposal(1L, List.of(new SignerDTO("a@email.com", 1))));

        assertTrue(ex.getMessage().contains("without items"));
        verify(eventPublisher, never()).publish(any(), any());
    }

    @Test
    void sendProposal_shouldThrowWhenSignerOrderIsNotSequential() {
        Proposal proposal = buildReadyToSendProposal();
        when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));

        List<SignerDTO> signers = List.of(
                new SignerDTO("first@email.com", 1),
                new SignerDTO("third@email.com", 3)
        );

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> proposalService.sendProposal(1L, signers));

        assertTrue(ex.getMessage().contains("sequential"));
        verify(eventPublisher, never()).publish(any(), any());
    }

    @Test
    void sendProposal_shouldThrowWhenSignerEmailIsDuplicated() {
        Proposal proposal = buildReadyToSendProposal();
        when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));

        List<SignerDTO> signers = List.of(
                new SignerDTO("same@email.com", 1),
                new SignerDTO("same@email.com", 2)
        );

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> proposalService.sendProposal(1L, signers));

        assertTrue(ex.getMessage().contains("emails must be unique"));
        verify(eventPublisher, never()).publish(any(), any());
    }

    @Test
    void sendProposal_shouldUpdateStatusAndPublishEvent() {
        Proposal proposal = buildReadyToSendProposal();
        when(proposalRepository.findById(1L)).thenReturn(Optional.of(proposal));
        when(proposalRepository.save(any(Proposal.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<SignerDTO> signers = List.of(
                new SignerDTO("first@email.com", 1),
                new SignerDTO("second@email.com", 2)
        );

        proposalService.sendProposal(1L, signers);

        assertEquals(ProposalStatus.SENT, proposal.getStatus());
        verify(proposalRepository).save(proposal);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publish(eq("proposal.sent"), eventCaptor.capture());

        ProposalSentEvent publishedEvent = (ProposalSentEvent) eventCaptor.getValue();
        assertEquals(1L, publishedEvent.getProposalId());
        assertEquals(2, publishedEvent.getSigners().size());
    }

    private Proposal buildReadyToSendProposal() {
        Proposal proposal = new Proposal();
        proposal.setId(1L);
        proposal.setClientId(10L);
        proposal.setClientName("Larissa");
        proposal.setClientEmail("larissa@email.com");
        proposal.setStatus(ProposalStatus.CREATED);

        ProposalItem item = new ProposalItem();
        item.setItemName("Plan A");
        item.setItemQuantity(1);
        item.setItemPrice(new BigDecimal("199.90"));
        item.setProposal(proposal);

        proposal.setItems(new ArrayList<>(List.of(item)));
        return proposal;
    }
}

