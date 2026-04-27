package com.powermobile.challenge.sign.repository;

import com.powermobile.challenge.sign.domain.Contract;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractRepository extends JpaRepository<Contract, Long> {
    boolean existsByProposalId(Long proposalId);
}
