package com.oranx;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main application class for ORAN-X.
 * Entry point for the Quarkus application.
 */
@QuarkusMain
public class OranXApplication implements QuarkusApplication {

    private static final Logger LOG = LoggerFactory.getLogger(OranXApplication.class);

    @Override
    public int run(String... args) throws Exception {
        LOG.info("========================================");
        LOG.info("  ORAN-X: O-RAN Intelligent xApp       ");
        LOG.info("  Lifecycle Manager v1.0.0             ");
        LOG.info("========================================");
        LOG.info("");
        LOG.info("Starting ORAN-X services...");
        LOG.info("- Orchestration Engine: Lagrangian Decomposition");
        LOG.info("- LLM Integration: LangChain4j + OpenAI");
        LOG.info("- TMF APIs: TMF622, TMF620, TMF640, TMF688");
        LOG.info("- REST API: 15+ endpoints");
        LOG.info("");
        LOG.info("API Documentation: http://localhost:8080/swagger-ui");
        LOG.info("OpenAPI Spec: http://localhost:8080/q/openapi");
        LOG.info("Health Check: http://localhost:8080/api/v1/dashboard/health");
        LOG.info("");

        Quarkus.waitForExit();
        return 0;
    }

    public static void main(String... args) {
        Quarkus.run(OranXApplication.class, args);
    }
}
