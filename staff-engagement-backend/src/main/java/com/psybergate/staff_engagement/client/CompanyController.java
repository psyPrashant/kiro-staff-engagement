package com.psybergate.staff_engagement.client;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CompanyController {

	private final CompanyRepository companyRepository;

	@GetMapping("/api/companies")
	public List<Company> getCompanies() {
		return companyRepository.findAll();
	}
}
