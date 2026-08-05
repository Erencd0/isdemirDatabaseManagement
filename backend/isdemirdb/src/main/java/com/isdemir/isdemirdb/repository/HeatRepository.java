package com.isdemir.isdemirdb.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.isdemir.isdemirdb.entity.Heat;

@Repository
public interface HeatRepository extends JpaRepository<Heat, Integer> {

    // Returns the latest heat of one line (same converter), i.e. the highest heat number.
    // Used to compare the times of a new heat against that last heat.
    Optional<Heat> findFirstByHeatNoBetweenOrderByHeatNoDesc(Integer low, Integer high);

    // Only the heats of the given converters. The listing uses it so a user sees just the
    // converters their kv1/kv2/kv3 roles cover.
    List<Heat> findByConverterNoIn(Collection<Integer> converterNos);

    // In the event flow heatId = dokum_no; used to find the matching heat row.
    Optional<Heat> findByHeatNo(Integer heatNo);

    // Returns the current highest dokum_id (0 when the table is empty)
    @Query("SELECT COALESCE(MAX(h.id), 0) FROM Heat h")
    Integer findMaxId();

    // Returns the highest heat number inside a given band (0 when the band is empty).
    // The counter advances by the numeric band of dokum_no and NOT by the konverter_no
    // column, so old/inconsistent konverter_no data cannot corrupt the next number.
    // E.g. converter=1 -> band [6100001, 6199999].
    @Query("SELECT COALESCE(MAX(h.heatNo), 0) FROM Heat h WHERE h.heatNo BETWEEN :low AND :high")
    Integer findMaxHeatNoInRange(Integer low, Integer high);
}
