# cosy-test
[![Build Status](https://travis-ci.org/feedzai/cosy-test.svg?branch=master)](https://travis-ci.org/feedzai/cosy-test)
[![codecov](https://codecov.io/gh/feedzai/cosy-test/branch/master/graph/badge.svg)](https://codecov.io/gh/feedzai/cosy-test)
[![Maven metadata URI](https://img.shields.io/maven-metadata/v/http/central.maven.org/maven2/com/feedzai/cosy-test_2.12/maven-metadata.xml.svg)](https://mvnrepository.com/artifact/com.feedzai/cosy-test)

Bringing up Docker Compose environments for system, integration and performance testing, with support for [ScalaTest](http://www.scalatest.org/),
[JUnit 4](https://junit.org/junit4/), [JUnit 5](https://junit.org/junit5/), [TestNG](http://testng.org/doc/)  and [Gatling](https://gatling.io/).

## Why do I need this?

Imagine you need to test a complex system that requires having a lot of components working and simulating a realistic scenario.
Probably, you would just use a docker-compose file to start your environment and then do your tests.
That seems easy... But:

* How would you know that the environment is already up and running for testing?
* What if you need to test a lot of those environments concurrently?
* Would you be capable of managing all the container mapped ports without conflicts?

That is where `cosy-test` can make your life easier.

## What is cosy-test and how it helps you?

It is a simple framework that allows integration with several testing frameworks.
With `cosy-test` it is possible to simply use docker compose files and define environment variables in order to
start docker environments, run tests and bring environments down without pains and restrictions.

Main features:

* Brings the environments up before the tests and tears them down afterwards.
* Start tests just after all health checks are passing.
* Can be configured to dump container logs for debugging purposes.
* Containers can be kept running on failure, success or both.
* Control your system through environment variables.
* Ports exposed by containers are mapped to the host machine and are made available to the tests. This approach is
  especially handy on MacOS (as opposed to reaching the containers by their IP addresses).

## Requirements

In order for `cosy-test` to work, it is necessary to have Docker and Docker Compose installed. There are no version restrictions,
however we recommend using:

- Docker >= 18.02.0 (_older versions have problems during containers start up and/or tear down_)
- Docker Compose >= 1.17.1

## Usage

SBT

    libraryDependencies += "com.feedzai" %% "cosy-test" % "0.0.3"

Maven

    <dependency>
        <groupId>com.feedzai</groupId>
        <artifactId>cosy-test_2.12</artifactId>
        <version>0.0.3</version>
    </dependency>

## Example — ScalaTest

``` scala
class IntegrationSpec extends FlatSpec with DockerComposeTestSuite with MustMatchers {

  def dockerSetup = Some(
    DockerComposeSetup(
      "scalatest",  // Setup name
      Seq(Paths.get("src", "test", "resources", "docker-compose-scalatest.yml")), // Docker compose files
      Paths.get("").toAbsolutePath, // Docker compose working directory
      Map.empty // Environment variables. Example: Map("CONTAINER_EXPOSED_PORT" -> "80")
    )
  )

  behavior of "Scala Test"

  it must "Retrieve all services" in {
    val expectedServices = Set("container1", "container2", "container3")
    dockerSetup.foreach { setup =>
      setup.getServices().toSet mustEqual expectedServices
    }
  }
}
```


## Example — JUnit 4

``` java
public class IntegrationSpec {

    private static final DockerComposeJavaSetup dockerSetup;

    static {
        dockerSetup = new DockerComposeJavaSetup(
            "junit4test", // Setup name
            Collections.singletonList(Paths.get("src", "test", "resources", "docker-compose-junit4.yml")), // Docker compose files
            Paths.get("").toAbsolutePath(), // Docker compose working directory
            new HashMap<>() // Environment variables. Example: envMap.put("CONTAINER_EXPOSED_PORT", "80")
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

    @Test
    public void fetchServices() {
       Assert.assertThat(
           dockerSetup.getServices(),
           containsInAnyOrder("container1", "container2", "container3")
       );
    }
}
```

## Example — JUnit 5

``` java
public class IntegrationSpec {

    private static final DockerComposeJavaSetup dockerSetup;

    static {
        dockerSetup = new DockerComposeJavaSetup(
            "junit5test", // Setup name
            Collections.singletonList(Paths.get("src", "test", "resources", "docker-compose-junit5.yml")), // Docker compose files
            Paths.get("").toAbsolutePath(),  // Docker compose working directory
            new HashMap<>() // Environment variables. Example: envMap.put("CONTAINER_EXPOSED_PORT", "80")
        );
    }

    @RegisterExtension
    static DockerComposeExtension extension = new DockerComposeExtension(SetupManager.builder(dockerSetup).build());

    @Test
    public void fetchServices() {
        Assert.assertThat(
            dockerSetup.getServices(),
            containsInAnyOrder("container1", "container2", "container3")
        );
    }
}
```

## Example — TestNG

``` scala
public class IntegrationSpec extends DockerComposeAbstraction {

    private final DockerComposeJavaSetup dockerSetup = new DockerComposeJavaSetup(
        "testngtest", // Setup name
        Collections.singletonList(Paths.get("src", "test", "resources", "docker-compose-testng.yml")), // Docker compose files
        Paths.get("").toAbsolutePath(), // Docker compose working directory
        new HashMap<>() // Environment variables. Example: envMap.put("CONTAINER_EXPOSED_PORT", "80")
    );

    IntegrationSpec() {
        setupManager = SetupManager.builder(dockerSetup).build();
    }

    @Test
    public void fetchServices() {
        Assert.assertThat(
            dockerSetup.getServices(),
            containsInAnyOrder("container1", "container2", "container3")
        );
    }
}
```

## Example — Gatling

``` scala
class IntegrationSpec extends DockerComposeSimulation {

  override def dockerSetup = Some(
    DockerComposeSetup(
      "gatling", // Setup name
      Seq(Paths.get("src", "test", "resources", "docker-compose-gatling.yml")), // Docker compose files
      Paths.get("").toAbsolutePath, // Docker compose working directory
      Map.empty // Environment variables. Example: Map("CONTAINER_EXPOSED_PORT" -> "80")
    )
  )

  beforeSimulation()

  private val HttpProtocol: HttpProtocolBuilder = http
    .baseURL("http://localhost:8086")

  private val populationBuilder = {
    val getAction = http("Gatling simulation").get("")
    val s = scenario("Gatling simulation")
    s.during(10.seconds)(feed(Iterator.empty).exec(getAction))
      .inject(rampUsers(10).over(1.second))
      .protocols(HttpProtocol)
  }

  setUp(populationBuilder)
}
```

## Build, Test and Release

After cloning the repository you can simply build and run the tests, by executing the command:

    sbt test

Since `cosy-test` uses [sbt-dynver](https://github.com/dwijnand/sbt-dynver) to release a new version it is just required
to create a new tag like:

    git tag v1.0.0
