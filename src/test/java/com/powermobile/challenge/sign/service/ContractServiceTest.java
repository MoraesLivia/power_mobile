package com.powermobile.challenge.sign.service;

import com.powermobile.challenge.crm.dto.SignerDTO;
import com.powermobile.challenge.sign.domain.Contract;
import com.powermobile.challenge.sign.domain.ContractEvent;
import com.powermobile.challenge.sign.domain.ContractSigner;
import com.powermobile.challenge.sign.dto.ContractDTO;
import com.powermobile.challenge.sign.repository.ContractEventRepository;
import com.powermobile.challenge.sign.repository.ContractRepository;
import com.powermobile.challenge.sign.repository.ContractSignerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContractServiceTest {

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private ContractEventRepository eventRepository;

    @Mock
    private ContractSignerRepository signerRepository;

    @InjectMocks
    private ContractService contractService;

    private Contract contract;

    @BeforeEach
    void setUp() {
        contract = new Contract();
        contract.setId(7L);
        contract.setProposalId(2L);
        contract.setClientName("CArla");
        contract.setClientEmail("carla@email.com");
        contract.setStatus("PENDING");
        contract.setContent("{}");
    }

    @Test
    void createFromProposal_shouldThrowWhenContractAlreadyExists() {
        when(contractRepository.existsByProposalId(2L)).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                contractService.createFromProposal(2L, "Larissa", "larissa@email.com", List.of(new SignerDTO("a@email.com", 1))));

        assertTrue(ex.getMessage().contains("already exists"));
        verify(contractRepository, never()).save(any(Contract.class));
    }

    @Test
    void createFromProposal_shouldSaveContractSignersAndAudit() {
        when(contractRepository.existsByProposalId(2L)).thenReturn(false);
        when(contractRepository.save(any(Contract.class))).thenAnswer(invocation -> {
            Contract saved = invocation.getArgument(0);
            saved.setId(7L);
            return saved;
        });

        List<SignerDTO> signers = List.of(
                new SignerDTO("second@email.com", 2),
                new SignerDTO("first@email.com", 1)
        );

        contractService.createFromProposal(2L, "Larissa", "larissa@email.com", signers);

        ArgumentCaptor<Contract> contractCaptor = ArgumentCaptor.forClass(Contract.class);
        verify(contractRepository).save(contractCaptor.capture());

        Contract savedContract = contractCaptor.getValue();
        assertEquals("PENDING", savedContract.getStatus());
        assertTrue(savedContract.getContent().contains("\"proposalId\":2"));
        assertTrue(savedContract.getContent().contains("\"participants\""));

        verify(signerRepository).saveAll(org.mockito.ArgumentMatchers.<ContractSigner>anyList());

        ArgumentCaptor<ContractEvent> eventCaptor = ArgumentCaptor.forClass(ContractEvent.class);
        verify(eventRepository).save(eventCaptor.capture());
        assertEquals("CONTRACT_CREATED", eventCaptor.getValue().getType());
    }

    @Test
    void sign_shouldThrowWhenOutOfOrderSignerTriesToSign() {
        when(contractRepository.findById(7L)).thenReturn(Optional.of(contract));

        ContractSigner pendingSigner = signer(101L, 7L, "expected@email.com", 1, "PENDING");
        when(signerRepository.findByContractIdOrderBySigningOrder(7L)).thenReturn(List.of(pendingSigner));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> contractService.sign(7L, "other@email.com"));

        assertTrue(ex.getMessage().contains("Not your turn"));
        verify(signerRepository, never()).save(any(ContractSigner.class));
    }

    @Test
    void sign_shouldMarkContractAsSignedWhenLastSignerSigns() {
        when(contractRepository.findById(7L)).thenReturn(Optional.of(contract));

        ContractSigner firstSigner = signer(1L, 7L, "first@email.com", 1, "SIGNED");
        ContractSigner secondSigner = signer(2L, 7L, "second@email.com", 2, "PENDING");

        when(signerRepository.findByContractIdOrderBySigningOrder(7L)).thenReturn(List.of(firstSigner, secondSigner));

        contractService.sign(7L, "second@email.com");

        assertEquals("SIGNED", secondSigner.getStatus());
        assertEquals("SIGNED", contract.getStatus());
        verify(signerRepository).save(secondSigner);
        verify(contractRepository).save(contract);
        verify(eventRepository, times(2)).save(any(ContractEvent.class));
    }

    @Test
    void reject_shouldMarkContractAsRejectedAndAudit() {
        when(contractRepository.findById(7L)).thenReturn(Optional.of(contract));

        ContractSigner current = signer(12L, 7L, "first@email.com", 1, "PENDING");
        when(signerRepository.findByContractIdOrderBySigningOrder(7L)).thenReturn(List.of(current));

        contractService.reject(7L, "first@email.com");

        assertEquals("REJECTED", current.getStatus());
        assertEquals("REJECTED", contract.getStatus());
        verify(signerRepository).save(current);
        verify(contractRepository).save(contract);

        ArgumentCaptor<ContractEvent> eventCaptor = ArgumentCaptor.forClass(ContractEvent.class);
        verify(eventRepository).save(eventCaptor.capture());
        assertEquals("REJECTED", eventCaptor.getValue().getType());
    }

    @Test
    void findById_shouldReturnContractWithParticipantsOrdered() {
        when(contractRepository.findById(7L)).thenReturn(Optional.of(contract));

        ContractSigner first = signer(1L, 7L, "first@email.com", 1, "SIGNED");
        ContractSigner second = signer(2L, 7L, "second@email.com", 2, "PENDING");
        when(signerRepository.findByContractIdOrderBySigningOrder(7L)).thenReturn(List.of(first, second));

        ContractDTO dto = contractService.findById(7L);

        assertEquals(7L, dto.getId());
        assertEquals(2, dto.getParticipants().size());
        assertEquals("first@email.com", dto.getParticipants().get(0).getEmail());
        assertEquals(1, dto.getParticipants().get(0).getSigningOrder());
    }

    private ContractSigner signer(Long id, Long contractId, String email, Integer signingOrder, String status) {
        ContractSigner signer = new ContractSigner();
        signer.setId(id);
        signer.setContractId(contractId);
        signer.setEmail(email);
        signer.setSigningOrder(signingOrder);
        signer.setStatus(status);
        return signer;
    }
}

