package com.powermobile.challenge.crm.service;

import com.powermobile.challenge.crm.domain.Client;
import com.powermobile.challenge.crm.dto.ClientDTO;
import com.powermobile.challenge.crm.repository.ClientRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClientService {

    private final ClientRepository clientRepository;

    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    public ClientDTO createClient(ClientDTO clientDTO) {

        Client client = new Client();
        client.setClientName(clientDTO.getClientName());
        client.setClientEmail(clientDTO.getClientEmail());

        Client saved = clientRepository.save(client);

        return toDTO(saved);
    }

    public ClientDTO findById(Long id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Client not found"));
        return toDTO(client);
    }

    public List<ClientDTO> findAll() {
        return clientRepository.findAll()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    private ClientDTO toDTO(Client client) {
        return new ClientDTO(
                client.getId(),
                client.getClientName(),
                client.getClientEmail()
        );
    }
}