package com.feedzai.cosytest.testng;

import com.feedzai.cosytest.wrapper.DockerComposeJavaSetup;
import com.feedzai.cosytest.wrapper.SetupManager;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;


public class IntegrationSpec extends DockerComposeAbstraction {

    private static final Logger logger = LoggerFactory.getLogger(IntegrationSpec.class);

    private final DockerComposeJavaSetup dockerSetup = new DockerComposeJavaSetup(
        "testngtest",
        Collections.singletonList(Paths.get("src", "test", "resources", "docker-compose-testng.yml")),
        Paths.get("").toAbsolutePath(),
        new HashMap<>()
    );

    IntegrationSpec() {
        setupManager = SetupManager.builder(dockerSetup).build();
    }

    @BeforeClass
    public void init() {
        logger.info("Running TestNG Integration Spec!");
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
        Assert.assertThat(dockerSetup.getServiceMappedPort("container1", 80), contains("8093"));
        Assert.assertThat(dockerSetup.getServiceMappedPort("container2", 80), contains("8094"));
        Assert.assertThat(dockerSetup.getServiceMappedPort("container3", 80), contains("8095"));
    }

}
