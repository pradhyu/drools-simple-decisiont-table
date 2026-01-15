package com.example.drools.config;

import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.io.Resource;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieContainer;
import org.kie.internal.builder.DecisionTableConfiguration;
import org.kie.internal.builder.DecisionTableInputType;
import org.kie.internal.builder.KnowledgeBuilderFactory;
import org.kie.internal.io.ResourceFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DroolsConfig {

    @Bean
    public KieContainer kieContainer() {
        KieServices kieServices = KieServices.Factory.get();
        KieFileSystem kieFileSystem = kieServices.newKieFileSystem();

        // List of decision table files
        String[] decisionTables = {
                "rules/clinical-rules.csv",
                "rules/claims-rules.csv"
        };

        // Configuration for CSV
        DecisionTableConfiguration configuration = KnowledgeBuilderFactory.newDecisionTableConfiguration();
        configuration.setInputType(DecisionTableInputType.CSV);

        for (String dtPath : decisionTables) {
            // Print DRL for debugging
            try (java.io.InputStream is = getClass().getClassLoader().getResourceAsStream(dtPath)) {
                if (is != null) {
                    org.drools.decisiontable.SpreadsheetCompiler compiler = new org.drools.decisiontable.SpreadsheetCompiler();
                    String drl = compiler.compile(is, org.drools.decisiontable.InputType.CSV);
                    System.out.println("\n=== GENERATED DRL START (" + dtPath + ") ===");
                    System.out.println(drl);
                    System.out.println("=== GENERATED DRL END ===\n");
                } else {
                    System.err.println("Could not find decision table file: " + dtPath);
                }
            } catch (Exception e) {
                System.err.println("Failed to print generated DRL for " + dtPath + ": " + e.getMessage());
            }

            // Load the CSV decision table
            Resource resource = ResourceFactory.newClassPathResource(dtPath);
            resource.setResourceType(ResourceType.DTABLE);
            resource.setConfiguration(configuration);
            kieFileSystem.write(resource);
        }

        KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
        kieBuilder.buildAll();

        KieModule kieModule = kieBuilder.getKieModule();
        return kieServices.newKieContainer(kieModule.getReleaseId());
    }
}
