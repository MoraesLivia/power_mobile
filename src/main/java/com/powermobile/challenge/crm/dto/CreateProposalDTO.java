package com.powermobile.challenge.crm.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class CreateProposalDTO {

    @NotNull(message = "Client ID is required")
    private Long clientId;

    @NotNull(message = "Items are required")
    @Size(min = 1, message = "Proposal must have at least 1 item")
    private List<@Valid ProposalItemDTO> items;
}

