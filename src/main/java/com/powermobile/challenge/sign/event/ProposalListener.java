package com.powermobile.challenge.sign.event;

import com.powermobile.challenge.crm.event.ProposalSentEvent;
import com.powermobile.challenge.shared.event.EventListener;
import com.powermobile.challenge.sign.service.ContractService;
import org.springframework.stereotype.Component;

@Component
public class ProposalListener implements EventListener {

    private final ContractService contractService;

    public ProposalListener(ContractService contractService) {
        this.contractService = contractService;
    }

    @Override
    public void handle(Object event) {

         if (event instanceof ProposalSentEvent e) {
             contractService.createFromProposal(
                     e.getProposalId(),
                     e.getClientName(),
                     e.getClientEmail(),
                     e.getSigners()
             );
         }
    }
}
