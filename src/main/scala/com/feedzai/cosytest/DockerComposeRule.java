/*
 * The copyright of this file belongs to Feedzai. The file cannot be
 * reproduced in whole or part, stored in a retrieval system,
 * transmitted in any form, or by any means electronic, mechanical,
 * photocopying, or otherwise, without prior permission of the owner.
 *
 * © 2018 Feedzai, Strictly Confidential
 */

package com.feedzai.cosytest;

import org.junit.rules.ExternalResource;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.runtime.BoxedUnit;
import scala.util.Try;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;

public class DockerComposeRule extends ExternalResource {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private final DockerComposeJavaSetup dockerSetup;

    private final Duration containerStartUpTimeout;

    private Boolean keepContainersOnSuccess = false;
    private Boolean keepContainersOnFailure = false;

    private Optional<Path> logDumpLocation   = Optional.empty();
    private Optional<String> logDumpFileName = Optional.empty();

    private Boolean testFailed = false;

    public DockerComposeRule() {
        this(null);
    }

    public DockerComposeRule(DockerComposeJavaSetup dockerSetup) {
        this(dockerSetup, Duration.ofMinutes(5));
    }

    public DockerComposeRule(DockerComposeJavaSetup dockerSetup, Duration containerStartUpTimeout) {
        this.dockerSetup = dockerSetup;
        this.containerStartUpTimeout = containerStartUpTimeout;
    }

    public Boolean getKeepContainersOnSuccess() {
        return keepContainersOnSuccess;
    }

    public void setKeepContainersOnSuccess(Boolean keepContainersOnSuccess) {
        this.keepContainersOnSuccess = keepContainersOnSuccess;
    }

    public Boolean getKeepContainersOnFailure() {
        return keepContainersOnFailure;
    }

    public void setKeepContainersOnFailure(Boolean keepContainersOnFailure) {
        this.keepContainersOnFailure = keepContainersOnFailure;
    }

    public Optional<Path> getLogDumpLocation() {
        return logDumpLocation;
    }

    public void setLogDumpLocation(Optional<Path> logDumpLocation) {
        this.logDumpLocation = logDumpLocation;
    }

    public Optional<String> getLogDumpFileName() {
        return logDumpFileName;
    }

    public void setLogDumpFileName(Optional<String> logDumpFileName) {
        this.logDumpFileName = logDumpFileName;
    }

    public Boolean getTestFailed() {
        return testFailed;
    }

    public void setTestFailed(Boolean testFailed) {
        this.testFailed = testFailed;
    }

    @Override
    protected void before() {
        if (dockerSetup != null) {
            logger.info("Starting containers...");
            Boolean started = dockerSetup.up(containerStartUpTimeout);
            if(!started) {
                this.after();
                Assert.fail("Failed to start containers for setup " + dockerSetup.setupName() + "!");
            }
            logger.info("Containers started!");
        }
    }

    @Override
    protected void after() {
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
                Assert.assertThat(
                        "Failed to remove containers for setup " + dockerSetup.setupName() + "!",
                        removed,
                        is(true)
                );
                logger.info("Containers removed!");
            }
        }
    }
}
