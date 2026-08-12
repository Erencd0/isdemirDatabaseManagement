package com.isdemir.isdemirdb.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import com.isdemir.isdemirdb.entity.Heat;
import com.isdemir.isdemirdb.exception.InvalidDataException;
import com.isdemir.isdemirdb.repository.HeatRepository;
import com.isdemir.isdemirdb.service.ReportService;

// No Spring context: only the repository is faked, the rest is called directly.
// Row 0 is the interval, rows 2-3 are the header, so the first day sits on row 4.
class ReportControllerTest {

    private static final int KV_ROW = 3;
    private static final int FIRST_DAY_ROW = 4;
    private static final int DETAIL_HEADER_ROW = 2; // Detay opens with the interval too

    private final HeatRepository heatRepository = mock(HeatRepository.class);
    private final ReportController controller =
            new ReportController(new ReportService(heatRepository));

    private Heat heat(int converterNo, LocalDateTime tapTime) {
        Heat heat = new Heat();
        heat.setConverterNo(converterNo);
        heat.setHeatNo(6_000_000 + converterNo * 100_000 + tapTime.getDayOfMonth());
        heat.setTapTime(tapTime);
        return heat;
    }

    private XSSFWorkbook workbookOf(LocalDate start, LocalDate end) throws Exception {
        byte[] file = controller.report(start, end).getBody();
        assertNotNull(file);
        return new XSSFWorkbook(new ByteArrayInputStream(file));
    }

    private Sheet summaryOf(LocalDate start, LocalDate end) throws Exception {
        return workbookOf(start, end).getSheet("Özet");
    }

    private String text(Sheet sheet, int row, int column) {
        return sheet.getRow(row).getCell(column).getStringCellValue();
    }

    private double value(Sheet sheet, int row, int column) {
        return sheet.getRow(row).getCell(column).getNumericCellValue();
    }

    private String mergeAt(Sheet sheet, int row, int column) {
        return sheet.getMergedRegions().stream()
                .filter(r -> r.getFirstRow() == row && r.getFirstColumn() == column)
                .findFirst()
                .map(CellRangeAddress::formatAsString)
                .orElse("");
    }

    @Test
    void countsHeatsPerDayAndPerConverter() throws Exception {
        when(heatRepository.findByTapTimeBetweenOrderByTapTimeAsc(any(), any())).thenReturn(List.of(
                heat(1, LocalDateTime.of(2026, 1, 1, 8, 0)),
                heat(1, LocalDateTime.of(2026, 1, 1, 20, 0)),
                heat(2, LocalDateTime.of(2026, 1, 3, 10, 0)),
                heat(3, LocalDateTime.of(2026, 1, 3, 23, 59))));

        Sheet sheet = summaryOf(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 3));

        assertEquals("01.01.2026 - 03.01.2026", text(sheet, 0, 1));
        assertEquals("Konverterler", text(sheet, 2, 1));
        assertEquals("KV2", text(sheet, KV_ROW, 2));

