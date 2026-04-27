package com.powermobile.challenge.sign.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContractParticipantDTO {

    private String email;
    private Integer signingOrder;
    private String status;
}
