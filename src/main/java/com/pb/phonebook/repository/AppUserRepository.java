package com.pb.phonebook.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pb.phonebook.entity.AppUser;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);    
}
