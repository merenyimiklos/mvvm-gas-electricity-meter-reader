package hu.merenyimiklos.meterreader.export

import android.content.ContentResolver
import android.net.Uri
import hu.merenyimiklos.meterreader.domain.UsageCalculator
import hu.merenyimiklos.meterreader.model.BillingSettings
import hu.merenyimiklos.meterreader.model.MeterReading
import java.time.LocalDate
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class XlsxExporter(private val contentResolver: ContentResolver) {
    fun export(uri: Uri, readings: List<MeterReading>, settings: BillingSettings) {
        val output = requireNotNull(contentResolver.openOutputStream(uri)) {
            "Nem sikerült megnyitni az export fájlt."
        }
        ZipOutputStream(output.buffered()).use { zip ->
            writeEntry(zip, "[Content_Types].xml", contentTypesXml())
            writeEntry(zip, "_rels/.rels", rootRelationshipsXml())
            writeEntry(zip, "xl/workbook.xml", workbookXml())
            writeEntry(zip, "xl/_rels/workbook.xml.rels", workbookRelationshipsXml())
            writeEntry(zip, "xl/styles.xml", stylesXml())
            writeEntry(zip, "xl/worksheets/sheet1.xml", sheetXml(readings, settings))
        }
    }

    private fun sheetXml(readings: List<MeterReading>, settings: BillingSettings): String {
        val sorted = readings.sortedWith(compareBy<MeterReading> { it.dateEpochDay }.thenBy { it.type.name })
        val consumptionById = UsageCalculator.consumptionByReading(sorted)
        val rows = mutableListOf<List<CellValue>>()
        rows += listOf(
            CellValue.Text("Dátum"), CellValue.Text("Típus"), CellValue.Text("Mérőállás"),
            CellValue.Text("Fogyasztás"), CellValue.Text("Egység"),
            CellValue.Text("Becsült fogyasztási költség (Ft)"),
            CellValue.Text("Becsült fizetendő (Ft)"), CellValue.Text("Megjegyzés")
        )
        sorted.forEach { reading ->
            val consumption = consumptionById[reading.id]
            val costs = UsageCalculator.estimatedPayableForReading(reading, consumption, settings)
            rows += listOf(
                CellValue.Text(LocalDate.ofEpochDay(reading.dateEpochDay).toString()),
                CellValue.Text(reading.type.displayName),
                CellValue.Number(reading.value),
                consumption?.let { CellValue.Number(it) } ?: CellValue.Empty,
                CellValue.Text(reading.type.unit),
                costs.first?.let { CellValue.Number(it) } ?: CellValue.Empty,
                costs.second?.let { CellValue.Number(it) } ?: CellValue.Empty,
                CellValue.Text(reading.note)
            )
        }

        val xmlRows = rows.mapIndexed { rowIndex, cells ->
            val rowNumber = rowIndex + 1
            val cellXml = cells.mapIndexedNotNull { columnIndex, value ->
                if (value == CellValue.Empty) return@mapIndexedNotNull null
                val reference = columnName(columnIndex) + rowNumber
                when (value) {
                    is CellValue.Text -> inlineStringCell(reference, value.value, rowIndex == 0)
                    is CellValue.Number -> numberCell(reference, value.value, rowIndex == 0)
                    CellValue.Empty -> null
                }
            }.joinToString("")
            "<row r=\"" + rowNumber + "\">" + cellXml + "</row>"
        }.joinToString("")

        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
<cols><col min="1" max="1" width="14" customWidth="1"/><col min="2" max="2" width="16" customWidth="1"/><col min="3" max="7" width="24" customWidth="1"/><col min="8" max="8" width="34" customWidth="1"/></cols>
<sheetData>$xmlRows</sheetData>
</worksheet>"""
    }

    private fun inlineStringCell(reference: String, value: String, header: Boolean): String {
        val style = if (header) " s=\"1\"" else ""
        return "<c r=\"" + reference + "\" t=\"inlineStr\"" + style + "><is><t>" + escapeXml(value) + "</t></is></c>"
    }

    private fun numberCell(reference: String, value: Double, header: Boolean): String {
        val style = if (header) " s=\"1\"" else ""
        val number = String.format(Locale.US, "%.3f", value).trimEnd('0').trimEnd('.')
        return "<c r=\"" + reference + "\"" + style + "><v>" + number + "</v></c>"
    }

    private fun columnName(index: Int): String {
        var number = index + 1
        val result = StringBuilder()
        while (number > 0) {
            val remainder = (number - 1) % 26
            result.append(('A'.code + remainder).toChar())
            number = (number - 1) / 26
        }
        return result.reverse().toString()
    }

    private fun escapeXml(value: String): String = value
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        .replace("\"", "&quot;").replace("'", "&apos;")

    private fun writeEntry(zip: ZipOutputStream, path: String, content: String) {
        zip.putNextEntry(ZipEntry(path))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun contentTypesXml() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
<Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>"""

    private fun rootRelationshipsXml() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

    private fun workbookXml() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
<sheets><sheet name="Mérőállások" sheetId="1" r:id="rId1"/></sheets>
</workbook>"""

    private fun workbookRelationshipsXml() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
<Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""

    private fun stylesXml() = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
<fonts count="2"><font><sz val="11"/><name val="Calibri"/></font><font><b/><sz val="11"/><name val="Calibri"/></font></fonts>
<fills count="1"><fill><patternFill patternType="none"/></fill></fills>
<borders count="1"><border/></borders>
<cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
<cellXfs count="2"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/><xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0"/></cellXfs>
</styleSheet>"""

    private sealed interface CellValue {
        data class Text(val value: String) : CellValue
        data class Number(val value: Double) : CellValue
        data object Empty : CellValue
    }

    companion object {
        const val MIME_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    }
}
