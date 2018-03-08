package com.feedzai.cosytest.testng;

import com.feedzai.cosytest.wrapper.SetupManager;
import org.testng.ITestResult;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;

public abstract class DockerComposeAbstraction {

    public SetupManager setupManager = null;

    @AfterMethod
    public void checkTestResult(ITestResult result) {
        if (result.getStatus() == ITestResult.FAILURE && setupManager != null) {
            setupManager.setTestFailed(true);
        }
    }

    @BeforeClass
    public void beforeAll() {
        if(setupManager != null) {
            setupManager.bootstrap();
        }
    }

    @AfterClass
    public void afterAll() {
        if(setupManager != null) {
            setupManager.tearDown();
        }
    }
}
