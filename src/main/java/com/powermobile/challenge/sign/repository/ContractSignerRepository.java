package com.powermobile.challenge.sign.repository;

import com.powermobile.challenge.sign.domain.ContractSigner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContractSignerRepository extends JpaRepository<ContractSigner, Long> {
    List<ContractSigner> findByContractIdOrderBySigningOrder(Long contractId);
}
