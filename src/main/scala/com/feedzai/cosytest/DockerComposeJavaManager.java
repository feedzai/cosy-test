/*
 * The copyright of this file belongs to Feedzai. The file cannot be
 * reproduced in whole or part, stored in a retrieval system,
 * transmitted in any form, or by any means electronic, mechanical,
 * photocopying, or otherwise, without prior permission of the owner.
 *
 * © 2018 Feedzai, Strictly Confidential
 */

package com.feedzai.cosytest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import scala.runtime.BoxedUnit;
import scala.util.Try;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Utility class that provides container lifecycle management operations and configuration.
 */
public class DockerComposeJavaManager {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private final DockerComposeJavaSetup dockerSetup;

    private final Duration containerStartUpTimeout;

    private boolean keepContainersOnSuccess;
    private boolean keepContainersOnFailure;

    private Optional<Path> logDumpLocation;
    private Optional<String> logDumpFileName;

    private boolean testFailed;

    DockerComposeJavaManager(final Builder builder) {
        this.dockerSetup = builder.dockerSetup;
        this.containerStartUpTimeout = builder.containerStartUpTimeout;
        this.keepContainersOnSuccess = builder.keepContainersOnSuccess;
        this.keepContainersOnFailure = builder.keepContainersOnFailure;
        this.logDumpLocation = builder.logDumpLocation;
        this.logDumpFileName = builder.logDumpFileName;
        testFailed = false;
    }

    public boolean getKeepContainersOnSuccess() {
        return keepContainersOnSuccess;
    }

    public boolean getKeepContainersOnFailure() {
        return keepContainersOnFailure;
    }

    public Optional<Path> getLogDumpLocation() {
        return logDumpLocation;
    }

    public Optional<String> getLogDumpFileName() {
        return logDumpFileName;
    }

    public boolean getTestFailed() {
        return testFailed;
    }

    public void setTestFailed(final boolean testFailed) {
        this.testFailed = testFailed;
    }

    public void launchContainers() {
        if (dockerSetup != null) {
            logger.info("Starting containers...");
            Boolean started = dockerSetup.up(containerStartUpTimeout);
            if(!started) {
                this.tearDownContainers();
                Assert.fail("Failed to start containers for setup " + dockerSetup.setupName() + "!");
            }
            logger.info("Containers started!");
        }
    }

    public void tearDownContainers() {
        if (dockerSetup != null) {
            if (testFailed && logDumpFileName.isPresent() && logDumpLocation.isPresent()) {
                Try<BoxedUnit> dumpLogs = dockerSetup.dumpLogs(logDumpFileName.get(), logDumpLocation.get());
                if (dumpLogs.isFailure()) {
                    logger.error("Failed to dump logs!", dumpLogs.failed().get());
                }
            }

            Boolean keep = (keepContainersOnSuccess && !testFailed) || (keepContainersOnFailure && testFailed);

            if (!keep) {
                logger.info("Removing containers...");
                Boolean removed = dockerSetup.down();
                assertThat(
                        "Failed to remove containers for setup " + dockerSetup.setupName() + "!",
                        removed,
                        is(true)
                );
                logger.info("Containers removed!");
            }
        }
    }

    public static Builder builder(DockerComposeJavaSetup dockerSetup) {
        return new Builder(dockerSetup);
    }

    public static class Builder {

        DockerComposeJavaSetup dockerSetup;

        Duration containerStartUpTimeout = Duration.ofMinutes(5);

        boolean keepContainersOnSuccess = false;
        boolean keepContainersOnFailure = false;

        Optional<Path> logDumpLocation   = Optional.empty();
        Optional<String> logDumpFileName = Optional.empty();

        Builder(final DockerComposeJavaSetup manager) {
            this.dockerSetup = manager;
        }

        public Builder withContainerStartUpTimeout(final Duration startUpTimeout) {
            this.containerStartUpTimeout = startUpTimeout;
            return this;
        }

        public Builder withKeepContainersOnSuccess(final boolean keepContainersOnSuccess) {
            this.keepContainersOnSuccess = keepContainersOnSuccess;
            return this;
        }

        public Builder withKeepContainersOnFailure(final boolean keepContainersOnFailure) {
            this.keepContainersOnFailure = keepContainersOnFailure;
            return this;
        }

        public Builder withLogDumpLocation(final Path logDumpLocation) {
            this.logDumpLocation = Optional.of(logDumpLocation);
            return this;
        }

        public Builder withLogDumpFileName(final String logDumpFileName) {
            this.logDumpFileName = Optional.of(logDumpFileName);
            return this;
        }

        public DockerComposeJavaManager build() {
            return new DockerComposeJavaManager(this);
        }

    }
}
