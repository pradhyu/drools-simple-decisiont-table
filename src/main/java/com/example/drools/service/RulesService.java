package com.example.drools.service;

import com.example.drools.model.Audit;
import com.example.drools.model.Claim;
import com.example.drools.model.EvaluationRequest;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RulesService {

    private final KieContainer kieContainer;

    @Autowired
    public RulesService(KieContainer kieContainer) {
        this.kieContainer = kieContainer;
    }

    public Audit executeRules(EvaluationRequest request) {
        KieSession kieSession = kieContainer.newKieSession();
        Audit audit = new Audit();

        if (request.getPerson() != null) {
            kieSession.insert(request.getPerson());
        }

        if (request.getClaims() != null) {
            for (Claim claim : request.getClaims()) {
                kieSession.insert(claim);
            }
        }

        kieSession.insert(audit);

        kieSession.fireAllRules();
        kieSession.dispose();

        return audit;
    }
}
