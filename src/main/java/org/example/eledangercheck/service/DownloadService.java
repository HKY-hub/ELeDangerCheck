package org.example.eledangercheck.service;

import org.example.eledangercheck.entity.Disclosure;
import org.springframework.stereotype.Service;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;

import org.apache.poi.xwpf.usermodel.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class DownloadService {

    public byte[] generateWord(Disclosure disclosure) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             XWPFDocument doc = new XWPFDocument()) {
            
            XWPFParagraph title = doc.createParagraph();
            title.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = title.createRun();
            titleRun.setText(disclosure.getTitle());
            titleRun.setBold(true);
            titleRun.setFontSize(18);
            
            doc.createParagraph();
            
            if (disclosure.getDisclosureNo() != null) {
                XWPFParagraph noPara = doc.createParagraph();
                noPara.setIndentationLeft(200);
                XWPFRun noRun = noPara.createRun();
                noRun.setText("编号：" + disclosure.getDisclosureNo());
                noRun.setFontSize(12);
            }
            
            if (disclosure.getDisclosureType() != null) {
                XWPFParagraph typePara = doc.createParagraph();
                typePara.setIndentationLeft(200);
                XWPFRun typeRun = typePara.createRun();
                typeRun.setText("类型：" + disclosure.getDisclosureType());
                typeRun.setFontSize(12);
            }
            
            doc.createParagraph();
            
            XWPFParagraph contentTitle = doc.createParagraph();
            XWPFRun contentTitleRun = contentTitle.createRun();
            contentTitleRun.setText("内容：");
            contentTitleRun.setBold(true);
            contentTitleRun.setFontSize(12);
            
            String content = disclosure.getContent();
            if (content != null) {
                String[] lines = content.split("\n");
                for (String line : lines) {
                    XWPFParagraph contentPara = doc.createParagraph();
                    contentPara.setIndentationLeft(400);
                    XWPFRun contentRun = contentPara.createRun();
                    contentRun.setText(line.trim());
                    contentRun.setFontSize(12);
                }
            }
            
            doc.createParagraph();
            
            if (disclosure.getEmergencyContact() != null) {
                XWPFParagraph contactPara = doc.createParagraph();
                XWPFRun contactRun = contactPara.createRun();
                contactRun.setText("应急联系人：" + disclosure.getEmergencyContact());
                contactRun.setFontSize(12);
            }
            
            if (disclosure.getEmergencyRoute() != null) {
                XWPFParagraph routePara = doc.createParagraph();
                XWPFRun routeRun = routePara.createRun();
                routeRun.setText("应急路线：" + disclosure.getEmergencyRoute());
                routeRun.setFontSize(12);
            }
            
            doc.write(out);
            return out.toByteArray();
        }
    }

    public byte[] generatePdf(Disclosure disclosure) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);
            
            document.setMargins(50, 50, 50, 50);
            
            Paragraph title = new Paragraph(disclosure.getTitle());
            title.setTextAlignment(TextAlignment.CENTER);
            title.setFontSize(18);
            title.setBold();
            title.setMarginBottom(20);
            document.add(title);
            
            if (disclosure.getDisclosureNo() != null) {
                Paragraph noPara = new Paragraph("编号：" + disclosure.getDisclosureNo());
                noPara.setFontSize(12);
                noPara.setMarginBottom(8);
                document.add(noPara);
            }
            
            if (disclosure.getDisclosureType() != null) {
                Paragraph typePara = new Paragraph("类型：" + disclosure.getDisclosureType());
                typePara.setFontSize(12);
                typePara.setMarginBottom(8);
                document.add(typePara);
            }
            
            Paragraph contentTitle = new Paragraph("内容：");
            contentTitle.setFontSize(12);
            contentTitle.setBold();
            contentTitle.setMarginBottom(8);
            document.add(contentTitle);
            
            String content = disclosure.getContent();
            if (content != null) {
                String[] lines = content.split("\n");
                for (String line : lines) {
                    Paragraph contentPara = new Paragraph(line.trim());
                    contentPara.setFontSize(12);
                    contentPara.setMarginBottom(4);
                    contentPara.setFirstLineIndent(24);
                    document.add(contentPara);
                }
            }
            
            if (disclosure.getEmergencyContact() != null) {
                Paragraph contactPara = new Paragraph("应急联系人：" + disclosure.getEmergencyContact());
                contactPara.setFontSize(12);
                contactPara.setMarginTop(16);
                document.add(contactPara);
            }
            
            if (disclosure.getEmergencyRoute() != null) {
                Paragraph routePara = new Paragraph("应急路线：" + disclosure.getEmergencyRoute());
                routePara.setFontSize(12);
                routePara.setMarginBottom(8);
                document.add(routePara);
            }
            
            document.close();
            return out.toByteArray();
        }
    }
}