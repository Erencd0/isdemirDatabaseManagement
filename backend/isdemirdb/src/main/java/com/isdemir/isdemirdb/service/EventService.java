package com.isdemir.isdemirdb.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.isdemir.isdemirdb.dto.EventRequest;
import com.isdemir.isdemirdb.dto.EventResponse;
import com.isdemir.isdemirdb.entity.Heat;
import com.isdemir.isdemirdb.exception.EventRuleException;
import com.isdemir.isdemirdb.repository.HeatRepository;
import com.isdemir.isdemirdb.security.ConverterAccess;

import lombok.RequiredArgsConstructor;

// ALL event business rules live in this layer. There is NO new table: every event fills one
// time column of the existing dokum_tablosu. heatId = dokum_no, and eventNo decides which
// time column gets written:
//   1 -> hurda_sarj_baslama_zamani
//   2 -> hurda_sarj_bitis_zamani
//   3 -> ana_uflemeye_baslama_zamani
//   4 -> ana_ufleme_bitis_zamani
//   5 -> dokum_zamani
// Rule violation -> EventRuleException -> HTTP 400 + ApiResponse in GlobalExceptionHandler.
@Service
@RequiredArgsConstructor
public class EventService {

    private final HeatRepository heatRepository;

    // eventNo -> step name. Index = eventNo - 1.
    private static final String[] EVENT_NAMES = {
            "Hurda Şarj Başlama",
            "Hurda Şarj Bitiş",
            "Ana Üflemeye Başlama",
            "Ana Üfleme Bitiş",
            "Döküm Zamanı",
    };

    // Saves the events of one heat inside a single transaction. heatId (dokum_no) comes from
    // the top level (HeatRecordRequest); the event items do not carry their own heatId.
    // An empty/null list returns an empty list (no event in this request -- material only is
    // perfectly valid).
    //
    // Rules:
    //  - The step order is NOT mandatory: some event numbers may stay empty (e.g. 3 can
    //    arrive while 1-2 are still empty).
    //  - Overwrite: an incoming eventNo overwrites the existing column (if any).
    //  - Times must be STRICTLY INCREASING: the filled time columns must increase along the
    //    step order; equality is a violation too (two steps cannot share one time).
    //  - When the heat row does not exist, this request creates it (heatId = new dokum_no).
    //
    // IMPORTANT: first EVERY incoming event time is written, and ONLY THEN the whole array is
    // validated in one pass. That way a batch updating several steps of an existing heat does
    // not produce a false conflict against the OLD neighbour values that have not been
    // overwritten yet (validating event by event, the first event would clash with a stale
    // neighbour that is about to be updated).
    @Transactional
    public List<EventResponse> saveBatch(Integer heatId, List<EventRequest> requests) {
        List<EventResponse> results = new ArrayList<>();
        if (requests == null || requests.isEmpty()) {
            return results;
        }
        if (heatId == null) {
            throw new EventRuleException("heatId zorunludur.");
        }

        // Find the heat once; create it with this request when it does not exist.
        Heat heat = heatRepository.findByHeatNo(heatId).orElseGet(() -> newHeat(heatId));

        // 1) Validate every incoming event and write the time columns (NO neighbour check).
        for (EventRequest request : requests) {
            request.setHeatId(heatId);
            validateFields(request);
            setTime(heat, request.getEventNo(), request.getEventTime());
        }

        // 2) Are the filled columns of the FINAL heat strictly increasing? Checked in one pass.
        validateOrderAll(heat);

        // 3) It has to start after the previous heat of the same converter (heats cannot overlap).
        validatePreviousHeat(heat);

        // 4) A single save; a violation would have thrown above and nothing would have been
        //    written (rollback).
        heatRepository.save(heat);

        for (EventRequest request : requests) {
            int eventNo = request.getEventNo();
            results.add(new EventResponse(heatId, eventNo,
                    EVENT_NAMES[eventNo - 1], request.getEventTime()));
        }
        return results;
    }

    // ---- Internal helpers ----

    // Creates a new heat from the first event when the heat row does not exist yet.
    // dokum_id is assigned by hand (no sequence, same convention as HeatService), konverter_no
    // is derived from the 2nd digit of dokum_no (e.g. 6[3]00001 -> 3), kayit_zamani is now.
    private Heat newHeat(Integer heatId) {
        Heat heat = new Heat();
        heat.setId(heatRepository.findMaxId() + 1);
        heat.setHeatNo(heatId);
        heat.setConverterNo(ConverterAccess.converterOfHeatNo(heatId));
        heat.setCreatedAt(LocalDateTime.now());
        return heat;
    }

