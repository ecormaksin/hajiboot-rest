package com.example.api;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.*;
import static com.jayway.restassured.RestAssured.when;
import static com.jayway.restassured.RestAssured.given;

import java.util.Arrays;
import java.util.List;

import com.jayway.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

import com.example.App;
import com.example.domain.customer.Customer;
import com.example.domain.customer.CustomerRepository;
import com.jayway.restassured.RestAssured;

@SpringBootTest(classes = App.class, 
	webEnvironment = WebEnvironment.RANDOM_PORT)
public class CustomerRestControllerIntegrationTest2 {

	private static final String targetPath = "/api/customers";
	
	@Autowired
	CustomerRepository customerRepository;
	
	@LocalServerPort
	int port;
	String apiEndpoint;
	Customer customer1;
	Customer customer2;

	@BeforeEach
	public void setUp() {
		customerRepository.deleteAll();
		customer1 = new Customer("Taro", "Yamada");
		customer2 = new Customer("Ichiro", "Suzuki");
		
		List<Customer> customers = Arrays.asList(customer1, customer2);
		customerRepository.saveAll(customers);
		RestAssured.port = port;
	}
	
	
	@Test
	public void testGetCustomers() throws Exception {
		when().get(targetPath)
			.then()
			.statusCode(HttpStatus.OK.value())
			.body("numberOfElements", is(2))
			.body("content[0].id", is(customer2.getId()))
			.body("content[0].firstName", is(customer2.getFirstName()))
			.body("content[0].lastName", is(customer2.getLastName()))
			.body("content[1].id", is(customer1.getId()))
			.body("content[1].firstName", is(customer1.getFirstName()))
			.body("content[1].lastName", is(customer1.getLastName()));
	}
	
	@Test
	public void testPostCustomers() throws Exception {
		Customer customer3 = new Customer("Nobita", "Nobi");
		
		given().body(customer3)
				.contentType(ContentType.JSON)
				.and()
				.when().post(targetPath)
				.then()
				.statusCode(HttpStatus.CREATED.value())
				.body("id", is(notNullValue()))
				.body("firstName", is(customer3.getFirstName()))
				.body("lastName", is(customer3.getLastName()));
		
		given().get(targetPath)
			.then()
			.statusCode(HttpStatus.OK.value())
			.body("numberOfElements", is(3));
	}
	
	@Test
	public void testDeleteCustomers() throws Exception {
		when().delete(targetPath + "/{id}", customer1.getId())
				.then()
				.statusCode(HttpStatus.NO_CONTENT.value());

		given().get(targetPath)
				.then()
				.statusCode(HttpStatus.OK.value())
				.body("numberOfElements", is(1));
	}
}
