package com.epages.wiremock.starter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import com.github.tomakehurst.wiremock.WireMockServer;

@WireMockTest(port = 8081)
@SpringBootTest
@RunWith(SpringRunner.class)
@Configuration
public class SmokeTest {

	@Autowired
	public WireMockServer server;

	@Test
	public void check_server_availability() {
		assertThat(server.isRunning()).isTrue();
		assertThat(server.port()).isEqualTo(8081);
		ResponseEntity<String> result = new RestTemplate().exchange("http://localhost:" + server.port() + "/__admin", HttpMethod.GET, null, String.class);
		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

}
