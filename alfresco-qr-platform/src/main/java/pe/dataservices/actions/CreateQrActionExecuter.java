package pe.dataservices.actions;

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

public class CreateQrActionExecuter extends ActionExecuterAbstractBase {
    private static Log logger = LogFactory.getLog(CreateQrActionExecuter.class);
    private ServiceRegistry serviceRegistry;

    public void setServiceRegistry(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }

    @Override
    protected void addParameterDefinitions(List<ParameterDefinition> paramList) {
    }

    @Override
    protected void executeImpl(Action action, NodeRef actionedUponNodeRef) {
        ContentReader reader = serviceRegistry.getContentService().getReader(actionedUponNodeRef, ContentModel.PROP_CONTENT);
        if (reader == null || !reader.getMimetype().equals("application/pdf")) {
            logger.warn("El nodo no es un PDF, saltando la generación del código QR");
            return;
        }

        Map<QName, Serializable> props = serviceRegistry.getNodeService().getProperties(actionedUponNodeRef);
        StringBuilder qrCode = new StringBuilder();
        for (Entry<QName, Serializable> entry : props.entrySet()){
            qrCode.append(entry.getKey().getLocalName())
                        .append(" : ")
                        .append(entry.getValue())
                        .append("\n");
        }
        ContentWriter writer = serviceRegistry.getContentService().getWriter(actionedUponNodeRef,
                ContentModel.PROP_CONTENT, true);

        try {
            PdfReader pdfReader = new PdfReader(reader.getContentInputStream());
            PdfStamper stamper = new PdfStamper(pdfReader, writer.getContentOutputStream());

            int pageNo = 1;
            PdfContentByte over = stamper.getOverContent(pageNo);

            BarcodeQRCode barcodeQRCode = new BarcodeQRCode(qrCode.toString(), 1, 1, null);
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