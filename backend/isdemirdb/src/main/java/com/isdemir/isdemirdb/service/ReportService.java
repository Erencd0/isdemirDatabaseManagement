package com.isdemir.isdemirdb.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import com.isdemir.isdemirdb.entity.Heat;
import com.isdemir.isdemirdb.repository.HeatRepository;

import lombok.RequiredArgsConstructor;

// Builds the .xlsx of the "Rapor Olustur" button. The controller only passes the interval on.
// A heat belongs to the day of its dokum_zamani; a heat without one is not in the report.
// The report is NOT filtered by the kv1/kv2/kv3 role - like the timeline it shows all
// converters, since a report of "how the plant ran" is pointless with a third of it missing.
// Picking a single converter is left to the reader: Detay carries an Excel filter on its
// "Konverter" column, so it happens in the file and not in another request.
@Service
@RequiredArgsConstructor
public class ReportService {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    // Only used when the interval has no heat at all, so the sheet is not a bare column.
    private static final List<Integer> DEFAULT_CONVERTERS = List.of(1, 2, 3);

    private static final int HEADER_ROW = 2; // Tarih | Konverterler | Toplam
    private static final int KV_ROW = 3;     //          KV1 KV2 KV3

    // The sheet is read on a shop floor screen, so everything is a size up from Excel's
    // default (11pt / 20pt rows / ~2000 wide columns). These three numbers are the whole
    // "how big is it" setting: RANGE_FONT_SIZE is the interval line on top, FONT_SIZE the
    // table itself.
    private static final short RANGE_FONT_SIZE = 16;
    private static final short FONT_SIZE = 14;
    private static final float ROW_HEIGHT = 26;

    // The same red the screen uses (brand-700 / brand-50 of index.css), so the file and the
    // application look like the same thing. Change these two and both sheets follow.
    private static final byte[] HEAD_FILL = { (byte) 0xB9, (byte) 0x1C, (byte) 0x1C };
    private static final byte[] SOFT_FILL = { (byte) 0xFE, (byte) 0xF2, (byte) 0xF2 };
    private static final byte[] WHITE = { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF };

    private final HeatRepository heatRepository;

    public byte[] build(LocalDate start, LocalDate end) throws IOException {
        // Between is inclusive on both sides: LocalTime.MAX keeps the last day whole.
        List<Heat> heats = heatRepository.findByTapTimeBetweenOrderByTapTimeAsc(
                start.atStartOfDay(), end.atTime(LocalTime.MAX));

        try (XSSFWorkbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            writeSummary(workbook, start, end, heats);
            writeDetail(workbook, start, end, heats);
            workbook.write(out);
            return out.toByteArray();
        }
    }

    // "Ozet": the chosen interval on top, then one row per day of it, one column per
    // converter, totals on the right (per day) and at the bottom (per converter).
    private void writeSummary(Workbook workbook, LocalDate start, LocalDate end,
            List<Heat> heats) {

        // The columns are the converters that actually ran in this interval: a converter with
        // no heat gets no column at all, and the "Konverterler" title shrinks with them.
        List<Integer> converters = heats.stream()
                .map(Heat::getConverterNo)
                .distinct()
                .sorted()
                .toList();
        if (converters.isEmpty()) {
            converters = DEFAULT_CONVERTERS; // nothing ran at all -> an all-zero table
        }
        // day -> converter -> how many heats
        Map<LocalDate, Map<Integer, Long>> counts = heats.stream().collect(
                Collectors.groupingBy(h -> h.getTapTime().toLocalDate(),
                        Collectors.groupingBy(Heat::getConverterNo, Collectors.counting())));

        int lastKvColumn = converters.size();
        int totalColumn = lastKvColumn + 1;

        Sheet sheet = workbook.createSheet("Özet");
        sheet.setDefaultRowHeightInPoints(ROW_HEIGHT);
        CellStyle rangeLabel = style(workbook, RANGE_FONT_SIZE, true, false,
                HorizontalAlignment.LEFT);
        CellStyle range = style(workbook, RANGE_FONT_SIZE, false, false, HorizontalAlignment.LEFT);
        CellStyle head = fill(style(workbook, FONT_SIZE, true, true, HorizontalAlignment.CENTER),
                HEAD_FILL, true);
        CellStyle day = style(workbook, FONT_SIZE, false, true, HorizontalAlignment.LEFT);
        CellStyle count = style(workbook, FONT_SIZE, false, true, HorizontalAlignment.CENTER);
        // The bottom row is a total, not a reading: a light band tells them apart at a glance.
        CellStyle total = fill(style(workbook, FONT_SIZE, true, true, HorizontalAlignment.CENTER),
                SOFT_FILL, false);

        // Label and value in two cells, so the interval is readable without widening column A.
        Row rangeRow = newRow(sheet, 0);
        cell(rangeRow, 0, "Rapor Aralığı:", rangeLabel);
        cell(rangeRow, 1, start.format(DAY) + " - " + end.format(DAY), range);

        // "Tarih" and "Toplam" are two rows tall; "Konverterler" sits over the KV columns and
        // is exactly as wide as there are converters.
        Row header = newRow(sheet, HEADER_ROW);
        Row kvHeader = newRow(sheet, KV_ROW);

        cell(header, 0, "Tarih", head);
        cell(kvHeader, 0, "", head);
        merge(sheet, HEADER_ROW, KV_ROW, 0, 0);

        for (int i = 0; i < converters.size(); i++) {
            // Only the first cell carries the text, the rest exist so the merged band keeps
            // its borders.
            cell(header, 1 + i, i == 0 ? "Konverterler" : "", head);
            cell(kvHeader, 1 + i, "KV" + converters.get(i), head);
        }
        merge(sheet, HEADER_ROW, HEADER_ROW, 1, lastKvColumn);

        cell(header, totalColumn, "Toplam", head);
        cell(kvHeader, totalColumn, "", head);
        merge(sheet, HEADER_ROW, KV_ROW, totalColumn, totalColumn);

        // One row per day of the interval, days without a heat included.
        long[] converterTotals = new long[converters.size()];
        int rowIndex = KV_ROW + 1;
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            Map<Integer, Long> ofDay = counts.getOrDefault(date, Map.of());
            Row row = newRow(sheet, rowIndex++);
            cell(row, 0, date.format(DAY), day);

            long dayTotal = 0;
            for (int i = 0; i < converters.size(); i++) {
                long value = ofDay.getOrDefault(converters.get(i), 0L);
                number(row, 1 + i, value, count);
                converterTotals[i] += value;
                dayTotal += value;
            }
            number(row, totalColumn, dayTotal, count);
        }

