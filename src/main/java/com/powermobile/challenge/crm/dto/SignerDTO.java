package com.powermobile.challenge.crm.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class SignerDTO {

    @NotBlank(message = "Signer email is required")
    @Email(message = "Signer email must be valid")
    private String email;

    @NotNull(message = "Signing order is required")
    @Min(value = 1, message = "Signing order must be greater than or equal to 1")
    private Integer signingOrder;
}
