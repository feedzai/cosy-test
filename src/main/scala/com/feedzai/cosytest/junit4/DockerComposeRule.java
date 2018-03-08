package com.feedzai.cosytest.junit4;

import com.feedzai.cosytest.wrapper.SetupManager;
import org.junit.rules.ExternalResource;

import java.util.Optional;

public class DockerComposeRule extends ExternalResource {

    private final SetupManager setupManager;

    public DockerComposeRule() {
        this(null);
    }

    public DockerComposeRule(SetupManager setupManager) {
        this.setupManager = setupManager;
    }

    public Optional<Boolean> getTestFailed() {
        if(setupManager != null) {
            return Optional.of(setupManager.getTestFailed());
        } else {
            return Optional.empty();
        }
    }

    public void setTestFailed(Boolean testFailed) {
        if(setupManager != null) {
            setupManager.setTestFailed(testFailed);
        }
    }

    @Override
    protected void before() {
        setupManager.bootstrap();
    }

    @Override
    protected void after() {
        setupManager.tearDown();
    }
}
