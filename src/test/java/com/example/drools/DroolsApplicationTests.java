package com.example.drools;

import com.example.drools.model.Audit;
import com.example.drools.model.Claim;
import com.example.drools.model.EvaluationRequest;
import com.example.drools.model.Person;
import com.example.drools.service.RulesService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DroolsApplicationTests {

    @Autowired
    private RulesService rulesService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Test
    void testAdultMaleRule() {
        Person p = new Person("John", 30, "Male");
        EvaluationRequest request = new EvaluationRequest(p, Collections.emptyList());
        Audit audit = rulesService.executeRules(request);

        assertTrue(audit.getAudits().contains("Adult Male detected"), "Should detect Adult Male");
    }

    @Test
    void testSeniorFemaleRule() {
        Person p = new Person("Jane", 70, "Female");
        EvaluationRequest request = new EvaluationRequest(p, Collections.emptyList());
        Audit audit = rulesService.executeRules(request);

        assertTrue(audit.getAudits().contains("Senior Female detected"), "Should detect Senior Female");
    }

    @Test
    void testMinorRule() {
        Person p = new Person("Kid", 10, "Male");
        EvaluationRequest request = new EvaluationRequest(p, Collections.emptyList());
        Audit audit = rulesService.executeRules(request);

        assertTrue(audit.getAudits().contains("Minor detected"), "Should detect Minor");
    }

    @Test
    void testHighValueClaimRule() {
        Person p = new Person("Patient", 30, "Male");
        // High Value Medical Claim (> 5000)
        Claim c1 = new Claim("C001", 6000.0, "Medical");
        // Normal Dental Claim (< 1000)
        Claim c2 = new Claim("C002", 500.0, "Dental");

        EvaluationRequest request = new EvaluationRequest(p, List.of(c1, c2));
        Audit audit = rulesService.executeRules(request);

        assertTrue(audit.getAudits().contains("Adult Male detected"), "Should still detect Adult Male");
        assertTrue(audit.getAudits().contains("High Value Medical Claim"), "Should detect High Value Medical Claim");
    }

    @Test
    void testRulesEndpoint() throws Exception {
        Person p = new Person("ControllerTest", 40, "Female");
        Claim c = new Claim("C003", 2000.0, "Vision"); // High Value Vision (> 500)

        EvaluationRequest request = new EvaluationRequest(p, List.of(c));

        mockMvc.perform(post("/api/rules")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.audits[?(@ == 'Adult Female detected')]").exists())
                .andExpect(jsonPath("$.audits[?(@ == 'High Value Vision Claim')]").exists());
    }
}
