package com.isdemir.isdemirdb.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.isdemir.isdemirdb.dto.HeatDetailResponse;
import com.isdemir.isdemirdb.entity.Heat;
import com.isdemir.isdemirdb.entity.Material;
import com.isdemir.isdemirdb.entity.MaterialUsage;
import com.isdemir.isdemirdb.exception.InvalidDataException;
import com.isdemir.isdemirdb.exception.RecordNotFoundException;
import com.isdemir.isdemirdb.repository.HeatRepository;
import com.isdemir.isdemirdb.repository.MaterialRepository;
import com.isdemir.isdemirdb.repository.MaterialUsageRepository;
import com.isdemir.isdemirdb.security.ConverterAccess;

import lombok.RequiredArgsConstructor;

// All business logic of heats and their material usages lives in this layer.
// The controller only calls this service; exception -> HTTP status mapping is done in
// GlobalExceptionHandler.
@Service
@RequiredArgsConstructor
public class HeatService {

    private final HeatRepository heatRepository;
    private final MaterialUsageRepository materialUsageRepository;
    private final MaterialRepository materialRepository;
    private final ConverterAccess converterAccess;

    // Lists the heats of the converters the logged in user is authorised for (kv1/kv2/kv3).
    public List<Heat> findAll() {
        return heatRepository.findByConverterNoIn(converterAccess.allowed());
    }

    // Creates a new heat. Breaking a time/order rule raises InvalidDataException (-> 400).
    public Heat create(Heat heat) {
        // A heat can only be opened on a converter the user holds the role for (-> 403).
        // Checked first, so an unauthorised converter is not answered with a rule error.
        converterAccess.require(heat.getConverterNo());
        // The time values must be in a sensible order among themselves; otherwise do not save.
        String timeError = validateTimes(heat);
        if (timeError != null) {
            throw new InvalidDataException(timeError);
        }
        // Heats of the same converter are sequential: a new heat cannot start before the
        // previous heat of that converter. Different converters run in parallel and have
        // no rule between them.
        String orderError = validateOrderAgainstPreviousHeat(heat);
        if (orderError != null) {
            throw new InvalidDataException(orderError);
        }
        // When no record time was submitted, use the current time
        if (heat.getCreatedAt() == null) {
            heat.setCreatedAt(LocalDateTime.now());
        }
        // Assign the id by hand: current max id + 1. When the insert fails no sequence
        // advances, so the next insert retries the very same number.
        heat.setId(heatRepository.findMaxId() + 1);

        // The dokum_no rule lives in nextHeatNo(). The "sonraki-no" endpoint uses the same
        // calculation, so the number shown on screen equals the number that gets saved.
        heat.setHeatNo(nextHeatNo(heat.getConverterNo()));

        return heatRepository.save(heat);
    }

    // Calculates the next heat number.
    // Rule: 7 digits, starts with 6, the 2nd digit is the converter number (the selected
    // role kv1/2/3 -> 1/2/3), the remaining 5 digits are a per-converter counter starting
    // at 00001. Example: converter=3 (kv3) -> first heat 6300001, then 6300002 ...
    // The frontend also calls this to show the read-only number on the form.
    public Integer nextHeatNo(Integer converterNo) {
        converterAccess.require(converterNo);
        int base = 6_000_000 + converterNo * 100_000; // e.g. converter 3 -> 6300000
        // Only the numbers inside this converter's band (base+1 .. base+99999) are looked
        // at, so the prefix is always correct (kv1 -> 61..., kv2 -> 62..., kv3 -> 63...).
        Integer lastHeatNo = heatRepository.findMaxHeatNoInRange(base + 1, base + 99_999);
        // First heat of this converter -> base+1 (6300001), otherwise last number + 1
        return lastHeatNo == 0 ? base + 1 : lastHeatNo + 1;
    }

    // Adds a material to a heat. The heat id comes from the path and overrides the body.
    // Unknown heat -> RecordNotFoundException (-> 404).
    public MaterialUsage addMaterial(Integer heatId, MaterialUsage materialUsage) {
        Heat heat = requireHeat(heatId);
        // A material cannot be delivered before the heat: the delivery time cannot be
        // earlier than the tap time.
        String timeError = validateMaterialTime(heat, materialUsage.getDeliveredAt());
        if (timeError != null) {
            throw new InvalidDataException(timeError);
        }
        materialUsage.setHeatId(heatId);
        return materialUsageRepository.save(materialUsage);
    }

