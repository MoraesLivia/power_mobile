package com.powermobile.challenge.crm.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProposalItemDTO {
    private Long id;
    
    @NotBlank(message = "Item name is required")
    private String itemName;
    
    @Min(value = 1, message = "Item quantity must be greater than 0")
    private Integer itemQuantity;
    private BigDecimal itemPrice;
}
