package com.example.api;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.example.App;
import com.example.domain.customer.Customer;
import com.example.domain.customer.CustomerRepository;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

@SpringBootTest(classes = App.class, 
	webEnvironment = WebEnvironment.RANDOM_PORT)
public class CustomerRestControllerIntegrationTest {
	
	@Autowired
	CustomerRepository customerRepository;
	
	@LocalServerPort
	int port;
	String apiEndpoint;
	TestRestTemplate restTemplate = new TestRestTemplate();
	Customer customer1;
	Customer customer2;

	@Data
	@JsonIgnoreProperties(ignoreUnknown = true)
	static class Page<T> {
		private List<T> content;
		private int numberOfElements;
	}
	
	@BeforeEach
	public void setUp() {
		customerRepository.deleteAll();
		customer1 = new Customer("Taro", "Yamada");
		customer2 = new Customer("Ichiro", "Suzuki");
		
		List<Customer> customers = Arrays.asList(customer1, customer2);
		customerRepository.saveAll(customers);
		apiEndpoint = "http://localhost:" + port + "/api/customers";
	}
	
	@Test
	public void testGetCustomers() throws Exception {
		ResponseEntity<Page<Customer>> response = restTemplate.exchange(
				apiEndpoint, HttpMethod.GET, null /* body,header */,
				new ParameterizedTypeReference<Page<Customer>> () {});
		assertThat(response.getStatusCode(), is(HttpStatus.OK));
		assertThat(response.getBody().getNumberOfElements(), is(2));
		
		// firstNameの昇順で取得されるのでインデックスと直観的には対応していない。（実務上のテストとしては問題があると思う。）
		compareCustomer(response.getBody().getContent().get(0), customer2);
		compareCustomer(response.getBody().getContent().get(1), customer1);
	}
	
	private void compareCustomer(Customer actual, Customer expected) {
		assertThat(actual.getId(), is(expected.getId()));
		assertThat(actual.getFirstName(), is(expected.getFirstName()));
		assertThat(actual.getLastName(), is(expected.getLastName()));
	}

	@Test
	public void testPostCustomers() throws Exception {
		Customer customer3 = new Customer("Nobita", "Nobi");
		
		ResponseEntity<Customer> response = restTemplate.exchange(apiEndpoint, 
				HttpMethod.POST, new HttpEntity<>(customer3), Customer.class);
		assertThat(response.getStatusCode(), is(HttpStatus.CREATED));
		Customer customer = response.getBody();
		assertThat(customer.getId(), is(notNullValue()));
		assertThat(customer.getFirstName(), is(customer3.getFirstName()));
		assertThat(customer.getLastName(), is(customer3.getLastName()));
		
		assertThat(
				restTemplate
					.exchange(
							apiEndpoint, 
							HttpMethod.GET, 
							null, 
							new ParameterizedTypeReference<Page<Customer>>() {
							}).getBody().getNumberOfElements(), is(3));
	}
	
	@Test
	public void testDeleteCustomers() throws Exception {
		ResponseEntity<Void> response = restTemplate.exchange(apiEndpoint + "/{id}", 
				HttpMethod.DELETE, null /* body,header */, Void.class, 
				Collections.singletonMap("id", customer1.getId()));
		assertThat(response.getStatusCode(), is(HttpStatus.NO_CONTENT));
		
		assertThat(
				restTemplate
					.exchange(
							apiEndpoint, 
							HttpMethod.GET, 
							null, 
							new ParameterizedTypeReference<Page<Customer>>() {
							}).getBody().getNumberOfElements(), is(1));
	}
}
