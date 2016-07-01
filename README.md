# Spring REST Docs WireMock Integration

[ ![Build Status](https://travis-ci.org/ePages-de/restdocs-wiremock.svg)](https://travis-ci.org/ePages-de/restdocs-wiremock)
[ ![Download](https://api.bintray.com/packages/epages/maven/restdocs-wiremock/images/download.svg)](https://bintray.com/epages/maven/restdocs-wiremock/_latestVersion)

This is a plugin for auto-generating [WireMock](http://wiremock.org/) stubs
as part of documenting your REST API with [Spring REST Docs](http://projects.spring.io/spring-restdocs/).

The basic idea is to use the requests and responses from the integration tests as stubs for testing your client's 
API contract. The mock templates can be packaged as jar files and be published into your company's
artifact repository for this purpose.

## Contents

This repository consists of four projects

* `restdocs-wiremock`: The library to extend Spring REST Docs with WireMock stub snippet generation.
* `restdocs-server`: A sample server documenting its REST API (i.e. the Spring REST Docs "notes" example).
   Besides producing human-readable documentation it will also generate JSON snippets to be used as stubs for WireMock.
* `wiremock-spring-boot-starter`: A spring boot starter which adds a `WireMockServer` to your client's ApplicationContext for integration testing.
  This is optional, but highly recommended when verifying your client contract in a SpringBootTest.
* `restdocs-client`: A sample client using the server API, with integration testing its client contract against the stubs provided via WireMock.


## How to include `restdocs-wiremock` into your server project

### Dependencies

The project is published on `jcenter` from `bintray`, so firstly, you need to add `jcenter` as package
repository for your project. Please make sure you use the current release of Spring REST Docs, which is 
`1.1.0.RELEASE` as of this writing. The example below shows how to set Spring REST Docs to this version
when using Spring dependency management.

Then, add restdocs-wiremock as a dependency in test scope. In gradle it would look like this:

```groovy
dependencyManagement.imports {
    ext['spring-restdocs.version'] = '1.1.0.RELEASE'
}
dependencies {
  testCompile('com.epages:restdocs-wiremock:0.6.9')
}
```

When using maven:

```xml
<properties>
	<spring-restdocs.version>1.1.0.RELEASE</spring-restdocs.version>
</properties>
<dependency>
	<groupId>com.epages</groupId>
	<artifactId>restdocs-wiremock</artifactId>
	<version>0.6.9</version>
	<scope>test</scope>
</dependency>
```

### Producing snippets

During REST Docs run, snippets like the one below are generated and put into a dedicated jar file, which you can
publish into your artifact repository. 

Integration into your test code is as simple as adding `wiremockJson()` from `com.epages.restdocs.WireMockDocumentation`
to the `document()` calls for Spring REST Docs. For example:

```java
@RunWith(SpringJUnit4ClassRunner.class)
...
class ApiDocumentation {
    // ... the usual test setup.
    void testGetSingleNote() {
        this.mockMvc.perform(get("/notes/1").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andDo(document("get-note",
          wiremockJson(),
          responseFields( ... )
        ));
    }
}
```

The snippet below is the resulting snippet of a `200 OK` response to `/notes/1`, with
the response body as provided by the integration test.

```json
{
  "request" : {
    "method" : "GET",
    "urlPath" : "/notes/1"
  },
  "response" : {
    "status" : 200,
    "headers" : {
      "Content-Type" : [ "application/hal+json" ],
      "Content-Length" : [ "344" ]
    },
    "body" : "{\n  \"title\" : \"REST maturity model\",\n  \"body\" : \"http://martinfowler.com/articles/richardsonMaturityModel.html\",\n  \"_links\" : {\n    \"self\" : {\n      \"href\" : \"http://localhost:8080/notes/1\"\n    },\n    \"note\" : {\n      \"href\" : \"http://localhost:8080/notes/1\"\n    },\n    \"tags\" : {\n      \"href\" : \"http://localhost:8080/notes/1/tags\"\n    }\n  }\n}"
  }
}
```

### The WireMock stubs jar

On the server side you need to collect the WireMock stubs and publish them into an artifact repository.
In gradle this can be achieved by a custom jar task.

```groovy
task wiremockJar(type: Jar) {
	description = 'Generates the jar file containing the wiremock stubs for your REST API.'
	group = 'Build'
	classifier = 'wiremock'
	dependsOn project.tasks.test
	from (snippetsDir) {
		include '**/wiremock-stub.json'
		into "wiremock/${project.name}/mappings"
	}
}
```

*TODO: Add maven example.*

On the client side, add a dependency to the test-runtime to the jar containing the WireMock stubs. After
that, the JSON files can be accessed as classpath resources.

```groovy
testRuntime (group:'com.epages', name:'restdocs-server', version:'0.6.9', classifier:'wiremock', ext:'jar')
``` 

## How to use WireMock in your client tests

Integrating a WireMock server can easily be achieved by including our `wiremock-spring-boot-starter` into your project.
It adds a `wireMockServer` bean, which you can auto-wire in your test code. By default, we start WireMock on a dynamic port,
and set a `wiremock.port` property to the port WireMock is running on. This property can be used to point your clients
to the location of the `WireMock` server.

Services based on `spring-cloud-netflix`, i.e. using `feign` and `ribbon`, are auto-configured for you.

### Dependencies

To add a dependency via gradle, extend your `build.gradle` with the following line:

```groovy
  testCompile('com.epages:wiremock-spring-boot-starter:0.6.9')
```


When using maven, add the following dependency in test scope.

```xml
<dependency>
	<groupId>com.epages</groupId>
	<artifactId>wiremock-spring-boot-starter</artifactId>
	<version>0.6.9</version>
	<scope>test</scope>
</dependency>
```

### Configuring your test to use the WireMock stubs

Here is an excerpt of the sample test from the restdocs-client project to illustrate the usage.

```java
@RunWith(SpringJUnit4ClassRunner.class) // (1)
@SpringApplicationConfiguration(classes = { ClientApplication.class }) // (2)
@ActiveProfiles("test") // (3)
@WireMockTest(stubPath = "wiremock/restdocs-server") // (4) 
public class NoteServiceTest {

    @Autowired
    private WireMockServer wireMockServer; // (5)

    ....
}
```

1. Use Spring's JUnit Runner (as of 1.4.0 this will be called `SpringRunner`), for this is an integration test.
2. Include the usual application configuration classes
3. Extend your test with properties to point to your WireMock server.
   In our example we are using a Spring Expression inside `application-test.properties` to point our noteservice to
   WireMock: `noteservice.baseUri=http://localhost:${wiremock.port}/`
4. The `@WireMockTest` annotation enables the `wireMockServer` bean, which can be accessed
   from your test's application context. By default, it starts a WireMockServer on a dynamic port, but you could also
   set it to a fixed port. The `stubPath` property can be used to point to a classpath resource folder that
   holds your json stubs.
5. If you want, you can auto-wire the `WireMockServer` instance, and re-configure it, just as described in the official
   [WireMock documentation](http://wiremock.org/).

It is possible to read-in a subset of mappings for each test, by repeating the `@WireMockTest` annotation on the test method.
The `stubPath` is concatenated from the values given on the test class and the test method, just as a `@RequestMapping` annotation in Spring would.
In the example given below, the resulting stubPath provided to WireMock is composed as `wiremock/myservice/specific-mappings`.

```java
@WireMockTest(stubPath = "wiremock/myservice")
public class MyTest {
    @Test
    @WireMockTest(stubPath = "specific-mappings")
    public void testDifferentMappings() {
     ....
    }
```

## Building from source

1. Publish the current restdocs-wiremock library code into your local maven repository.

  ```shell
  ./gradlew restdocs-wiremock:build restdocs-wiremock:publishToMavenLocal
  ```

2.  Run the server tests, which uses the WireMock integration into Spring REST Docs. 
    As a result, there is a `restdocs-server-wiremock` jar file in your maven repository.
    Mind that this jar only contains a set of json files without explicit dependency on WireMock itself. 

  ```shell
  ./gradlew restdocs-server:build restdocs-server:publishToMavenLocal
  ```

3. Run the client tests, that expect a specific API from the server. 
   By mocking a server via WireMock the client can be tested in isolation, but would notice a breaking change.

  ```shell
  ./gradlew restdocs-client:build
  ```

  
## Publishing

This project makes use of the [axion-release-plugin](https://github.com/allegro/axion-release-plugin)
and publishing is automated in travis, when a new release is tagged in git.

Locally you should be able to create a new release by running the `release` task on gradle. A successful
travis build of this tag should finally end up on [bintray](https://bintray.com/epages/maven/restdocs-wiremock/).

```shell
./gradlew clean build release
```
