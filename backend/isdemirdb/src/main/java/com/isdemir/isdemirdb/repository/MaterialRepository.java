package com.isdemir.isdemirdb.repository;

import java.util.List;

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

    // Returns the materials of the given codes (code -> name mapping for "detay gor")
    List<Material> findByCodeIn(List<Integer> codes);

    // Checks whether a material code belongs to an ACTIVE material of the given additive
    // (malzeme_turu). Used by the bulk insert to validate "this code is not in that
    // additive" (e.g. code=12 & type=POTAKATKI returns false when there is no match).
    boolean existsByCodeAndTypeAndActiveTrue(Integer code, String type);
}
