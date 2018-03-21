package com.feedzai.cosytest.junit4;

import com.feedzai.cosytest.wrapper.DockerComposeJavaSetup;
import com.feedzai.cosytest.wrapper.SetupManager;
import org.junit.*;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;

import static org.hamcrest.Matchers.*;


public class IntegrationSpec {

    private static final Logger logger  = LoggerFactory.getLogger(IntegrationSpec.class);
    private static final String zipFile = "zipFile";
    private static final Path zipPath   = Paths.get(String.format("%s.zip", zipFile));

    private static final DockerComposeJavaSetup dockerSetup;

    static {
        dockerSetup = new DockerComposeJavaSetup(
            "junit4test",
            Collections.singletonList(Paths.get("src", "test", "resources", "docker-compose-junit4.yml")),
            Paths.get("").toAbsolutePath(),
            new HashMap<>()
        );
    }

    @ClassRule
    public static DockerComposeRule dockerComposeRule = new DockerComposeRule(SetupManager.builder(dockerSetup).build());

    @Rule
    public TestWatcher testWatcher = new TestWatcher() {
        @Override
        protected void failed(Throwable e, Description description) {
            dockerComposeRule.setTestFailed(true);
        }
    };

    @BeforeClass
    public static void init() {
        logger.info("Running JUnit 4 Integration Spec!");
    }

    @AfterClass
    public static void teardown() {
        try {
            Files.deleteIfExists(zipPath);
        } catch (IOException e) {
            logger.info("Failed to teardown JUnit Integration Spec!");
        }
    }

    @Test
    public void fetchServiceContainerIds() {
        ArrayList<String> ids = dockerSetup.getServiceContainerIds("container1");
        Assert.assertThat(ids, hasSize(1));
        Assert.assertThat(ids.get(0).isEmpty(), is(false));
    }

    @Test
    public void fetchServiceContainerIps() {
        ArrayList<InetAddress> ips = dockerSetup.getServiceContainerAddresses("container1");
        Assert.assertThat(ips, hasSize(1));
        Assert.assertThat(ips.get(0).isSiteLocalAddress(), is(true));
    }

    @Test
    public void fetchProjectContainerIds() {
        ArrayList<String> ids = dockerSetup.getProjectContainerIds();
        Assert.assertThat(ids, hasSize(3));
        ids.parallelStream().forEach(id -> Assert.assertThat(id.isEmpty(), is(false)));
    }

    @Test
    public void fetchProjectContainerIps() {
        ArrayList<InetAddress> ips = dockerSetup.getProjectContainerAddresses();
        Assert.assertThat(ips, hasSize(3));
        ips.parallelStream().forEach(ip -> Assert.assertThat(ip.isSiteLocalAddress(), is(true)));
    }

    @Test
    public void fetchProjectNetworkIds() {
        ArrayList<String> ids = dockerSetup.getProjectNetworkIds();
        Assert.assertThat(ids, hasSize(1));
        Assert.assertThat(ids.get(0).isEmpty(), is(false));
    }

    @Test
    public void fetchServices() {
        Assert.assertThat(
                dockerSetup.getServices(),
                containsInAnyOrder("container1", "container2", "container3")
        );
    }

    @Test
    public void fetchServiceMappedPort() {
        Assert.assertThat(dockerSetup.getServiceMappedPort("container1", 80), contains("8084"));
        Assert.assertThat(dockerSetup.getServiceMappedPort("container2", 80), contains("8085"));
        Assert.assertThat(dockerSetup.getServiceMappedPort("container3", 80), contains("8086"));
    }

    @Test
    public void fetchContainerMappedPort() {
        ArrayList<String> ids = dockerSetup.getServiceContainerIds("container1");
        Assert.assertThat(ids, hasSize(1));
        Assert.assertThat(dockerSetup.getContainerMappedPort(ids.get(0), 80), is("8084"));
    }

    @Test
    public void fetchContainerIp() {
        ArrayList<String> ids = dockerSetup.getServiceContainerIds("container1");
        Assert.assertThat(ids, hasSize(1));
        Optional<InetAddress> ip = dockerSetup.getContainerAddress(ids.get(0));
        Assert.assertThat(ip.isPresent(), is(true));
        Assert.assertThat(ip.get().isSiteLocalAddress(), is(true));
    }

    @Test
    public void validateContainerHasHealthCheck() {
        ArrayList<String> ids = dockerSetup.getServiceContainerIds("container1");
        Assert.assertThat(ids, hasSize(1));
        Assert.assertThat(dockerSetup.isContainerWithHealthCheck(ids.get(0)), is(true));
        Assert.assertThat(dockerSetup.isContainerWithHealthCheck("invalidId"), is(false));
    }

    @Test
    public void validateContainerManagement() throws IOException {
        // Containers up and running
        Assert.assertThat(dockerSetup.checkContainersRemoval(), is(false));
        // Cleanup environment
        Assert.assertThat(dockerSetup.cleanup(), is(true));
        Assert.assertThat(dockerSetup.checkContainersRemoval(), is(true));
        // Start containers
        Assert.assertThat(dockerSetup.dockerComposeUp(), is(true));
        Assert.assertThat(dockerSetup.waitForAllHealthyContainers(Duration.ofSeconds(5)), is(true));
        // Stop containers
        Assert.assertThat(dockerSetup.dockerComposeDown(), is(true));
        Assert.assertThat(dockerSetup.checkContainersRemoval(), is(true));

        // Start containers
        Assert.assertThat(dockerSetup.dockerComposeUp(), is(true));
        ArrayList<String> ids = dockerSetup.getServiceContainerIds("container1");
        Assert.assertThat(ids, hasSize(1));
        Assert.assertThat(dockerSetup.waitForHealthyContainer(ids.get(0), Duration.ofSeconds(5)), is(true));

        // Get logs
        ArrayList<String> containerLogs = dockerSetup.getContainerLogs(Optional.of("container1"));
        ArrayList<String> allLogs = dockerSetup.getContainerLogs(Optional.empty());

        Assert.assertThat(containerLogs.size(), is(not(0)));
        Assert.assertThat(containerLogs.stream().anyMatch(line -> line.contains("container1_1")), is(true));
        Assert.assertThat(containerLogs.stream().anyMatch(line -> line.contains("container2_1")), is(false));

        Assert.assertThat(allLogs.size(), is(not(0)));
        Assert.assertThat(allLogs.stream().anyMatch(line -> line.contains("container1_1")), is(true));
        Assert.assertThat(allLogs.stream().anyMatch(line -> line.contains("container2_1")), is(true));

        Assert.assertThat(dockerSetup.dumpLogs(zipFile, Paths.get("")).isSuccess(), is(true));
        Assert.assertThat(Files.exists(zipPath), is(true));
        Files.delete(zipPath);
    }
}


