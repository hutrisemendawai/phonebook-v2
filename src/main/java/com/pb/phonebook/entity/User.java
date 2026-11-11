package com.pb.phonebook.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import org.springframework.data.domain.Persistable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class User implements Persistable<Long> {

    // Tanpa @GeneratedValue: ID di-assign manual dari service (lihat
    // UserServiceImpl),
    // supaya ID yang sudah dihapus bisa dipakai ulang oleh user baru.
    @Id
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, length = 500)
    private String name;

    @Column(nullable = true, length = 500)
    private String email;

    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;

    @ManyToOne
    @JoinColumn(name = "location_id")
    private Location location;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Extension> extensions = new ArrayList<>();

    // Menandai entity ini "baru" (harus di-INSERT, bukan di-UPDATE).
    // Default false supaya entity hasil load dari DB (lewat no-args constructor)
    // selalu dianggap existing.
    // Hanya di-set true secara eksplisit lewat builder saat bikin user baru di
    // UserServiceImpl.
    @Transient
    @Builder.Default
    private boolean newEntity = false;

    @Override
    public boolean isNew() {
        return newEntity;
    }
}
