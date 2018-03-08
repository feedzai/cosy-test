package com.feedzai.cosytest.junit5;

import com.feedzai.cosytest.wrapper.DockerComposeJavaSetup;
import com.feedzai.cosytest.wrapper.SetupManager;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;


public class IntegrationSpec {

    private static final Logger logger = LoggerFactory.getLogger(IntegrationSpec.class);

    private static final DockerComposeJavaSetup dockerSetup;

    static {
        dockerSetup = new DockerComposeJavaSetup(
            "junit5test",
            Collections.singletonList(Paths.get("src", "test", "resources", "docker-compose-junit5.yml")),
            Paths.get("").toAbsolutePath(),
            new HashMap<>()
        );
    }

    @RegisterExtension
    static DockerComposeExtension extension = new DockerComposeExtension(SetupManager.builder(dockerSetup).build());

    @BeforeAll
    public static void init() {
        logger.info("Running JUnit 5 Integration Spec!");
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
        Assert.assertThat(dockerSetup.getServiceMappedPort("container1", 80), contains("8090"));
        Assert.assertThat(dockerSetup.getServiceMappedPort("container2", 80), contains("8091"));
        Assert.assertThat(dockerSetup.getServiceMappedPort("container3", 80), contains("8092"));
    }

}
