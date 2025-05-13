package pe.dataservices.platformsample.actions;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.OutputStream;
import java.io.Serializable;
import java.util.*;
import java.util.List;

public class CreateMetadataPdfActionExecuter extends ActionExecuterAbstractBase {
    private static Log logger = LogFactory.getLog(CreateMetadataPdfActionExecuter.class);
    private ServiceRegistry serviceRegistry;

    public void setServiceRegistry(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }

    @Override
    protected void addParameterDefinitions(List<ParameterDefinition> paramList) {
    }

    @Override
    protected void executeImpl(Action action, NodeRef actionedUponNodeRef) {
        try {
            Map<QName, Serializable> properties = serviceRegistry.getNodeService().getProperties(actionedUponNodeRef);

            String originalName = (String) properties.get(ContentModel.PROP_NAME);
            String metadataFileName = "Metadatos_" + originalName.replaceFirst("\\.pdf$", "") + ".pdf";

            NodeRef parent = serviceRegistry.getNodeService().getPrimaryParent(actionedUponNodeRef).getParentRef();

            Map<QName, Serializable> newProps = new HashMap<>();
            newProps.put(ContentModel.PROP_NAME, metadataFileName);
            newProps.put(ContentModel.PROP_TITLE, "Metadatos de " + originalName);

            NodeRef metadataNode = serviceRegistry.getNodeService().createNode(
                    parent,
                    ContentModel.ASSOC_CONTAINS,
                    QName.createQName(ContentModel.USER_MODEL_URI, metadataFileName),
                    ContentModel.TYPE_CONTENT,
                    newProps).getChildRef();

            ContentWriter writer = serviceRegistry.getContentService().getWriter(metadataNode, ContentModel.PROP_CONTENT, true);
            writer.setMimetype("application/pdf");

            OutputStream out = writer.getContentOutputStream();
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);

            document.open();

            // Título
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, BaseColor.BLUE);
            Paragraph title = new Paragraph("Metadatos del documento: " + originalName, titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph(" ")); // Espacio

            // Información general
            document.add(new Paragraph("Fecha de generación: " + new Date(), new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL)));
            document.add(new Paragraph(" ")); // Espacio

            // Crear tabla de metadatos
            PdfPTable table = new PdfPTable(2); // 2 columnas
            table.setWidthPercentage(100);

            // Encabezados de tabla
            PdfPCell header1 = new PdfPCell(new Phrase("Propiedad", new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD)));
            PdfPCell header2 = new PdfPCell(new Phrase("Valor", new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD)));
            header1.setBackgroundColor(BaseColor.LIGHT_GRAY);
            header2.setBackgroundColor(BaseColor.LIGHT_GRAY);
            table.addCell(header1);
            table.addCell(header2);

            Set<QName> keys = properties.keySet();
            for (QName key : keys) {
                Serializable value = properties.get(key);
                if (value != null) {
                    table.addCell(key.getLocalName());
                    table.addCell(value.toString());
                }
            }

            document.add(table);
            document.close();
            out.close();

            if (serviceRegistry.getNodeService().hasAspect(actionedUponNodeRef, ContentModel.ASPECT_TAGGABLE)) {
                serviceRegistry.getNodeService().addAspect(metadataNode, ContentModel.ASPECT_TAGGABLE, null);

                Serializable tagsValue = properties.get(ContentModel.PROP_TAGS);
                if (tagsValue != null) {

                    if (tagsValue instanceof List<?>) {

                        List<NodeRef> tags = (List<NodeRef>) tagsValue;
                        for (NodeRef tagNode : tags) {
                            serviceRegistry.getTaggingService().addTag(metadataNode,
                                    (String) serviceRegistry.getNodeService().getProperty(tagNode, ContentModel.PROP_NAME));
                        }
                    }
                }
            }

            logger.info("PDF de metadatos creado con éxito: " + metadataFileName);

        } catch (Exception e) {
            logger.error("Error al crear el PDF de metadatos", e);
            throw new RuntimeException("Error al crear el PDF de metadatos", e);
        }
    }
}