        // 01.01 -> two heats on KV1, none elsewhere; the day total is on the right
        assertEquals(2, value(sheet, FIRST_DAY_ROW, 1));
        assertEquals(0, value(sheet, FIRST_DAY_ROW, 2));
        assertEquals(2, value(sheet, FIRST_DAY_ROW, 4));
        // 02.01 has no heat at all but keeps its row
        assertEquals("02.01.2026", text(sheet, FIRST_DAY_ROW + 1, 0));
        assertEquals(0, value(sheet, FIRST_DAY_ROW + 1, 4));
        // 03.01 -> one heat on KV3, 23:59 still belongs to that day
        assertEquals(1, value(sheet, FIRST_DAY_ROW + 2, 3));
        // Bottom row: per converter totals, then the grand total
        assertEquals("Toplam", text(sheet, FIRST_DAY_ROW + 3, 0));
        assertEquals(2, value(sheet, FIRST_DAY_ROW + 3, 1));
        assertEquals(1, value(sheet, FIRST_DAY_ROW + 3, 2));
        assertEquals(1, value(sheet, FIRST_DAY_ROW + 3, 3));
        assertEquals(4, value(sheet, FIRST_DAY_ROW + 3, 4));
    }

    // A converter without a single heat in the interval gets no column at all, and the
    // "Konverterler" title shrinks to the converters that are left.
    @Test
    void columnsFollowTheConvertersThatRan() throws Exception {
        when(heatRepository.findByTapTimeBetweenOrderByTapTimeAsc(any(), any())).thenReturn(List.of(
                heat(1, LocalDateTime.of(2026, 1, 1, 8, 0)),
                heat(3, LocalDateTime.of(2026, 1, 1, 9, 0))));

        Sheet sheet = summaryOf(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1));

        assertEquals("KV1", text(sheet, KV_ROW, 1));
        assertEquals("KV3", text(sheet, KV_ROW, 2)); // no KV2 column in between
        assertEquals("Toplam", text(sheet, 2, 3));
        assertEquals("B3:C3", mergeAt(sheet, 2, 1)); // the title spans two converters, not three
        assertEquals(1, value(sheet, FIRST_DAY_ROW, 2));
        assertEquals(2, value(sheet, FIRST_DAY_ROW, 3));
    }

    // Detay: one row per heat with every process time, the same heats the summary counted.
    @Test
    void listsEveryHeatOnTheDetailSheet() throws Exception {
        Heat first = heat(1, LocalDateTime.of(2026, 1, 1, 8, 5));
        first.setScrapChargeStartTime(LocalDateTime.of(2026, 1, 1, 7, 0));
        first.setScrapChargeEndTime(LocalDateTime.of(2026, 1, 1, 7, 20));
        first.setMainBlowStartTime(LocalDateTime.of(2026, 1, 1, 7, 30));
        // main blow end left empty on purpose
        // A heat that started the evening before and was tapped after midnight.
        Heat overnight = heat(3, LocalDateTime.of(2026, 1, 3, 0, 30));
        overnight.setScrapChargeStartTime(LocalDateTime.of(2026, 1, 2, 23, 50));

        when(heatRepository.findByTapTimeBetweenOrderByTapTimeAsc(any(), any()))
                .thenReturn(List.of(first, overnight));

        Sheet sheet = workbookOf(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 3))
                .getSheet("Detay");

        assertEquals("01.01.2026 - 03.01.2026", text(sheet, 0, 1)); // same interval line as Özet
        assertEquals("Döküm No", text(sheet, DETAIL_HEADER_ROW, 0));
        assertEquals("Konverter", text(sheet, DETAIL_HEADER_ROW, 1));
        assertEquals("Tarih", text(sheet, DETAIL_HEADER_ROW, 2));
        assertEquals("Hurda Şarj Başlama", text(sheet, DETAIL_HEADER_ROW, 3));
        assertEquals("Hurda Şarj Bitiş", text(sheet, DETAIL_HEADER_ROW, 4));
        assertEquals("Ana Üflemeye Başlama", text(sheet, DETAIL_HEADER_ROW, 5));
        assertEquals("Ana Üfleme Bitiş", text(sheet, DETAIL_HEADER_ROW, 6));
        assertEquals("Döküm Zamanı", text(sheet, DETAIL_HEADER_ROW, 7));

        int firstRow = DETAIL_HEADER_ROW + 1;
        assertEquals(6100001, value(sheet, firstRow, 0));
        assertEquals("KV1", text(sheet, firstRow, 1));
        assertEquals("01.01.2026", text(sheet, firstRow, 2));
        assertEquals("07:00", text(sheet, firstRow, 3));
        assertEquals("07:20", text(sheet, firstRow, 4));
        assertEquals("07:30", text(sheet, firstRow, 5));
        assertEquals("", text(sheet, firstRow, 6)); // no main blow end -> empty cell
        assertEquals("08:05", text(sheet, firstRow, 7));

        // The heat belongs to the day it was tapped; every time cell is a plain clock.
        assertEquals("KV3", text(sheet, firstRow + 1, 1));
        assertEquals("03.01.2026", text(sheet, firstRow + 1, 2));
        assertEquals("23:50", text(sheet, firstRow + 1, 3));
        assertEquals("00:30", text(sheet, firstRow + 1, 7));
        // and nothing after the last heat
        assertEquals(firstRow + 1, sheet.getLastRowNum());
    }

    // The converter is picked inside the file: Excel's own filter sits on the "Konverter"
    // column alone, so that is the only header with an arrow and no other column can be
    // filtered by accident.
    @Test
    void onlyTheConverterColumnCarriesTheFilter() throws Exception {
        when(heatRepository.findByTapTimeBetweenOrderByTapTimeAsc(any(), any())).thenReturn(List.of(
                heat(1, LocalDateTime.of(2026, 1, 1, 8, 0)),
                heat(2, LocalDateTime.of(2026, 1, 1, 9, 0))));

        Sheet sheet = workbookOf(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1))
                .getSheet("Detay");

        // Column B only, from its header down to the last heat.
        assertEquals("B3:B5", ((XSSFSheet) sheet).getCTWorksheet().getAutoFilter().getRef());
        assertEquals("KV1", text(sheet, DETAIL_HEADER_ROW + 1, 1));
        assertEquals("KV2", text(sheet, DETAIL_HEADER_ROW + 2, 1));
    }

    // An interval without a single heat still opens in Excel: the filter cannot point at a
    // row that is not there.
    @Test
    void emptyIntervalStillHasAValidFilter() throws Exception {
        when(heatRepository.findByTapTimeBetweenOrderByTapTimeAsc(any(), any()))
                .thenReturn(List.of());

        Sheet sheet = workbookOf(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1))
                .getSheet("Detay");

        // Just the header cell (a one cell range writes itself without the colon)
        assertEquals("B3", ((XSSFSheet) sheet).getCTWorksheet().getAutoFilter().getRef());
    }

    @Test
    void rejectsReversedInterval() {
        assertThrows(InvalidDataException.class, () -> controller
                .report(LocalDate.of(2026, 1, 31), LocalDate.of(2026, 1, 1)));
    }
}
