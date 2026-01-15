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
    void testScreeningRule() {
        // Person age 55 should trigger both Mammogram (40+) and Colonoscopy (50+)
        // screenings
        Person p = new Person("ElderlyPatient", 55, "Female");
        EvaluationRequest request = new EvaluationRequest(p, Collections.emptyList());
        Audit audit = rulesService.executeRules(request);

        assertTrue(audit.getAudits().contains("Screening Recommended: Mammogram required+"),
                "Should recommend Mammogram");
        assertTrue(audit.getAudits().contains("Screening Recommended: Colonoscopy required+"),
                "Should recommend Colonoscopy");
    }

    @Test
    void testAdjudicationRule() {
        // Case 1: Minor (10), Dental (2000) -> High-cost Minor Dental
        Person p1 = new Person("MinorPatient", 10, "Male");
        Claim c1 = new Claim("C004", 2000.0, "Dental");
        Audit audit1 = rulesService.executeRules(new EvaluationRequest(p1, List.of(c1)));
        assertTrue(audit1.getAudits().contains("Adjudication: REVIEW REQUIRED: High-cost Minor Dental"));

        // Case 2: Senior (75), Medical (5000) -> APPROVED Senior Medical
        Person p2 = new Person("SeniorPatient", 75, "Female");
        Claim c2 = new Claim("C005", 5000.0, "Medical");
        Audit audit2 = rulesService.executeRules(new EvaluationRequest(p2, List.of(c2)));
        assertTrue(audit2.getAudits().contains("Adjudication: APPROVED: Senior Medical within limit"));

        // Case 3: Any (40), Vision (3000) -> DENIED Vision limit
        Person p3 = new Person("AnyPatient", 40, "Male");
        Claim c3 = new Claim("C006", 3000.0, "Vision");
        Audit audit3 = rulesService.executeRules(new EvaluationRequest(p3, List.of(c3)));
        assertTrue(audit3.getAudits().contains("Adjudication: DENIED: Vision benefit limit exceeded"));
    }

    @Test
    void testAdvancedFeaturesRule() {
        // Regex match with "TestUser" and type exclusion
        Person p = new Person("TestUser001", 30, "Male");
        Claim c = new Claim("C007", 100.0, "Vision"); // Vision is not in ("Medical", "Dental")
        Audit audit = rulesService.executeRules(new EvaluationRequest(p, List.of(c)));

        assertTrue(audit.getAudits().contains("Special Case: Test user with non-clinical claim"),
                "Should match regex and exclusion logic");
    }

    @Test
    void testExclusionLogicRule() {
        // If NO claims > 500 exist, mark as Low Activity
        Person p = new Person("LowActivityUser", 30, "Male");
        Claim c = new Claim("C008", 200.0, "Dental"); // All claims < 500
        Audit audit = rulesService.executeRules(new EvaluationRequest(p, List.of(c)));

        assertTrue(audit.getAudits().contains("Status: Very Low Activity (No claims > 500)"),
                "Should detect absence of high claims");
        assertTrue(audit.getAudits().contains("Status: Low Risk Portfolio (No claims > 10k)"),
                "Should also detect absence of very high claims");
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
