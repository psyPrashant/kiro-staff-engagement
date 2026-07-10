package com.psybergate.staff_engagement.client;

import com.jayway.jsonpath.JsonPath;
import com.psybergate.staff_engagement.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for GET /api/projects with companyId filter.
 *
 * Validates: Requirements 3.1, 3.2, 3.3
 */
class ProjectFilterIntegrationTest extends BaseIntegrationTest {

	@Autowired
	private TestRestTemplate restTemplate;

	private String sessionCookie;

	@BeforeEach
	void authenticateAndGetCookie() {
		HttpHeaders loginHeaders = new HttpHeaders();
		loginHeaders.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> loginRequest = new HttpEntity<>(
				"{\"email\":\"alice.johnson@psybergate.com\",\"password\":\"Password1\"}", loginHeaders);
		ResponseEntity<String> loginResponse = restTemplate.postForEntity("/api/auth/login", loginRequest, String.class);
		assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

		List<String> cookies = loginResponse.getHeaders().get(HttpHeaders.SET_COOKIE);
		assertThat(cookies).isNotNull().isNotEmpty();
		sessionCookie = cookies.stream()
				.filter(c -> c.startsWith("JSESSIONID"))
				.findFirst()
				.orElseThrow();
	}

	private ResponseEntity<String> getWithAuth(String url) {
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.COOKIE, sessionCookie);
		return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
	}

	@Test
	void getProjects_withCompanyIdFilter_returnsOnlyMatchingProjects() {
		// First get a real company ID from seed data
		ResponseEntity<String> companiesResponse = getWithAuth("/api/companies");
		assertThat(companiesResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		Integer companyId = JsonPath.read(companiesResponse.getBody(), "$[0].id");

		// GET projects filtered by companyId
		ResponseEntity<String> response = getWithAuth("/api/projects?companyId=" + companyId);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		String body = response.getBody();
		assertThat(body).isNotNull();

		// Verify all returned projects belong to the specified company
		List<Integer> companyIds = JsonPath.read(body, "$[*].company.id");
		assertThat(companyIds).isNotEmpty();
		assertThat(companyIds).allMatch(id -> id.equals(companyId));
	}

	@Test
	void getProjects_withoutFilter_returnsAllProjects() {
		// GET all projects without any filter
		ResponseEntity<String> response = getWithAuth("/api/projects");

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		String body = response.getBody();
		assertThat(body).isNotNull();

		// Seed data has at least 3 projects
		List<Object> projects = JsonPath.read(body, "$[*]");
		assertThat(projects).hasSizeGreaterThanOrEqualTo(3);
	}

	@Test
	void getProjects_nonExistentCompanyId_returnsEmptyList() {
		// GET projects with a companyId that doesn't exist
		ResponseEntity<String> response = getWithAuth("/api/projects?companyId=99999");

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		String body = response.getBody();
		assertThat(body).isNotNull();

		// Verify response is an empty array
		List<Object> projects = JsonPath.read(body, "$[*]");
		assertThat(projects).isEmpty();
	}
}
