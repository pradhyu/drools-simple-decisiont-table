package com.example.drools.controller;

import com.example.drools.model.Audit;
import com.example.drools.model.Person;
import com.example.drools.service.RulesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rules")
public class RulesController {

    private final RulesService rulesService;

    @Autowired
    public RulesController(RulesService rulesService) {
        this.rulesService = rulesService;
    }

    @PostMapping
    public Audit checkRules(@RequestBody com.example.drools.model.EvaluationRequest request) {
        return rulesService.executeRules(request);
    }
}
