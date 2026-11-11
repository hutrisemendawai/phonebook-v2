package com.pb.phonebook.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.pb.phonebook.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

        Optional<User> findByEmailIgnoreCase(String email);

        Optional<User> findByNameIgnoreCase(String name);

        @Query("SELECT u.id FROM User u ORDER BY u.id ASC")
        List<Long> findAllIdsOrdered();

        @Query("SELECT u FROM User u " +
                        "LEFT JOIN u.company c " +
                        "LEFT JOIN u.location l " +
                        "LEFT JOIN u.extensions ext " +
                        "WHERE (:subregion IS NULL OR :subregion = '' OR TRIM(LOWER(l.subregion)) = TRIM(LOWER(:subregion))) "
                        +
                        "AND (:locationId IS NULL OR l.id = :locationId) "
                        +
                        "AND (:keyword IS NULL OR :keyword = '' OR (" +
                        "   LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                        "   OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                        "   OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                        "   OR LOWER(l.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                        "   OR LOWER(l.subregion) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                        "   OR LOWER(ext.number) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                        "   OR LOWER(ext.type) LIKE LOWER(CONCAT('%', :keyword, '%'))" +
                        "))")
        List<User> searchUsersWithSubregion(
                        @Param("keyword") String keyword,
                        @Param("subregion") String subregion,
                        @Param("locationId") Long locationId,
                        Sort sort);

        default List<User> searchUsersWithSubregion(
                        String keyword,
                        String subregion,
                        Sort sort) {
                return searchUsersWithSubregion(keyword, subregion, null, sort);
        }

        @Query("SELECT u FROM User u " +
                        "JOIN u.company c " +
                        "JOIN u.location l " +
                        "WHERE LOWER(TRIM(u.name)) = LOWER(TRIM(:name)) " +
                        "AND LOWER(TRIM(c.name)) = LOWER(TRIM(:companyName)) " +
                        "AND LOWER(TRIM(l.name)) = LOWER(TRIM(:locationName)) " +
                        "AND LOWER(TRIM(l.subregion)) = LOWER(TRIM(:subregion))")

        Optional<User> findExactDuplicate(
                        @Param("name") String name,
                        @Param("companyName") String companyName,
                        @Param("locationName") String locationName,
                        @Param("subregion") String subregion);
}
