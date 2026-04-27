package com.powermobile.challenge.sign.dto;

import com.fasterxml.jackson.annotation.JsonRawValue;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContractDTO {

    private Long id;
    private Long proposalId;

    @JsonRawValue
    private String content;

    private String clientName;
    private String clientEmail;
    private String status;
    private List<ContractParticipantDTO> participants;

}
