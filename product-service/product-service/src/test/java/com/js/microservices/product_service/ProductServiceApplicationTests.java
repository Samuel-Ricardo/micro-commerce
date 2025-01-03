package com.js.microservices.product_service;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.MongoDBContainer;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;


@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductServiceApplicationTests {

	@ServiceConnection
	static MongoDBContainer mongo = new MongoDBContainer("mongo:latest");

	@LocalServerPort
	private Integer port;


  @BeforeEach
  void setUp() {
	  RestAssured.baseURI = "http://localhost";
	  RestAssured.port = port;
  }

  static { mongo.start(); }

	@Test
	void shouldCreateProduct () {

	  String requestBody = """
					{
							"name": "Laptop",
							"skuCode": "LAPTOP123",
							"description": "A high-performance laptop for professionals.",
							"price": 1299.99
					}
			  
					""";

	  RestAssured.given()
			  .contentType("application/json")
			  .body(requestBody)
			  .when()
			  .post("/api/v1/products")
			  .then()
			  .statusCode(201)
			  .body("id", notNullValue())
			  .body("name", equalTo("Laptop"))
			  .body("description", equalTo("A high-performance laptop for professionals."))
			  .body("price", equalTo(1299.99F));


	}

	@Test
	void getAllProducts() {

	  RestAssured.given()
			  .contentType("application/json")
			  .when()
			  .get("/api/v1/products")
			  .then()
			  .statusCode(200)
			  .body("size()", is(0));

	}
}
