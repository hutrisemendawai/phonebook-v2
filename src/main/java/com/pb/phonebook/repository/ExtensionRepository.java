package com.pb.phonebook.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.pb.phonebook.entity.Extension;

public interface ExtensionRepository extends JpaRepository<Extension, Long> {

    @Query("SELECT DISTINCT e.type FROM Extension e ORDER BY e.type")
    List<String> findDistinctTypes();
}
