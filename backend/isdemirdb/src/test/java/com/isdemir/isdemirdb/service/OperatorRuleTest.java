package com.isdemir.isdemirdb.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.isdemir.isdemirdb.entity.Heat;
import com.isdemir.isdemirdb.exception.InvalidDataException;
import com.isdemir.isdemirdb.repository.HeatRepository;
import com.isdemir.isdemirdb.repository.MaterialRepository;
import com.isdemir.isdemirdb.repository.MaterialUsageRepository;
import com.isdemir.isdemirdb.repository.OperatorRepository;
import com.isdemir.isdemirdb.security.ConverterAccess;

// A new heat may only name an operator that still works here. The frontend combobox already
// offers nothing else; this is the check that holds for a direct POST (Postman) as well.
// No Spring context: the repositories and the access check are faked.
class OperatorRuleTest {

    private static final long ACTIVE = 3L;   // Mustafa Demir
    private static final long INACTIVE = 8L; // Hasan Öztürk, retired

    private final HeatRepository heatRepository = mock(HeatRepository.class);
    private final MaterialUsageRepository materialUsageRepository = mock(MaterialUsageRepository.class);
    private final MaterialRepository materialRepository = mock(MaterialRepository.class);
    private final ConverterAccess converterAccess = mock(ConverterAccess.class);
    private final OperatorRepository operatorRepository = mock(OperatorRepository.class);

    private final HeatService service = new HeatService(
            heatRepository, materialUsageRepository, materialRepository, converterAccess,
            new OperatorService(operatorRepository));

    private Heat heat(Long operatorId) {
        Heat heat = new Heat();
        heat.setConverterNo(1);
        heat.setScrapChargeStartTime(LocalDateTime.of(2026, 1, 10, 8, 0));
        heat.setTapTime(LocalDateTime.of(2026, 1, 10, 9, 0));
        heat.setOperatorId(operatorId);
        return heat;
    }

    @Test
    void savesAHeatWithAnActiveOperator() {
        when(operatorRepository.existsByIdAndActiveTrue(ACTIVE)).thenReturn(true);
        when(heatRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        Heat saved = service.create(heat(ACTIVE));

        assertEquals(ACTIVE, saved.getOperatorId());
        assertEquals(6_100_001, saved.getHeatNo()); // first heat of KV1 in an empty table
    }

    // The whole point of step 4: a retired operator cannot be posted, from anywhere.
    @Test
    void rejectsAnInactiveOperator() {
        when(operatorRepository.existsByIdAndActiveTrue(INACTIVE)).thenReturn(false);

        InvalidDataException error =
                assertThrows(InvalidDataException.class, () -> service.create(heat(INACTIVE)));

        assertEquals(true, error.getMessage().contains("8"));
    }

    // An id nobody has answers the same way (existsByIdAndActiveTrue is false for both).
    @Test
    void rejectsAnUnknownOperator() {
        assertThrows(InvalidDataException.class, () -> service.create(heat(999L)));
    }

    @Test
    void rejectsAHeatWithoutAnOperator() {
        assertThrows(InvalidDataException.class, () -> service.create(heat(null)));
    }
}
