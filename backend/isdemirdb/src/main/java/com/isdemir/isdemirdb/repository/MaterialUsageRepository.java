package com.isdemir.isdemirdb.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.isdemir.isdemirdb.entity.MaterialUsage;

@Repository
public interface MaterialUsageRepository extends JpaRepository<MaterialUsage, Integer> {

    // Returns every material usage of one heat ("detay gor")
    List<MaterialUsage> findByHeatId(Integer heatId);
}
