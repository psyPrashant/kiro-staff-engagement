package com.psybergate.staff_engagement.client;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CompanyController {

	private final CompanyRepository companyRepository;
	private final ClientService clientService;

	@GetMapping("/api/companies")
	public List<Company> getCompanies() {
		return companyRepository.findAll();
	}

	@PostMapping("/api/companies")
	@ResponseStatus(HttpStatus.CREATED)
	public Company createCompany(@RequestBody CreateCompanyRequest request) {
		return clientService.createCompany(request);
	}
}
