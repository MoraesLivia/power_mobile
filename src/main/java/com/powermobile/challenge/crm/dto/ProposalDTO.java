package com.powermobile.challenge.crm.dto;

import com.powermobile.challenge.crm.domain.ProposalStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProposalDTO {
    private Long id;
    private Long clientId;
    private String clientName;
    private String clientEmail;
    private ProposalStatus status;
    private List<ProposalItemDTO> items;
}
