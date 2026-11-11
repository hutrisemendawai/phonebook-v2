package com.pb.phonebook.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.pb.phonebook.dto.LocationDto;
import com.pb.phonebook.service.LocationService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("locations", locationService.getAllLocations());
        return "locations/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("location", new LocationDto());
        return "locations/create";
    }

    @PostMapping
    public String create(@ModelAttribute("location") LocationDto dto,
            RedirectAttributes redirectAttributes) {
        locationService.createLocation(dto);
        redirectAttributes.addFlashAttribute("successMessage", "Data Lokasi berhasil ditambahkan!");
        return "redirect:/locations";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        LocationDto dto = locationService.getLocationById(id);
        model.addAttribute("location", dto);
        return "locations/edit";
    }

    @PostMapping("/edit/{id}")
    public String update(@PathVariable Long id, @ModelAttribute("location") LocationDto dto,
            RedirectAttributes redirectAttributes) {
        locationService.updateLocation(id, dto);
        redirectAttributes.addFlashAttribute("successMessage", "Data Lokasi berhasil diperbaharui!");
        return "redirect:/locations";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        locationService.deleteLocation(id);
        redirectAttributes.addFlashAttribute("successMessage", "Data lokasi berhasil dihapus.");
        return "redirect:/locations";
    }

}