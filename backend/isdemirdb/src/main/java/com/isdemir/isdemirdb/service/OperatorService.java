package com.isdemir.isdemirdb.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.isdemir.isdemirdb.entity.Operator;
import com.isdemir.isdemirdb.exception.InvalidDataException;
import com.isdemir.isdemirdb.repository.OperatorRepository;

import lombok.RequiredArgsConstructor;

// Operator logic lives in this layer. The controller only calls this service.
@Service
@RequiredArgsConstructor
public class OperatorService {

    private final OperatorRepository operatorRepository;

    // The operators that can be picked for a heat (for the combobox). Inactive operators
    // are left out on purpose: this list is what the frontend AND Postman may choose from.
    public List<Operator> findActive() {
        return operatorRepository.findByActiveTrueOrderByNameAscSurnameAsc();
    }

    // The operator of one heat, active or not. An old heat keeps the operator who ran it even
    // after they leave, so this deliberately does NOT filter on aktif - "detay gor" has to be
    // able to show a retired operator (and warn about them). Null when the heat has none.
    public Operator findForHeat(Long operatorId) {
        return operatorId == null ? null : operatorRepository.findById(operatorId).orElse(null);
    }

    // The rule behind a new heat: it must name an operator, and that operator has to be one
    // of the active ones above. The frontend combobox already offers nothing else, so this
    // is what keeps a direct POST (Postman) to the same standard.
    // InvalidDataException -> 400 (see GlobalExceptionHandler).
    public void requireActive(Long operatorId) {
        if (operatorId == null) {
            throw new InvalidDataException("Operatör seçilmelidir");
        }
        if (!operatorRepository.existsByIdAndActiveTrue(operatorId)) {
            throw new InvalidDataException(
                    "Operatör aktif değil veya bulunamadı (id: " + operatorId + ")");
        }
    }
}
