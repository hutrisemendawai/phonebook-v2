package com.pb.phonebook.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.pb.phonebook.dto.CompanyDto;
import com.pb.phonebook.entity.Company;
import com.pb.phonebook.repository.CompanyRepository;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {
    
    private final CompanyRepository companyRepository;

    private CompanyDto mapToDto(Company company) {
        return CompanyDto.builder()
                .id(company.getId())
                .name(company.getName())
                .code(company.getCode())
                .build();
    }

    private Company mapToEntity(CompanyDto dto) {
        return Company.builder()
                .id(dto.getId())
                .name(dto.getName())
                .code(dto.getCode())
                .build();
    }

    @Override
    public CompanyDto createCompany(CompanyDto companyDto) {
        Company company = mapToEntity(companyDto);
        return mapToDto(companyRepository.save(company));
    }

    @Override
    public CompanyDto getCompanyById(Long id) {
        return companyRepository.findById(id)
                .map(this::mapToDto)
                .orElse(null);
    }

    @Override
    public List<CompanyDto> getAllCompanies() {
        return companyRepository.findAll()
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    public CompanyDto updateCompany(Long id, CompanyDto companyDto) {
        return companyRepository.findById(id)
                .map(existing -> {
                    existing.setName(companyDto.getName());
                    existing.setCode(companyDto.getCode());
                    return mapToDto(companyRepository.save(existing));
                }).orElse(null);
    }

    @Override
    public void deleteCompany(Long id) {
        companyRepository.deleteById(id);
    }

}
