package org.esa.snap.visat.actions;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.dataio.ExpectedContent;
import org.esa.snap.dataio.ExpectedDataset;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.SnapDialogs;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;

import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Random;
import java.util.logging.Logger;

/**
 * @author Marco Peters
 */
@ActionID(category = "Testing", id = "org.esa.snap.visat.actions.CreateExpectedJsonCodeAction")
@ActionRegistration(displayName = "Copy Expected JSON Code to Clipboard", lazy = true)
@ActionReference(path = "Menu/Tools/Testing")
public class CreateExpectedJsonCodeAction implements ActionListener {

    public static final String LF = System.getProperty("line.separator");
    public static final Logger LOG = Logger.getLogger(CreateExpectedJsonCodeAction.class.getName());
    private Clipboard clipboard;
    private final Product product;

    public CreateExpectedJsonCodeAction(Product product) {
        this.product = product;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        final Window window = SnapApp.getDefault().getMainFrame();
        final ProgressMonitorSwingWorker worker = new ProgressMonitorSwingWorker(window, "Extracting Expected Content") {
            @Override
            protected Void doInBackground(ProgressMonitor pm) throws Exception {
                pm.beginTask("Collecting data...", ProgressMonitor.UNKNOWN);
                try {
                    fillClipboardWithJsonCode(new Random(123546));
                } catch (Exception e) {
                    e.printStackTrace();
                    LOG.
                            severe(e.getMessage());
                    SnapDialogs.showError(e.getMessage());
                } finally {
                    pm.done();
                }
                return null;
            }
        };
        worker.executeWithBlocking();

    }

    private Clipboard getClipboard() {
        if (clipboard == null) {
            clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        }
        return clipboard;
    }

    void fillClipboardWithJsonCode(Random random) throws IOException {
        final String jsonCode = createJsonCode(random);
        StringSelection clipboardContent = new StringSelection(jsonCode);
        getClipboard().setContents(clipboardContent, clipboardContent);
    }

    String createJsonCode(Random random) throws IOException {
        final ExpectedContent expectedContent = new ExpectedContent(product, random);
        ExpectedDataset expectedDataset = new ExpectedDataset();
        expectedDataset.setId(generateID(product));
        expectedDataset.setExpectedContent(expectedContent);

        expectedDataset.setDecodeQualification(getDecodeQualification(product));
        ObjectWriter writer = getConfiguredJsonWriter();
        final StringWriter stringWriter = new StringWriter();
        writer.writeValue(stringWriter, expectedDataset);
        stringWriter.flush();
        return stringWriter.toString();
    }

    private String getDecodeQualification(Product product) {
        ProductReader reader = product.getProductReader();
        if(reader == null) {
            throw new IllegalStateException("Product has no reader associated!");
        }
        ProductReaderPlugIn readerPlugIn = reader.getReaderPlugIn();
        DecodeQualification decodeQualification = readerPlugIn.getDecodeQualification(product.getFileLocation());
        return decodeQualification.name();
    }

    String generateID(Product product) {
        String id = product.getName();
        id = id.replace(" ", "_");
        id = id.replace(".", "_");
        return id;
    }

    static ObjectWriter getConfiguredJsonWriter() {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(SerializationFeature.WRITE_EMPTY_JSON_ARRAYS, false);
        final VisibilityChecker<?> defaultVisibilityChecker = mapper.getSerializationConfig().getDefaultVisibilityChecker();
        final VisibilityChecker<?> visibilityChecker = defaultVisibilityChecker.withIsGetterVisibility(JsonAutoDetect.Visibility.NONE);
        mapper.setVisibilityChecker(visibilityChecker);
        final ObjectWriter writer = mapper.writer();
        final MyDefaultPrettyPrinter prettyPrinter = new MyDefaultPrettyPrinter();
        final IdeaLikeIndenter indenter = new IdeaLikeIndenter();
        prettyPrinter.indentArraysWith(indenter);
        prettyPrinter.indentObjectsWith(indenter);
        return writer.with(prettyPrinter);
    }

    public void setClipboard(Clipboard clipboard) {
        this.clipboard = clipboard;
    }


    private static class IdeaLikeIndenter extends DefaultPrettyPrinter.NopIndenter {

        @Override
        public boolean isInline() {
            return false;
        }

        @Override
        public void writeIndentation(JsonGenerator jg, int level) throws IOException {
            jg.writeRaw(LF);
            while (level > 0) {
                jg.writeRaw("    ");
                level--;
            }

        }
    }

    private static class MyDefaultPrettyPrinter implements PrettyPrinter {

        private final DefaultPrettyPrinter defaultPrettyPrinter = new DefaultPrettyPrinter();

        public void indentArraysWith(DefaultPrettyPrinter.Indenter i) {
            defaultPrettyPrinter.indentArraysWith(i);
        }

        public void indentObjectsWith(DefaultPrettyPrinter.Indenter i) {
            defaultPrettyPrinter.indentObjectsWith(i);
        }

        public void writeRootValueSeparator(JsonGenerator jg) throws IOException {
            defaultPrettyPrinter.writeRootValueSeparator(jg);
        }

        public void writeStartObject(JsonGenerator jg) throws IOException {
            defaultPrettyPrinter.writeStartObject(jg);
        }

        public void beforeObjectEntries(JsonGenerator jg) throws IOException {
            defaultPrettyPrinter.beforeObjectEntries(jg);
        }

        public void writeObjectFieldValueSeparator(JsonGenerator jg) throws IOException {
            jg.writeRaw(": ");
        }

        public void writeObjectEntrySeparator(JsonGenerator jg) throws IOException {
            defaultPrettyPrinter.writeObjectEntrySeparator(jg);
        }

        public void writeEndObject(JsonGenerator jg, int nrOfEntries) throws IOException {
            defaultPrettyPrinter.writeEndObject(jg, nrOfEntries);
        }

        public void writeStartArray(JsonGenerator jg) throws IOException {
            defaultPrettyPrinter.writeStartArray(jg);
        }

        public void beforeArrayValues(JsonGenerator jg) throws IOException {
            defaultPrettyPrinter.beforeArrayValues(jg);
        }

        public void writeArrayValueSeparator(JsonGenerator jg) throws IOException {
            defaultPrettyPrinter.writeArrayValueSeparator(jg);
        }

        public void writeEndArray(JsonGenerator jg, int nrOfValues) throws IOException {
            defaultPrettyPrinter.writeEndArray(jg, nrOfValues);
        }

    }
}
