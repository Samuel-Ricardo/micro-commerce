package com.js.microservices.inventory_service;

import io.restassured.RestAssured;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import static org.hamcrest.Matchers.*;


@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class InventoryServiceApplicationTests {

	@LocalServerPort
	private Integer port;
	@ServiceConnection
	static private MySQLContainer mysql = new MySQLContainer(DockerImageName.parse("mysql:latest"));

	@BeforeEach
	void setup() {
		RestAssured.baseURI = "http://localhost";
		RestAssured.port = port;
	}

	static { mysql.start(); }

	@Test
	void shouldHaveStock() {
		RestAssured
				.when()
				.get("/api/v1/inventory?skuCode=iphone_15&quantity=100")
				.then()
				.statusCode(200)
				.body(containsString("true"));
	}

	@Test
	void shouldNotHaveStock() {
		RestAssured
				.when()
				.get("/api/v1/inventory?skuCode=iphone_15&quantity=101")
				.then()
				.statusCode(200)
				.body(containsString("false"));
	}
}
