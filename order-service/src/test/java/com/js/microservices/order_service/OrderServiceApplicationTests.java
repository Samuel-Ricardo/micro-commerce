package com.js.microservices.order_service;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.hamcrest.Matchers.*;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderServiceApplicationTests {

	@ServiceConnection
	static PostgreSQLContainer postgre = new PostgreSQLContainer("postgres");

	@LocalServerPort
	private Integer port;

	@BeforeEach
	void setup() {
		RestAssured.baseURI = "http://localhost";
		RestAssured.port = port;
	}

	static { postgre.start(); }

	@Test
	void shouldPlaceOrder() {

		String requestBody = """
				
				{
				  "orderNumber": "ORD-12345",
				  "skuCode": "PROD-001",
				  "price": 19.99,
				  "quantity": 2,
				  "userDetails": {
				    "email": "cliente@example.com",
				    "firstName": "Fulano",
				    "lastName": "de Tal"
				  }
				}
				
				""";

		RestAssured.given()
				.contentType("application/json")
				.body(requestBody)
				.when()
				.post("/api/v1/orders")
				.then()
				.statusCode(201)
				.body(containsString("has been placed with success"))
		;


	}

}
