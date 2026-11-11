package com.pb.phonebook.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.pb.phonebook.entity.AppUser;
import com.pb.phonebook.service.RegisterService;

import lombok.RequiredArgsConstructor;


@Controller
@RequiredArgsConstructor
public class RegisterController {

    private final RegisterService registerService;
    
    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("user", new AppUser());
        return "register";
    }

    @PostMapping("/register")
    public String processRegister(@ModelAttribute("user") AppUser user,
                                   @RequestParam String confirmPassword,
                                   Model model) {

        if (user.getUsername() == null || user.getUsername().isBlank()
                || user.getPassword() == null || user.getPassword().isBlank()) {
            model.addAttribute("errorMessage", "Username dan password wajib diisi.");
            return "register";
        }

        if (!user.getPassword().equals(confirmPassword)) {
            model.addAttribute("errorMessage", "Password dan konfirmasi password tidak sama.");
            return "register";
        }

        registerService.registerUser(user.getUsername(), user.getPassword());
        return "redirect:/login?registered";
    }
}
