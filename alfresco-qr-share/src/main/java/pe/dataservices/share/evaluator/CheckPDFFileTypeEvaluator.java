package pe.dataservices.share.evaluator;

import org.alfresco.web.evaluator.BaseEvaluator;
import org.json.simple.JSONObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class CheckPDFFileTypeEvaluator extends BaseEvaluator {
    private static Log logger = LogFactory.getLog(CheckPDFFileTypeEvaluator.class);

    @Override
    public boolean evaluate(JSONObject jsonObject) {
        try {
            String mimetype = getNodeMimetype(jsonObject);
            logger.debug("CheckPDFFileTypeEvaluator - Evaluando nodo con mimetype: " + mimetype);

            return mimetype != null && mimetype.equals("application/pdf");
        } catch (Exception e) {
            logger.error("CheckPDFFileTypeEvaluator - Error al evaluar el tipo de archivo", e);
            return false;
        }
    }
}
