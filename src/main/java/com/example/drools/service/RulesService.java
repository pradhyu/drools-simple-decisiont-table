package com.example.drools.service;

import com.example.drools.model.Audit;
import com.example.drools.model.Person;
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

    public Audit executeRules(Person person) {
        KieSession kieSession = kieContainer.newKieSession();
        Audit audit = new Audit();

        kieSession.insert(person);
        kieSession.insert(audit);

        kieSession.fireAllRules();
        kieSession.dispose();

        return audit;
    }
}
