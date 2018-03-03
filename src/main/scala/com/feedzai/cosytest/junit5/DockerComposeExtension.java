package com.feedzai.cosytest.junit5;

import com.feedzai.cosytest.wrapper.SetupManager;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class DockerComposeExtension implements BeforeAllCallback, AfterAllCallback, AfterTestExecutionCallback {

    private final SetupManager setupManager;

    public DockerComposeExtension(SetupManager setupManager) {
        this.setupManager = setupManager;
    }

    @Override
    public void afterTestExecution(ExtensionContext context) throws Exception {
        if (setupManager != null && context.getExecutionException().isPresent()) {
            setupManager.setTestFailed(true);
        }
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        if (setupManager != null) {
            setupManager.bootstrap();
        }
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        if (setupManager != null) {
            setupManager.tearDown();
        }
    }
}
