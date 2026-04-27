package com.powermobile.challenge.crm.event;

import com.powermobile.challenge.crm.dto.SignerDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class ProposalSentEvent {

    private Long proposalId;
    private String clientName;
    private String clientEmail;
    private Long clientId;
    private List<SignerDTO> signers;
}
