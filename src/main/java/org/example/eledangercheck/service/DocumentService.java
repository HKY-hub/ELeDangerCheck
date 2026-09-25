package org.example.eledangercheck.service;

import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xwpf.usermodel.*;
import org.example.eledangercheck.entity.Disclosure;
import org.example.eledangercheck.entity.Hazard;
import org.example.eledangercheck.entity.SafetyMeasure;
import org.example.eledangercheck.entity.Task;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class DocumentService {

    @Value("${app.document.output-path:./documents}")
    private String outputPath;

    public byte[] generateWordDisclosureCard(Disclosure disclosure, Task task, 
                                             List<Hazard> hazards, List<SafetyMeasure> measures) throws IOException {
        XWPFDocument document = new XWPFDocument();

        XWPFParagraph title = document.createParagraph();
        title.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun titleRun = title.createRun();
        titleRun.setText("电力作业安全交底卡");
        titleRun.setFontSize(18);
        titleRun.setBold(true);
        titleRun.addBreak();

        XWPFParagraph infoSection = document.createParagraph();
        infoSection.setSpacingAfter(200);
        
        XWPFTable infoTable = document.createTable(3, 4);
        infoTable.setWidth(8000);
        
        addTableCell(infoTable, "交底编号", ParagraphAlignment.CENTER, true);
        addTableCell(infoTable, disclosure.getDisclosureNo() != null ? disclosure.getDisclosureNo() : "-", ParagraphAlignment.LEFT, false);
        addTableCell(infoTable, "任务名称", ParagraphAlignment.CENTER, true);
        addTableCell(infoTable, task.getTaskName() != null ? task.getTaskName() : "-", ParagraphAlignment.LEFT, false);
        
        addTableCell(infoTable, "电压等级", ParagraphAlignment.CENTER, true);
        addTableCell(infoTable, task.getVoltageLevel() != null ? task.getVoltageLevel() : "-", ParagraphAlignment.LEFT, false);
        addTableCell(infoTable, "设备类型", ParagraphAlignment.CENTER, true);
        addTableCell(infoTable, task.getEquipmentType() != null ? task.getEquipmentType() : "-", ParagraphAlignment.LEFT, false);
        
        addTableCell(infoTable, "作业类型", ParagraphAlignment.CENTER, true);
        addTableCell(infoTable, task.getWorkType() != null ? task.getWorkType() : "-", ParagraphAlignment.LEFT, false);
        addTableCell(infoTable, "作业位置", ParagraphAlignment.CENTER, true);
        addTableCell(infoTable, task.getEnvConditions() != null ? task.getEnvConditions() : "-", ParagraphAlignment.LEFT, false);

        XWPFParagraph hazardsTitle = document.createParagraph();
        hazardsTitle.setSpacingBefore(200);
        XWPFRun hazardsTitleRun = hazardsTitle.createRun();
        hazardsTitleRun.setText("一、危险点识别");
        hazardsTitleRun.setFontSize(14);
        hazardsTitleRun.setBold(true);

        if (hazards != null && !hazards.isEmpty()) {
            for (int i = 0; i < hazards.size(); i++) {
                Hazard hazard = hazards.get(i);
                XWPFParagraph hazardItem = document.createParagraph();
                hazardItem.setIndentationFirstLine(400);
                XWPFRun hazardRun = hazardItem.createRun();
                hazardRun.setText((i + 1) + ". " + hazard.getHazardName());
                hazardRun.setBold(true);
                
                if (hazard.getDescription() != null) {
                    XWPFParagraph hazardDesc = document.createParagraph();
                    hazardDesc.setIndentationFirstLine(400);
                    XWPFRun descRun = hazardDesc.createRun();
                    descRun.setText("   风险描述：" + hazard.getDescription());
                }
                
                if (hazard.getHazardLevel() != null) {
                    XWPFRun levelRun = hazardItem.createRun();
                    levelRun.setText("  [" + translateLevel(hazard.getHazardLevel()) + "]");
                    levelRun.setColor(getLevelColor(hazard.getHazardLevel()));
                }
            }
        } else {
            XWPFParagraph noHazards = document.createParagraph();
            noHazards.setIndentationFirstLine(400);
            noHazards.createRun().setText("暂无危险点");
        }

        XWPFParagraph measuresTitle = document.createParagraph();
        measuresTitle.setSpacingBefore(200);
        XWPFRun measuresTitleRun = measuresTitle.createRun();
        measuresTitleRun.setText("二、控制措施");
        measuresTitleRun.setFontSize(14);
        measuresTitleRun.setBold(true);

        if (measures != null && !measures.isEmpty()) {
            for (int i = 0; i < measures.size(); i++) {
                SafetyMeasure measure = measures.get(i);
                XWPFParagraph measureItem = document.createParagraph();
                measureItem.setIndentationFirstLine(400);
                XWPFRun measureRun = measureItem.createRun();
                measureRun.setText((i + 1) + ". " + measure.getMeasureName());
                
                if (measure.getMeasureDesc() != null) {
                    XWPFParagraph measureDesc = document.createParagraph();
                    measureDesc.setIndentationFirstLine(400);
                    XWPFRun descRun = measureDesc.createRun();
                    descRun.setText("   " + measure.getMeasureDesc());
                }
            }
        } else {
            XWPFParagraph noMeasures = document.createParagraph();
            noMeasures.setIndentationFirstLine(400);
            noMeasures.createRun().setText("暂无控制措施");
        }

        XWPFParagraph contentTitle = document.createParagraph();
        contentTitle.setSpacingBefore(200);
        XWPFRun contentTitleRun = contentTitle.createRun();
        contentTitleRun.setText("三、交底内容");
        contentTitleRun.setFontSize(14);
        contentTitleRun.setBold(true);

        if (disclosure.getContent() != null) {
            XWPFParagraph content = document.createParagraph();
            content.setIndentationFirstLine(400);
            content.createRun().setText(disclosure.getContent());
        } else {
            XWPFParagraph noContent = document.createParagraph();
            noContent.setIndentationFirstLine(400);
            noContent.createRun().setText("暂无交底内容");
        }

        XWPFParagraph emergencyTitle = document.createParagraph();
        emergencyTitle.setSpacingBefore(200);
        XWPFRun emergencyTitleRun = emergencyTitle.createRun();
        emergencyTitleRun.setText("四、应急信息");
        emergencyTitleRun.setFontSize(14);
        emergencyTitleRun.setBold(true);

        XWPFParagraph emergencyRoute = document.createParagraph();
        emergencyRoute.setIndentationFirstLine(400);
        emergencyRoute.createRun().setText("应急路线：" + (disclosure.getEmergencyRoute() != null ? disclosure.getEmergencyRoute() : "-"));

        XWPFParagraph emergencyContact = document.createParagraph();
        emergencyContact.setIndentationFirstLine(400);
        emergencyContact.createRun().setText("应急联系人：" + (disclosure.getEmergencyContact() != null ? disclosure.getEmergencyContact() : "-"));

        XWPFParagraph signSection = document.createParagraph();
        signSection.setSpacingBefore(400);
        XWPFRun signSectionRun = signSection.createRun();
        signSectionRun.setText("五、签字确认");
        signSectionRun.setFontSize(14);
        signSectionRun.setBold(true);

        XWPFTable signTable = document.createTable(2, 3);
        signTable.setWidth(8000);
        
        addTableCell(signTable, "工作负责人", ParagraphAlignment.CENTER, true);
        addTableCell(signTable, "作业人员", ParagraphAlignment.CENTER, true);
        addTableCell(signTable, "签字日期", ParagraphAlignment.CENTER, true);
        
        addTableCell(signTable, "____________________", ParagraphAlignment.CENTER, false);
        addTableCell(signTable, "____________________", ParagraphAlignment.CENTER, false);
        addTableCell(signTable, "____________________", ParagraphAlignment.CENTER, false);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        document.write(outputStream);
        document.close();

        return outputStream.toByteArray();
    }

    public byte[] generatePdfDisclosureCard(Disclosure disclosure, Task task,
                                            List<Hazard> hazards, List<SafetyMeasure> measures) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(outputStream);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        PdfFont titleFont = PdfFontFactory.createFont("STSong-Light", "UniGB-UCS2-H", PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
        PdfFont normalFont = PdfFontFactory.createFont("STSong-Light", "UniGB-UCS2-H", PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);

        Paragraph title = new Paragraph("电力作业安全交底卡")
                .setFont(titleFont)
                .setFontSize(18)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(title);

        float[] infoColumnWidths = {80, 150, 80, 150};
        Table infoTable = new Table(UnitValue.createPointArray(infoColumnWidths));
        infoTable.setWidth(UnitValue.createPercentValue(100));

        addPdfTableCell(infoTable, "交底编号", titleFont, true, TextAlignment.CENTER);
        addPdfTableCell(infoTable, disclosure.getDisclosureNo() != null ? disclosure.getDisclosureNo() : "-", normalFont, false, TextAlignment.LEFT);
        addPdfTableCell(infoTable, "任务名称", titleFont, true, TextAlignment.CENTER);
        addPdfTableCell(infoTable, task.getTaskName() != null ? task.getTaskName() : "-", normalFont, false, TextAlignment.LEFT);
        
        addPdfTableCell(infoTable, "电压等级", titleFont, true, TextAlignment.CENTER);
        addPdfTableCell(infoTable, task.getVoltageLevel() != null ? task.getVoltageLevel() : "-", normalFont, false, TextAlignment.LEFT);
        addPdfTableCell(infoTable, "设备类型", titleFont, true, TextAlignment.CENTER);
        addPdfTableCell(infoTable, task.getEquipmentType() != null ? task.getEquipmentType() : "-", normalFont, false, TextAlignment.LEFT);
        
        addPdfTableCell(infoTable, "作业类型", titleFont, true, TextAlignment.CENTER);
        addPdfTableCell(infoTable, task.getWorkType() != null ? task.getWorkType() : "-", normalFont, false, TextAlignment.LEFT);
        addPdfTableCell(infoTable, "作业位置", titleFont, true, TextAlignment.CENTER);
        addPdfTableCell(infoTable, task.getEnvConditions() != null ? task.getEnvConditions() : "-", normalFont, false, TextAlignment.LEFT);

        document.add(infoTable);

        Paragraph hazardsTitle = new Paragraph("一、危险点识别")
                .setFont(titleFont)
                .setFontSize(14)
                .setBold()
                .setMarginTop(20)
                .setMarginBottom(10);
        document.add(hazardsTitle);

        if (hazards != null && !hazards.isEmpty()) {
            for (int i = 0; i < hazards.size(); i++) {
                Hazard hazard = hazards.get(i);
                Paragraph hazardItem = new Paragraph((i + 1) + ". " + hazard.getHazardName())
                        .setFont(normalFont)
                        .setFontSize(12)
                        .setBold();
                
                if (hazard.getHazardLevel() != null) {
                    hazardItem.add(" [" + translateLevel(hazard.getHazardLevel()) + "]")
                            .setFontColor(getPdfLevelColor(hazard.getHazardLevel()));
                }
                document.add(hazardItem);

                if (hazard.getDescription() != null) {
                    Paragraph hazardDesc = new Paragraph("   风险描述：" + hazard.getDescription())
                            .setFont(normalFont)
                            .setFontSize(11);
                    document.add(hazardDesc);
                }
            }
        } else {
            document.add(new Paragraph("暂无危险点").setFont(normalFont).setFontSize(12));
        }

        Paragraph measuresTitle = new Paragraph("二、控制措施")
                .setFont(titleFont)
                .setFontSize(14)
                .setBold()
                .setMarginTop(20)
                .setMarginBottom(10);
        document.add(measuresTitle);

        if (measures != null && !measures.isEmpty()) {
            for (int i = 0; i < measures.size(); i++) {
                SafetyMeasure measure = measures.get(i);
                Paragraph measureItem = new Paragraph((i + 1) + ". " + measure.getMeasureName())
                        .setFont(normalFont)
                        .setFontSize(12);
                document.add(measureItem);

                if (measure.getMeasureDesc() != null) {
                    Paragraph measureDesc = new Paragraph("   " + measure.getMeasureDesc())
                            .setFont(normalFont)
                            .setFontSize(11);
                    document.add(measureDesc);
                }
            }
        } else {
            document.add(new Paragraph("暂无控制措施").setFont(normalFont).setFontSize(12));
        }

        Paragraph contentTitle = new Paragraph("三、交底内容")
                .setFont(titleFont)
                .setFontSize(14)
                .setBold()
                .setMarginTop(20)
                .setMarginBottom(10);
        document.add(contentTitle);

        if (disclosure.getContent() != null) {
            Paragraph content = new Paragraph(disclosure.getContent())
                    .setFont(normalFont)
                    .setFontSize(12);
            document.add(content);
        } else {
            document.add(new Paragraph("暂无交底内容").setFont(normalFont).setFontSize(12));
        }

        Paragraph emergencyTitle = new Paragraph("四、应急信息")
                .setFont(titleFont)
                .setFontSize(14)
                .setBold()
                .setMarginTop(20)
                .setMarginBottom(10);
        document.add(emergencyTitle);

        document.add(new Paragraph("应急路线：" + (disclosure.getEmergencyRoute() != null ? disclosure.getEmergencyRoute() : "-"))
                .setFont(normalFont).setFontSize(12));
        document.add(new Paragraph("应急联系人：" + (disclosure.getEmergencyContact() != null ? disclosure.getEmergencyContact() : "-"))
                .setFont(normalFont).setFontSize(12));

        Paragraph signTitle = new Paragraph("五、签字确认")
                .setFont(titleFont)
                .setFontSize(14)
                .setBold()
                .setMarginTop(20)
                .setMarginBottom(10);
        document.add(signTitle);

        float[] signColumnWidths = {120, 120, 80};
        Table signTable = new Table(UnitValue.createPointArray(signColumnWidths));
        signTable.setWidth(UnitValue.createPercentValue(100));

        addPdfTableCell(signTable, "工作负责人", titleFont, true, TextAlignment.CENTER);
        addPdfTableCell(signTable, "作业人员", titleFont, true, TextAlignment.CENTER);
        addPdfTableCell(signTable, "签字日期", titleFont, true, TextAlignment.CENTER);
        
        addPdfTableCell(signTable, "____________________", normalFont, false, TextAlignment.CENTER);
        addPdfTableCell(signTable, "____________________", normalFont, false, TextAlignment.CENTER);
        addPdfTableCell(signTable, "____________________", normalFont, false, TextAlignment.CENTER);

        document.add(signTable);

        document.close();

        return outputStream.toByteArray();
    }

    private void addTableCell(XWPFTable table, String text, ParagraphAlignment alignment, boolean isHeader) {
        XWPFTableCell cell = table.createRow().getCell(0);
        XWPFParagraph paragraph = cell.getParagraphs().get(0);
        if (paragraph == null) {
            paragraph = cell.addParagraph();
        }
        paragraph.setAlignment(alignment);
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        if (isHeader) {
            run.setBold(true);
            cell.setColor("E8F5E9");
        }
    }

    private void addPdfTableCell(Table table, String text, PdfFont font, boolean isHeader, TextAlignment alignment) {
        Cell cell = new Cell()
                .add(new Paragraph(text).setFont(font).setFontSize(isHeader ? 12 : 11))
                .setTextAlignment(alignment)
                .setPadding(5);
        if (isHeader) {
            cell.setBackgroundColor(new DeviceRgb(232, 245, 233));
        }
        table.addCell(cell);
    }

    private String translateLevel(String level) {
        return switch (level) {
            case "high" -> "高";
            case "medium" -> "中";
            case "low" -> "低";
            default -> level;
        };
    }

    private String getLevelColor(String level) {
        return switch (level) {
            case "high" -> "FF0000";
            case "medium" -> "FFA500";
            case "low" -> "008000";
            default -> "000000";
        };
    }

    private DeviceRgb getPdfLevelColor(String level) {
        return switch (level) {
            case "high" -> new DeviceRgb(255, 0, 0);
            case "medium" -> new DeviceRgb(255, 165, 0);
            case "low" -> new DeviceRgb(0, 128, 0);
            default -> new DeviceRgb(0, 0, 0);
        };
    }

    public String saveWordDisclosureCard(Disclosure disclosure, Task task,
                                         List<Hazard> hazards, List<SafetyMeasure> measures) throws IOException {
        byte[] content = generateWordDisclosureCard(disclosure, task, hazards, measures);
        String fileName = "交底卡_" + disclosure.getDisclosureNo() + "_" + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".docx";
        String filePath = outputPath + "/" + fileName;
        
        java.io.File dir = new java.io.File(outputPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            fos.write(content);
        }
        
        return filePath;
    }

    public String savePdfDisclosureCard(Disclosure disclosure, Task task,
                                        List<Hazard> hazards, List<SafetyMeasure> measures) throws IOException {
        byte[] content = generatePdfDisclosureCard(disclosure, task, hazards, measures);
        String fileName = "交底卡_" + disclosure.getDisclosureNo() + "_" + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";
        String filePath = outputPath + "/" + fileName;
        
        java.io.File dir = new java.io.File(outputPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            fos.write(content);
        }
        
        return filePath;
    }
}