package com.powermobile.challenge.crm.repository;

import com.powermobile.challenge.crm.domain.Proposal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProposalRepository extends JpaRepository<Proposal, Long> {
    List<Proposal> findByClientEmail(String clientEmail);
}
