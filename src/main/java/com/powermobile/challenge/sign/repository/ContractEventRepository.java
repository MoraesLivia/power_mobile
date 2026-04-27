package com.powermobile.challenge.sign.repository;

import com.powermobile.challenge.sign.domain.ContractEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractEventRepository extends JpaRepository<ContractEvent, Long> {
}