    // Updates a material usage of a heat (material code, quantity, delivery time).
    // It is looked up by kullanim_id; when it does not belong to that heat a
    // RecordNotFoundException (-> 404) is raised, so another heat's row cannot be overwritten.
    public MaterialUsage updateMaterial(Integer heatId, Integer usageId, MaterialUsage incoming) {
        Heat heat = requireHeat(heatId);
        MaterialUsage current = materialUsageRepository.findById(usageId)
                .filter(m -> heatId.equals(m.getHeatId()))
                .orElseThrow(() -> new RecordNotFoundException("Malzeme kullanımı bulunamadı"));
        // The updated delivery time cannot be earlier than the tap time either.
        String timeError = validateMaterialTime(heat, incoming.getDeliveredAt());
        if (timeError != null) {
            throw new InvalidDataException(timeError);
        }
        current.setMaterialCode(incoming.getMaterialCode());
        current.setQuantity(incoming.getQuantity());
        current.setDeliveredAt(incoming.getDeliveredAt());
        if (incoming.getUserId() != null) {
            current.setUserId(incoming.getUserId());
        }
        return materialUsageRepository.save(current);
    }

    // Deletes a material usage of a heat. When it does not belong to that heat a
    // RecordNotFoundException (-> 404) is raised.
    public void deleteMaterial(Integer heatId, Integer usageId) {
        requireHeat(heatId);
        MaterialUsage current = materialUsageRepository.findById(usageId)
                .filter(m -> heatId.equals(m.getHeatId()))
                .orElseThrow(() -> new RecordNotFoundException("Malzeme kullanımı bulunamadı"));
        materialUsageRepository.delete(current);
    }

    // "Detay gor": returns the heat together with every material added to it.
    // Unknown heat -> RecordNotFoundException (-> 404).
    public HeatDetailResponse detail(Integer id) {
        Heat heat = heatRepository.findById(id)
                .orElseThrow(() -> new RecordNotFoundException("Döküm bulunamadı"));
        return buildDetail(heat);
    }

    // Same output as detail(), but looked up by dokum_no instead of dokum_id. The "Yenile"
    // button of the frontend calls this to check whether a record has been created (by the
    // event flow) for the heat number on screen; a 404 means the user is still creating it.
    public HeatDetailResponse detailByNo(Integer heatNo) {
        Heat heat = heatRepository.findByHeatNo(heatNo)
                .orElseThrow(() -> new RecordNotFoundException("Döküm bulunamadı"));
        return buildDetail(heat);
    }

    // Turns a heat plus its materials into a HeatDetailResponse (shared by detail/detailByNo).
    // Both lookup paths run through here, so the converter check is done once: a heat of an
    // unauthorised converter looks like a missing record (404), it is not "found but hidden".
    private HeatDetailResponse buildDetail(Heat heat) {
        if (!converterAccess.allowed().contains(heat.getConverterNo())) {
            throw new RecordNotFoundException("Döküm bulunamadı");
        }
        List<MaterialUsage> materials = materialUsageRepository.findByHeatId(heat.getId());
        fillMaterialNames(materials);
        return new HeatDetailResponse(heat, materials);
    }

    // ---- Internal helpers ----

    // Every material write goes through here: unknown heat -> 404, someone else's converter
    // -> 403, so an unauthorised user cannot touch another converter's materials either.
    private Heat requireHeat(Integer heatId) {
        Heat heat = heatRepository.findById(heatId)
                .orElseThrow(() -> new RecordNotFoundException("Döküm bulunamadı"));
        converterAccess.require(heat.getConverterNo());
        return heat;
    }

    // The process times of a heat, as a chronologically ordered array.
    // Order: scrap charge start -> end -> main blow start -> main blow end -> tap
    private LocalDateTime[] timeArray(Heat h) {
        return new LocalDateTime[] {
                h.getScrapChargeStartTime(),
                h.getScrapChargeEndTime(),
                h.getMainBlowStartTime(),
                h.getMainBlowEndTime(),
                h.getTapTime(),
        };
    }

