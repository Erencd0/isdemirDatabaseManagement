package com.isdemir.isdemirdb.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.isdemir.isdemirdb.entity.Operator;

@Repository
public interface OperatorRepository extends JpaRepository<Operator, Long> {

    // The operators that still work here, by name (the combobox of the heat form).
    // An inactive operator is never offered for a new heat.
    List<Operator> findByActiveTrueOrderByNameAscSurnameAsc();

    // Guard for the heat POST: does this id belong to an operator that still works here?
    // A retired operator (or an id that is not there at all) answers false.
    boolean existsByIdAndActiveTrue(Long id);
}
