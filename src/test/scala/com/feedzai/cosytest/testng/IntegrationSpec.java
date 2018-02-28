package com.feedzai.cosytest.testng;

import com.feedzai.cosytest.DockerComposeJavaManager;
import com.feedzai.cosytest.DockerComposeJavaSetup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestResult;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;


public class IntegrationSpec {

    private static Logger logger = LoggerFactory.getLogger(IntegrationSpec.class);

    private DockerComposeJavaSetup dockerSetup;

    private DockerComposeJavaManager containerManager;

    @BeforeClass
    public void init() {

        dockerSetup = new DockerComposeJavaSetup(
                "testngtest",
                Collections.singletonList(Paths.get("src", "test", "resources", "docker-compose-junit.yml")),
                Paths.get("").toAbsolutePath(),
                new HashMap<>()
        );

        containerManager = DockerComposeJavaManager
                .builder(dockerSetup)
                .build();

        containerManager.launchContainers();
    }

    @AfterMethod
    public void checkTestResult(ITestResult result) {
        if (result.getStatus() == ITestResult.FAILURE) {
            containerManager.setTestFailed(true);
        }
    }

    @AfterClass
    public void tearDown() {
        containerManager.tearDownContainers();
    }

    @Test
    public void fetchServices() {
        assertThat(
                dockerSetup.getServices(),
                containsInAnyOrder("container1", "container2", "container3")
        );
    }

    @Test
    public void validateMappedPorts() {
        assertThat(dockerSetup.getServiceMappedPort("container1", 80), contains("8084"));
        assertThat(dockerSetup.getServiceMappedPort("container2", 80), contains("8085"));
        assertThat(dockerSetup.getServiceMappedPort("container3", 80), contains("8086"));
    }

}