    private void validateFields(EventRequest request) {
        if (request.getHeatId() == null) {
            throw new EventRuleException("heatId zorunludur.");
        }
        if (request.getEventNo() == null) {
            throw new EventRuleException("eventNo zorunludur.");
        }
        if (request.getEventNo() < 1 || request.getEventNo() > EVENT_NAMES.length) {
            throw new EventRuleException(
                    "eventNo 1 ile " + EVENT_NAMES.length + " arasında olmalıdır.");
        }
        if (request.getEventTime() == null) {
            throw new EventRuleException("eventTime zorunludur.");
        }
    }

    // The filled time columns of the heat must be STRICTLY INCREASING along the step order
    // (equality is a violation). Empty columns are skipped; only the filled steps are compared
    // consecutively. This runs on the FINAL state of the heat (called after every write of the
    // batch is done).
    private void validateOrderAll(Heat heat) {
        LocalDateTime previousTime = null;
        int previousNo = 0;
        for (int no = 1; no <= EVENT_NAMES.length; no++) {
            LocalDateTime time = getTime(heat, no);
            if (time == null) {
                continue;
            }
            if (previousTime != null && !time.isAfter(previousTime)) {
                throw new EventRuleException("Event zamanları sıralı değil: No " + no + " - "
                        + EVENT_NAMES[no - 1] + ", önceki dolu adım (No " + previousNo + " - "
                        + EVENT_NAMES[previousNo - 1] + ") zamanından sonra olmalıdır (eşit/önce olamaz).");
            }
            previousTime = time;
            previousNo = no;
        }
    }

    // Since the heats of one converter are sequential, the EARLIEST time of this heat cannot
    // be before the LATEST (tap) time of the previous heat in the same band -- otherwise the
    // two heats overlap. (The same rule exists for the form flow in
    // HeatService.validateOrderAgainstPreviousHeat; it is repeated here so it also holds for
    // the event flow.) Different converters are independent of each other.
    private void validatePreviousHeat(Heat heat) {
        int heatNo = heat.getHeatNo();
        int base = (heatNo / 100_000) * 100_000; // e.g. 6300009 -> 6300000 (this converter's band)
        // The highest heat number BELOW this heat is the previous heat. This heat itself is
        // not part of the range.
        Heat previous = heatRepository
                .findFirstByHeatNoBetweenOrderByHeatNoDesc(base + 1, heatNo - 1)
                .orElse(null);
        if (previous == null) {
            return; // first heat of this converter
        }
        LocalDateTime previousLast = latestTime(previous);
        LocalDateTime newFirst = earliestTime(heat);
        if (previousLast != null && newFirst != null && newFirst.isBefore(previousLast)) {
            throw new EventRuleException("Bu dökümün zamanları, aynı konverterdeki önceki dökümün"
                    + " (No: " + previous.getHeatNo() + ") döküm zamanından önce olamaz.");
        }
    }

    // The earliest (filled) time column of a heat.
    private LocalDateTime earliestTime(Heat h) {
        LocalDateTime earliest = null;
        for (int no = 1; no <= EVENT_NAMES.length; no++) {
            LocalDateTime t = getTime(h, no);
            if (t != null && (earliest == null || t.isBefore(earliest))) {
                earliest = t;
            }
        }
        return earliest;
    }

    // The latest (filled) time column of a heat.
    private LocalDateTime latestTime(Heat h) {
        LocalDateTime latest = null;
        for (int no = 1; no <= EVENT_NAMES.length; no++) {
            LocalDateTime t = getTime(h, no);
            if (t != null && (latest == null || t.isAfter(latest))) {
                latest = t;
            }
        }
        return latest;
    }

    // eventNo -> reads the matching time column.
    private LocalDateTime getTime(Heat h, int eventNo) {
        switch (eventNo) {
            case 1: return h.getScrapChargeStartTime();
            case 2: return h.getScrapChargeEndTime();
            case 3: return h.getMainBlowStartTime();
            case 4: return h.getMainBlowEndTime();
            case 5: return h.getTapTime();
            default: throw new EventRuleException("Geçersiz eventNo: " + eventNo);
        }
    }

    // eventNo -> writes the matching time column.
    private void setTime(Heat h, int eventNo, LocalDateTime time) {
        switch (eventNo) {
            case 1: h.setScrapChargeStartTime(time); break;
            case 2: h.setScrapChargeEndTime(time); break;
            case 3: h.setMainBlowStartTime(time); break;
            case 4: h.setMainBlowEndTime(time); break;
            case 5: h.setTapTime(time); break;
            default: throw new EventRuleException("Geçersiz eventNo: " + eventNo);
        }
    }
}
