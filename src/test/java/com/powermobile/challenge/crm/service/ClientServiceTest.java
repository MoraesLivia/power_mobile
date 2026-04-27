package com.powermobile.challenge.crm.service;

import com.powermobile.challenge.crm.domain.Client;
import com.powermobile.challenge.crm.dto.ClientDTO;
import com.powermobile.challenge.crm.repository.ClientRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private ClientService clientService;

    @Test
    void createClient_shouldSaveAndReturnDto() {
        ClientDTO input = new ClientDTO(null, "Larissa", "larissa@email.com");

        when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> {
            Client client = invocation.getArgument(0);
            client.setId(10L);
            return client;
        });

        ClientDTO result = clientService.createClient(input);

        ArgumentCaptor<Client> captor = ArgumentCaptor.forClass(Client.class);
        verify(clientRepository).save(captor.capture());

        Client savedClient = captor.getValue();
        assertEquals("Larissa", savedClient.getClientName());
        assertEquals("larissa@email.com", savedClient.getClientEmail());

        assertEquals(10L, result.getId());
        assertEquals("Larissa", result.getClientName());
        assertEquals("larissa@email.com", result.getClientEmail());
    }
}

