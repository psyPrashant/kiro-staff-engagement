package com.psybergate.staff_engagement.client.web;

import com.psybergate.staff_engagement.client.domain.Company;
import com.psybergate.staff_engagement.client.dto.CreateCompanyRequest;
import com.psybergate.staff_engagement.client.service.ClientService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CompanyController {

	private final ClientService clientService;

	@GetMapping("/api/companies")
	public List<Company> getCompanies() {
		return clientService.listCompanies();
	}

	@PostMapping("/api/companies")
	@ResponseStatus(HttpStatus.CREATED)
	public Company createCompany(@RequestBody CreateCompanyRequest request) {
		return clientService.createCompany(request);
	}
}
