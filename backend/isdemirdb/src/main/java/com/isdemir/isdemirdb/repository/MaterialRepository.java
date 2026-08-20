package com.isdemir.isdemirdb.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.isdemir.isdemirdb.entity.Material;

@Repository
public interface MaterialRepository extends JpaRepository<Material, Integer> {

    // Returns the distinct types of the active materials, alphabetically
    @Query("SELECT DISTINCT m.type FROM Material m "
            + "WHERE m.active = true AND m.type IS NOT NULL "
            + "ORDER BY m.type")
    List<String> findTypes();

    // Returns the active materials of one type
    List<Material> findByTypeAndActiveTrue(String type);

    // The material behind a malzeme_kodu the client sent (code -> malzeme_id on write).
    Optional<Material> findByCode(Integer code);

    // The ACTIVE material with this code inside this additive (malzeme_turu), empty when
    // the code does not belong to that additive (e.g. code=12 & type=POTAKATKI). The bulk
    // insert both validates with it and takes the malzeme_id it needs to write.
    Optional<Material> findByCodeAndTypeAndActiveTrue(Integer code, String type);
}
