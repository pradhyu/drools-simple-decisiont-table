package com.example.drools;

import com.example.drools.model.Audit;
import com.example.drools.model.Person;
import com.example.drools.service.RulesService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

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
        Audit audit = rulesService.executeRules(p);

        assertTrue(audit.getAudits().contains("Adult Male detected"), "Should detect Adult Male");
    }

    @Test
    void testSeniorFemaleRule() {
        Person p = new Person("Jane", 70, "Female");
        Audit audit = rulesService.executeRules(p);

        assertTrue(audit.getAudits().contains("Senior Female detected"), "Should detect Senior Female");
    }

    @Test
    void testMinorRule() {
        Person p = new Person("Kid", 10, "Male");
        Audit audit = rulesService.executeRules(p);

        assertTrue(audit.getAudits().contains("Minor detected"), "Should detect Minor");
    }

    @Test
    void testRulesEndpoint() throws Exception {
        Person p = new Person("ControllerTest", 40, "Female");

        mockMvc.perform(post("/api/rules")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(p)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.audits[0]").value("Adult Female detected"));
    }
}
