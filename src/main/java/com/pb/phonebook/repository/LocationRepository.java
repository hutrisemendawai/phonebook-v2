package com.pb.phonebook.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.pb.phonebook.entity.Location;

public interface LocationRepository extends JpaRepository<Location, Long> {
    List<Location> findBySubregion(String subregion);

    Optional<Location> findByNameIgnoreCaseAndSubregionIgnoreCase(String name, String subregion);

    List<Location> findByNameIgnoreCase(String name);

    @Query("SELECT DISTINCT l.subregion FROM Location l WHERE l.subregion IS NOT NULL AND TRIM(l.subregion) <> '' ORDER BY l.subregion ASC")
    List<String> findDistinctSubregions();
}
