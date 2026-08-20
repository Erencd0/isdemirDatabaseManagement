package com.isdemir.isdemirdb.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.isdemir.isdemirdb.dto.MaterialRequest;
import com.isdemir.isdemirdb.dto.MaterialResponse;
import com.isdemir.isdemirdb.entity.Heat;
import com.isdemir.isdemirdb.entity.Material;
import com.isdemir.isdemirdb.entity.MaterialUsage;
import com.isdemir.isdemirdb.repository.HeatRepository;
import com.isdemir.isdemirdb.repository.MaterialRepository;
import com.isdemir.isdemirdb.repository.MaterialUsageRepository;

import lombok.RequiredArgsConstructor;

// Business logic of the bulk material insert. PARTIAL SUCCESS: when one item of the list
// breaks a rule ONLY that item is rejected, the others are saved. That is why the batch is
// not wrapped in a single transaction -- every valid item is written on its own
// (repository.save). heatId (dokum_no) comes from the top level and applies to every item.
//
// Rules (PER ITEM; a violation makes that item return success=false, NO exception is thrown):
//  - Mandatory fields: heatId, additive, materialCode, quantity, addedTime.
//  - heatId = dokum_no; the item is rejected when no such heat exists.
//  - additive = malzeme_turu; the item is rejected when materialCode is not an ACTIVE
//    material of that type (e.g. "POTAKATKI" + 12 while 12 only exists under "HURDA").
//  - addedTime (malzeme_verilis_tarihi) cannot be before the dokum_zamani of the heat.
@Service
@RequiredArgsConstructor
public class MaterialRecordService {

    private final HeatRepository heatRepository;
    private final MaterialRepository materialRepository;
    private final MaterialUsageRepository materialUsageRepository;

    // An empty/null list (no material in this request) returns an empty list -- sending only
    // events is valid. Otherwise every item is processed one by one; the result list keeps
    // the order of the items.
    public List<MaterialResponse> saveBatch(Integer heatId, List<MaterialRequest> requests) {
        List<MaterialResponse> results = new ArrayList<>();
        if (requests == null || requests.isEmpty()) {
            return results;
        }
        for (MaterialRequest request : requests) {
            results.add(saveOne(heatId, request));
        }
        return results;
    }

    // Validates and saves a single item. On failure it does NOT throw; it returns a response
    // with success=false so the calling loop can continue with the remaining items.
    private MaterialResponse saveOne(Integer heatId, MaterialRequest request) {
        String fieldError = validateFields(heatId, request);
        if (fieldError != null) {
            return result(heatId, request, false, fieldError, null);
        }

        // heatId = dokum_no. The item is rejected when the heat does not exist.
        Heat heat = heatRepository.findByHeatNo(heatId).orElse(null);
        if (heat == null) {
            return result(heatId, request, false, "Döküm bulunamadı (No: " + heatId + ").", null);
        }

        // Does materialCode exist as ACTIVE inside this additive (type)? Otherwise reject it.
        // The same lookup hands back the malzeme_id the usage row is written with.
        Material material = materialRepository
                .findByCodeAndTypeAndActiveTrue(request.getMaterialCode(), request.getAdditive())
                .orElse(null);
        if (material == null) {
            return result(heatId, request, false, request.getMaterialCode() + " kodlu malzeme '"
                    + request.getAdditive() + "' katkısına ait değil.", null);
        }

        // A material cannot be delivered before the heat.
        if (heat.getTapTime() != null
                && request.getAddedTime().isBefore(heat.getTapTime())) {
            return result(heatId, request, false,
                    "Malzeme eklenme zamanı, döküm zamanından önce olamaz.", null);
        }

        MaterialUsage usage = new MaterialUsage();
        usage.setHeatId(heat.getId());
        usage.setMaterialId(material.getId());
        usage.setQuantity(request.getQuantity().doubleValue());
        usage.setDeliveredAt(request.getAddedTime());
        usage.setUserId(request.getUserId());
        MaterialUsage saved = materialUsageRepository.save(usage);

        return result(heatId, request, true, "Malzeme kaydedildi.", saved.getId());
    }

    private String validateFields(Integer heatId, MaterialRequest request) {
        if (heatId == null) {
            return "heatId zorunludur.";
        }
        if (request.getAdditive() == null || request.getAdditive().isBlank()) {
            return "additive zorunludur.";
        }
        if (request.getMaterialCode() == null) {
            return "materialCode zorunludur.";
        }
        if (request.getQuantity() == null) {
            return "quantity zorunludur.";
        }
        if (request.getAddedTime() == null) {
            return "addedTime zorunludur.";
        }
        return null;
    }

    // Builds the item response, echoing the input back so the client can match the item.
    private MaterialResponse result(Integer heatId, MaterialRequest request, boolean success,
            String message, Integer usageId) {
        return new MaterialResponse(success, message, heatId, request.getAdditive(),
                request.getMaterialCode(), request.getQuantity(), request.getAddedTime(), usageId);
    }
}
