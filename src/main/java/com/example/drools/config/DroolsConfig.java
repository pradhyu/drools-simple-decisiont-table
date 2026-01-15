package com.example.drools.config;

import org.drools.decisiontable.InputType;
import org.drools.decisiontable.SpreadsheetCompiler;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieContainer;
import org.kie.internal.builder.DecisionTableConfiguration;
import org.kie.internal.builder.DecisionTableInputType;
import org.kie.internal.builder.KnowledgeBuilderFactory;
import org.kie.internal.io.ResourceFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.Resource;

import java.io.IOException;

@Configuration
public class DroolsConfig {

    @Bean
    public KieContainer kieContainer() throws IOException {
        KieServices kieServices = KieServices.Factory.get();
        KieFileSystem kieFileSystem = kieServices.newKieFileSystem();

        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath*:rules/*.*");

        for (Resource file : resources) {
            String filename = file.getFilename();
            if (filename == null)
                continue;

            String lowerFilename = filename.toLowerCase();
            DecisionTableInputType inputType = null;
            InputType compilerInputType = null;

            if (lowerFilename.endsWith(".csv")) {
                inputType = DecisionTableInputType.CSV;
                compilerInputType = InputType.CSV;
            } else if (lowerFilename.endsWith(".xls") || lowerFilename.endsWith(".xlsx")) {
                inputType = DecisionTableInputType.XLS;
                compilerInputType = InputType.XLS;
            }

            if (inputType != null) {
                System.out.println("Loading rule file: " + filename);

                byte[] content;
                try (java.io.InputStream is = file.getInputStream()) {
                    content = org.springframework.util.StreamUtils.copyToByteArray(is);
                }

                // Print DRL for debugging
                try {
                    SpreadsheetCompiler compiler = new SpreadsheetCompiler();
                    String drl = compiler.compile(new java.io.ByteArrayInputStream(content), compilerInputType);
                    System.out.println("\n=== GENERATED DRL START (" + filename + ") ===");
                    System.out.println(drl);
                    System.out.println("=== GENERATED DRL END ===\n");
                } catch (Exception e) {
                    System.err.println("Failed to print generated DRL for " + filename + ": " + e.getMessage());
                }

                // Add to KieFileSystem
                org.kie.api.io.Resource droolsResource = ResourceFactory.newByteArrayResource(content);
                // Explicitly set the source path so Drools handles packages correctly relative
                // to the rules folder
                droolsResource.setSourcePath("src/main/resources/rules/" + filename);
                droolsResource.setResourceType(ResourceType.DTABLE);

                DecisionTableConfiguration configuration = KnowledgeBuilderFactory.newDecisionTableConfiguration();
                configuration.setInputType(inputType);
                droolsResource.setConfiguration(configuration);

                kieFileSystem.write(droolsResource);
            }
        }

        KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
        kieBuilder.buildAll();

        KieModule kieModule = kieBuilder.getKieModule();
        return kieServices.newKieContainer(kieModule.getReleaseId());
    }
}
