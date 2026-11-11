package com.pb.phonebook.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.pb.phonebook.dto.CompanyDto;
import com.pb.phonebook.service.CompanyService;

import lombok.RequiredArgsConstructor;


@Controller
@RequiredArgsConstructor
@RequestMapping("/companies")
public class CompanyController {
    
    private final CompanyService companyService;

    @GetMapping
    public String listCompanies(Model model) {
        model.addAttribute("companies", companyService.getAllCompanies());
        return "companies/list";
    }

    @GetMapping("/new")
    public String createCompanyForm(Model model) {
        model.addAttribute("company", new CompanyDto());
        return "companies/create";
    }

    @PostMapping
    public String saveCompany(@ModelAttribute("company") CompanyDto companyDto, RedirectAttributes redirectAttributes) {
        companyService.createCompany(companyDto);
        redirectAttributes.addFlashAttribute("successMessage", "Data perusahaan telah berhasil dibuat!");
        return "redirect:/companies";
    }

    @GetMapping("/edit/{id}")
    public String editCompanyForm(@PathVariable Long id, Model model) {
        model.addAttribute("company", companyService.getCompanyById(id));
        return "companies/edit";
    }

    @PostMapping("/{id}")
    public String updateCompany(@PathVariable Long id, @ModelAttribute("company")
                                CompanyDto companyDto,
                                RedirectAttributes redirectAttributes) {
        companyService.updateCompany(id, companyDto);
        redirectAttributes.addFlashAttribute("successMessage", "Data perusahaan sukses diperbaharui.");
        return "redirect:/companies";
    }

    @PostMapping("/delete/{id}")
    public String deleteCompany(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        companyService.deleteCompany(id);
        redirectAttributes.addFlashAttribute("successMessage", "Data perusahaan telah berhasil dihapus.");
        return "redirect:/companies";
    }
}