    private static final String[] TIME_LABELS = {
            "Hurda Şarj Başlama",
            "Hurda Şarj Bitiş",
            "Ana Üflemeye Başlama",
            "Ana Üfleme Bitiş",
            "Döküm Zamanı",
    };

    // Checks whether the process times of a heat are chronological among themselves.
    // Returns an error message when a later time comes BEFORE an earlier one, null when
    // consistent. Null fields are skipped.
    private String validateTimes(Heat h) {
        LocalDateTime[] times = timeArray(h);
        for (int i = 1; i < times.length; i++) {
            if (times[i] != null && times[i - 1] != null
                    && times[i].isBefore(times[i - 1])) {
                return TIME_LABELS[i] + " zamanı, " + TIME_LABELS[i - 1] + " zamanından önce olamaz";
            }
        }
        return null;
    }

    // The material delivery time cannot be earlier than dokum_zamani, the last time of the
    // heat. Skipped when either side is null (no delivery time given, or no tap time yet).
    private String validateMaterialTime(Heat heat, LocalDateTime deliveredAt) {
        LocalDateTime tapTime = heat.getTapTime();
        if (deliveredAt != null && tapTime != null && deliveredAt.isBefore(tapTime)) {
            return "Malzeme veriliş tarihi, döküm zamanından önce olamaz";
        }
        return null;
    }

    // The earliest (non null) process time of a heat
    private LocalDateTime earliestTime(Heat h) {
        LocalDateTime earliest = null;
        for (LocalDateTime t : timeArray(h)) {
            if (t != null && (earliest == null || t.isBefore(earliest))) {
                earliest = t;
            }
        }
        return earliest;
    }

    // The latest (non null) process time of a heat
    private LocalDateTime latestTime(Heat h) {
        LocalDateTime latest = null;
        for (LocalDateTime t : timeArray(h)) {
            if (t != null && (latest == null || t.isAfter(latest))) {
                latest = t;
            }
        }
        return latest;
    }

    // Since the heats of one converter are sequential, the times of a new heat cannot be
    // earlier than the times of the previous heat of that converter (heats cannot overlap).
    // Returns an error when the earliest time of the new heat is before the latest time of
    // the previous one. Different converters (e.g. 6100001 and 6300001) are independent and
    // are not checked.
    private String validateOrderAgainstPreviousHeat(Heat newHeat) {
        int base = 6_000_000 + newHeat.getConverterNo() * 100_000;
        Heat previous = heatRepository
                .findFirstByHeatNoBetweenOrderByHeatNoDesc(base + 1, base + 99_999)
                .orElse(null);
        if (previous == null) {
            return null; // first heat of this converter
        }
        LocalDateTime previousLast = latestTime(previous);
        LocalDateTime newFirst = earliestTime(newHeat);
        if (previousLast != null && newFirst != null && newFirst.isBefore(previousLast)) {
            return "Bu dökümün zamanları, aynı konverterdeki önceki dökümün (No: "
                    + previous.getHeatNo() + ") zamanlarından önce olamaz";
        }
        return null;
    }

    // Fills the material name and type matching malzeme_kodu into the material usages.
    // malzeme_tablosu is fetched in a single query and turned into a code -> Material map
    // (so there is no N+1).
    private void fillMaterialNames(List<MaterialUsage> materials) {
        if (materials.isEmpty()) {
            return;
        }
        List<Integer> codes = materials.stream()
                .map(MaterialUsage::getMaterialCode)
                .filter(c -> c != null)
                .distinct()
                .collect(Collectors.toList());
        Map<Integer, Material> materialsByCode = materialRepository.findByCodeIn(codes).stream()
                .collect(Collectors.toMap(Material::getCode, m -> m, (first, second) -> first));
        for (MaterialUsage usage : materials) {
            Material material = materialsByCode.get(usage.getMaterialCode());
            if (material != null) {
                usage.setMaterialName(material.getName());
                usage.setMaterialType(material.getType());
            }
        }
    }
}
