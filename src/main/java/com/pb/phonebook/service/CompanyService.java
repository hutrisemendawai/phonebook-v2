package com.pb.phonebook.service;

import java.util.List;

import com.pb.phonebook.dto.CompanyDto;


public interface CompanyService {
    
    CompanyDto createCompany(CompanyDto companyDto);
    CompanyDto getCompanyById(Long id);
    List<CompanyDto> getAllCompanies();
    CompanyDto updateCompany(Long id, CompanyDto companyDto);
    void deleteCompany(Long id);
    
}
