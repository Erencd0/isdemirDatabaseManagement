package com.isdemir.isdemirdb.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.isdemir.isdemirdb.entity.HeatPhoto;

@Repository
public interface HeatPhotoRepository extends JpaRepository<HeatPhoto, Long> {

    // The photos of one heat, oldest first (that is the upload order).
    List<HeatPhoto> findByHeatIdOrderByIdAsc(Long heatId);
}
