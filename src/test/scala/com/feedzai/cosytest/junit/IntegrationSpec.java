package com.feedzai.cosytest.junit;

import com.feedzai.cosytest.DockerComposeRule;
import com.feedzai.cosytest.DockerComposeJavaSetup;
import org.junit.*;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;


public class IntegrationSpec {

    private static Logger logger = LoggerFactory.getLogger(IntegrationSpec.class);


    private static final DockerComposeJavaSetup dockerSetup;

    static {
        dockerSetup = new DockerComposeJavaSetup(
            "junittest",
            Collections.singletonList(Paths.get("src", "test", "resources", "docker-compose-junit.yml")),
            Paths.get("").toAbsolutePath(),
            new HashMap<>()
        );
    }

    @ClassRule
    public static DockerComposeRule dockerComposeRule = new DockerComposeRule(dockerSetup);

    @Rule
    public TestWatcher testWatcher = new TestWatcher() {
        @Override
        protected void failed(Throwable e, Description description) {
            dockerComposeRule.setTestFailed(true);
        }
    };

    @BeforeClass
    public static void init() {
        logger.info("Running JUnit Integration Spec!");
    }

    @Test
    public void fetchServices() {
       Assert.assertThat(
           dockerSetup.getServices(),
           containsInAnyOrder("container1", "container2", "container3")
       );
    }

    @Test
    public void validateMappedPorts() {
        Assert.assertThat(dockerSetup.getServiceMappedPort("container1", 80), contains("8084"));
        Assert.assertThat(dockerSetup.getServiceMappedPort("container2", 80), contains("8085"));
        Assert.assertThat(dockerSetup.getServiceMappedPort("container3", 80), contains("8086"));
    }

}