        // Bottom row: the total of each converter, and the whole interval on the corner.
        Row totals = newRow(sheet, rowIndex);
        cell(totals, 0, "Toplam", total);
        long grandTotal = 0;
        for (int i = 0; i < converters.size(); i++) {
            number(totals, 1 + i, converterTotals[i], total);
            grandTotal += converterTotals[i];
        }
        number(totals, totalColumn, grandTotal, total);

        sheet.setColumnWidth(0, 4600); // the date column, "01.01.2026" plus a little air
        for (int c = 1; c <= totalColumn; c++) {
            sheet.setColumnWidth(c, 3600);
        }
    }

    // The process times of dokum_tablosu in their real order. kayit_zamani is left out on
    // purpose: it is when the row was typed in, not something that happened to the heat.
    private static final String[] TIME_LABELS = {
            "Hurda Şarj Başlama",
            "Hurda Şarj Bitiş",
            "Ana Üflemeye Başlama",
            "Ana Üfleme Bitiş",
            "Döküm Zamanı",
    };

    private LocalDateTime[] times(Heat heat) {
        return new LocalDateTime[] {
                heat.getScrapChargeStartTime(),
                heat.getScrapChargeEndTime(),
                heat.getMainBlowStartTime(),
                heat.getMainBlowEndTime(),
                heat.getTapTime(),
        };
    }

    private static final int CONVERTER_COLUMN = 1;   // Dokum No | Konverter | Tarih | times...
    private static final int DETAIL_HEADER_ROW = 2;  // same place as Ozet's, row 0 is the interval

    // "Detay": every heat of the interval, one per row, oldest first (that is the order the
    // query returns). Same heats the summary counts, so the two sheets always agree.
    // The converter is picked inside the file: Excel's own filter is set on the "Konverter"
    // column only, so that is the single header with an arrow on it. The user opens it and
    // ticks KV1/KV2/KV3; Excel hides the whole row of every other converter. It filters a copy
    // of the file just as well, and nothing about it goes through the backend.
    private void writeDetail(Workbook workbook, LocalDate start, LocalDate end, List<Heat> heats) {
        Sheet sheet = workbook.createSheet("Detay");
        sheet.setDefaultRowHeightInPoints(ROW_HEIGHT);

        CellStyle rangeLabel = style(workbook, RANGE_FONT_SIZE, true, false,
                HorizontalAlignment.LEFT);
        CellStyle range = style(workbook, RANGE_FONT_SIZE, false, false, HorizontalAlignment.LEFT);
        CellStyle head = fill(style(workbook, FONT_SIZE, true, true, HorizontalAlignment.CENTER),
                HEAD_FILL, true);
        CellStyle body = style(workbook, FONT_SIZE, false, true, HorizontalAlignment.CENTER);
        // The filtered column stands out from the rest, so the arrow is not hunted for.
        CellStyle converter = fill(style(workbook, FONT_SIZE, false, true,
                HorizontalAlignment.CENTER), SOFT_FILL, false);

        // The same interval line Ozet opens with, so the two sheets read as one report.
        Row rangeRow = newRow(sheet, 0);
        cell(rangeRow, 0, "Rapor Aralığı:", rangeLabel);
        cell(rangeRow, 1, start.format(DAY) + " - " + end.format(DAY), range);

        Row header = newRow(sheet, DETAIL_HEADER_ROW);
        cell(header, 0, "Döküm No", head);
        cell(header, CONVERTER_COLUMN, "Konverter", head);
        cell(header, 2, "Tarih", head);
        for (int i = 0; i < TIME_LABELS.length; i++) {
            cell(header, 3 + i, TIME_LABELS[i], head);
        }
        int lastColumn = 2 + TIME_LABELS.length;

        int rowIndex = DETAIL_HEADER_ROW + 1;
        for (Heat heat : heats) {
            Row row = newRow(sheet, rowIndex++);
            LocalDate day = heat.getTapTime().toLocalDate();
            number(row, 0, heat.getHeatNo(), body);
            // "KV1" and not a bare 1, so the filter list reads the way the screen does.
            cell(row, CONVERTER_COLUMN, "KV" + heat.getConverterNo(), converter);
            cell(row, 2, day.format(DAY), body);

            LocalDateTime[] times = times(heat);
            for (int i = 0; i < times.length; i++) {
                cell(row, 3 + i, clock(times[i]), body);
            }
        }

        // Only the Konverter column carries the filter, so only its header gets an arrow.
        // Excel still hides the whole row, which is what the filter is for.
        // ponytail: sorting from that arrow would move the Konverter column alone; if anyone
        // ever sorts from there instead of filtering, widen the range to every column.
        sheet.setAutoFilter(new CellRangeAddress(DETAIL_HEADER_ROW,
                Math.max(sheet.getLastRowNum(), DETAIL_HEADER_ROW),
                CONVERTER_COLUMN, CONVERTER_COLUMN));
        // The interval and the header stay put while a long list is scrolled.
        sheet.createFreezePane(0, DETAIL_HEADER_ROW + 1);

        sheet.setColumnWidth(0, 4200);
        sheet.setColumnWidth(CONVERTER_COLUMN, 4000); // a little air for the filter arrow
        sheet.setColumnWidth(2, 4600);
        for (int c = 3; c <= lastColumn; c++) {
            sheet.setColumnWidth(c, 5800);
        }
    }

    // A time of the heat, as it is shown in the Detay row: only the clock, the day is already
    // in the Tarih column. Empty time -> empty cell.
    // ponytail: a heat that runs over midnight (charging 23:50, tapping 00:30) shows both
    // times as plain clocks; print the date next to them if that ever gets misread.
    private String clock(LocalDateTime time) {
        return time == null ? "" : time.format(TIME);
    }

    // ---- Internal helpers ----

    // The sheet only needs a size, bold on/off and borders on/off; created once and shared,
    // since a style per cell would blow up the file.
    private CellStyle style(Workbook workbook, short fontSize, boolean isBold, boolean bordered,
            HorizontalAlignment alignment) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(isBold);
        font.setFontHeightInPoints(fontSize);
        style.setFont(font);
        style.setAlignment(alignment);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        if (bordered) {
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
        }
        return style;
    }

    // Every row of both sheets is created here. The height is written on the row itself instead
    // of being left to the sheet default: with only a default, Excel sizes a row it does not
    // find one for from the font, and the table starts shrinking somewhere down the list.
    private Row newRow(Sheet sheet, int index) {
        Row row = sheet.createRow(index);
        row.setHeightInPoints(ROW_HEIGHT);
        return row;
    }

    // Paints a style's background; whiteText is for the dark band, where black would not read.
    private CellStyle fill(CellStyle style, byte[] rgb, boolean whiteText) {
        XSSFCellStyle painted = (XSSFCellStyle) style;
        painted.setFillForegroundColor(new XSSFColor(rgb, null));
        painted.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        if (whiteText) {
            painted.getFont().setColor(new XSSFColor(WHITE, null));
        }
        return painted;
    }

    private void merge(Sheet sheet, int firstRow, int lastRow, int firstColumn, int lastColumn) {
        if (firstRow != lastRow || firstColumn != lastColumn) {
            sheet.addMergedRegion(
                    new CellRangeAddress(firstRow, lastRow, firstColumn, lastColumn));
        }
    }

    private void cell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        if (style != null) {
            cell.setCellStyle(style);
        }
    }

    private void number(Row row, int column, long value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }
}
