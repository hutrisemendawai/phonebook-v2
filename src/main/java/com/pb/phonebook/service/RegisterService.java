package com.pb.phonebook.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.pb.phonebook.entity.AppUser;
import com.pb.phonebook.repository.AppUserRepository;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class RegisterService {
    
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public void registerUser(String username, String password) {
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        // Role selalu USER untuk self-registration.
        // Akun ADMIN harus dibuat manual (lewat DB atau endpoint khusus yang di-protect hasRole("ADMIN")).
        user.setRole("USER");
        appUserRepository.save(user);
    }
}
