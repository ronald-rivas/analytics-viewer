package com.badcompanydevz.data

import com.badcompanydevz.model.AnalyticsEvent
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File

/** Service to export analytics events and properties to an Excel file. */
object ExcelExporter {

    /**
     * Exports the provided events to an Excel file.
     *
     * @param file The target file.
     * @param events The list of events to export.
     * @param userProperties The current user properties to include in each row.
     */
    fun export(file: File, events: List<AnalyticsEvent>, userProperties: Map<String, String>) {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Analytics Events")

        // Header row
        val headerRow = sheet.createRow(0)
        val headers = mutableListOf("Event Name", "Event Title")
        
        // Add all unique parameter keys as headers
        val allParamKeys = events.flatMap { it.params.map { it.first } }.distinct().sorted()
        headers.addAll(allParamKeys)
        
        // Add User Property keys as headers
        val userPropKeys = userProperties.keys.sorted()
        headers.addAll(userPropKeys)
        
        headerRow.createCell(0).setCellValue(headers[0])
        headerRow.createCell(1).setCellValue(headers[1])
        
        var colIdx = 2
        headers.drop(2).forEach { header ->
            headerRow.createCell(colIdx++).setCellValue(header)
        }

        // Data rows
        events.forEachIndexed { index, event ->
            val row = sheet.createRow(index + 1)
            row.createCell(0).setCellValue(event.name)
            row.createCell(1).setCellValue(event.params.find { it.first == "name" }?.second ?: "")

            colIdx = 2
            
            // Event Parameters
            allParamKeys.forEach { key ->
                val value = event.params.find { it.first == key }?.second ?: ""
                row.createCell(colIdx++).setCellValue(value)
            }

            // User Properties
            userPropKeys.forEach { key ->
                val value = userProperties[key] ?: ""
                row.createCell(colIdx++).setCellValue(value)
            }
        }

        // Auto-size columns
        for (i in 0 until headers.size) {
            sheet.autoSizeColumn(i)
        }

        file.outputStream().use { workbook.write(it) }
        workbook.close()
    }
}
