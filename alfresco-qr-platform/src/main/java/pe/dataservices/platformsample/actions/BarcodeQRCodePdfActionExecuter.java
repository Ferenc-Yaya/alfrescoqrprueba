package pe.dataservices.platformsample.actions;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.BarcodeQRCode;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.repo.action.ParameterDefinitionImpl;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class BarcodeQRCodePdfActionExecuter extends ActionExecuterAbstractBase {
    private static Log logger = LogFactory.getLog(BarcodeQRCodePdfActionExecuter.class);
    private ServiceRegistry serviceRegistry;
    public static final String PARAM_PAGE_NO = "page-no";

    public void setServiceRegistry(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }

    @Override
    protected void addParameterDefinitions(List<ParameterDefinition> paramList) {
        for (String s : new String[] {PARAM_PAGE_NO}) {
            paramList.add(new ParameterDefinitionImpl(s, DataTypeDefinition.TEXT, true, getParamDisplayLabel(s)));
        }
    }

    @Override
    protected void executeImpl(Action action, NodeRef actionedUponNodeRef) {
        // Obtener el número de página o usar un valor predeterminado si no se proporciona
        String pageNoStr = (String) action.getParameterValue(PARAM_PAGE_NO);
        int pageNo = 1; // Valor predeterminado: primera página

        if (pageNoStr != null && !pageNoStr.isEmpty()) {
            try {
                pageNo = Integer.parseInt(pageNoStr);
                logger.debug("Using provided page number: " + pageNo);
            } catch (NumberFormatException e) {
                logger.warn("Invalid page number format: " + pageNoStr + ". Using default page 1.");
            }
        } else {
            logger.debug("No page number provided. Using default page 1.");
        }

        // Verificar si el nodo es un PDF
        ContentReader reader = serviceRegistry.getContentService().getReader(actionedUponNodeRef, ContentModel.PROP_CONTENT);
        if (reader == null || !reader.getMimetype().equals("application/pdf")) {
            // No es un PDF, no hacer nada
            logger.warn("Node is not a PDF, skipping QR code generation");
            return;
        }

        Map<QName, Serializable> props = serviceRegistry.getNodeService().getProperties(actionedUponNodeRef);
        String qrCodeString = new String();
        for (Entry<QName, Serializable> entry : props.entrySet()){
            qrCodeString += entry.getKey().getLocalName()+" : "+entry.getValue()+"\n";
        }
        ContentWriter writer = serviceRegistry.getContentService().getWriter(actionedUponNodeRef,
                ContentModel.PROP_CONTENT, true);

        try {
            PdfReader pdfReader = new PdfReader(reader.getContentInputStream());
            PdfStamper stamper = new PdfStamper(pdfReader, writer.getContentOutputStream());
            PdfContentByte over = stamper.getOverContent(pageNo);
            BarcodeQRCode barcodeQRCode = new BarcodeQRCode(qrCodeString, 1, 1, null);
            Image qrcodeImage = barcodeQRCode.getImage();
            qrcodeImage.setAbsolutePosition(10,10);
            over.addImage(qrcodeImage);
            stamper.close();
            pdfReader.close();
        } catch (ContentIOException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (DocumentException e) {
            e.printStackTrace();
        }
    }
}