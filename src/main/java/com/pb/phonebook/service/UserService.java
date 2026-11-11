package com.pb.phonebook.service;

import java.util.List;

import org.springframework.data.domain.Page;

import com.pb.phonebook.dto.UserDto;

public interface UserService {
    UserDto createUser(UserDto userDto);

    UserDto getUserById(Long id);

    List<UserDto> getAllUsers();

    // Method lama tetap dipertahankan sebagai overload
    List<UserDto> searchUsers(String keyword, String subregion);

    // Method baru dengan dukungan sorting dinamis (location, name, dsb.)
    List<UserDto> searchUsers(String keyword, String subregion, String sortField, String sortDir);

    // Method dengan dukungan filter location
    List<UserDto> searchUsers(String keyword, String subregion, Long locationId, String sortField, String sortDir);

    // Method dengan pagination (15 item per halaman)
    Page<UserDto> searchUsersPaginated(String keyword, String subregion, Long locationId, String sortField, String sortDir, int page, int size);

    UserDto updateUser(Long id, UserDto userDto);

    void deleteUser(Long id);
}
