package org.esa.snap.gpt;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.main.GPT;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.dataio.ContentAssert;
import org.esa.snap.dataio.ExpectedContent;
import org.esa.snap.dataio.ExpectedDataset;
import org.esa.snap.dataio.ProductReaderAcceptanceTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.*;
import java.nio.file.NotDirectoryException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.*;
import java.util.logging.Formatter;

import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

/**
 * Created by jcoravu on 1/4/2020.
 */
public class RunGPTProductReaderTest {

    private static final String PROPERTY_NAME_LOG_FILE_PATH = "gpt.tests.log.file";
    private static final String PROPERTY_NAME_GPT_TEST_RESOURCES_FOLDER_PATH = "gpt.test.resources.dir";
    private static final String PROPERTY_NAME_GPT_SOURCE_PRODUCTS_FOLDER_PATH = "gpt.source.products.dir";
    private static final String PROPERTY_NAME_GPT_OUTPUT_PRODUCTS_FOLDER_PATH = "gpt.output.products.dir";
    private static final String PROPERTY_NAME_GPT_TEST_RESOURCE_FOLDER_NAME = "gpt.test.resource.dir.name";
    private static final String PROPERTY_NAME_GPT_TEST_RESOURCE_FILE_NAMES = "gpt.test.resource.file.names";
    private static final String PROPERTY_NAME_GRAPH_START_PATH_TO_REMOVE = "graph.start.path.to.remove";

    private static Logger logger;

    @BeforeClass
    public static void initialize() throws Exception {
        initLogger();
    }

    /**
     * The required parameters to run the method:
     * -Dgpt.test.resources.dir=D:/snap-sources/master/snap-gpt-tests/gpt-tests-resources
     * -Dgpt.source.products.dir=Z:/TestingJUnitFiles
     * -Dgpt.output.products.dir=D:/test-gpt-output
     * -Dgpt.test.resource.dir.name=s2tbx
     *
     * Optional parameters:
     * -Dgpt.test.resource.file.names=AlosAV2ReaderTest.json,AlosAV2ReadOpPixelSubsetTest.json,AlosAV2ReadOpGeometrySubsetTest.json
     * -Dgraph.start.path.to.remove=s2tbx/
     * @throws Exception
     */
    @Test
    public void testProcessFolder() throws Exception {
        String gptTestResourcesFolderPath = System.getProperty(PROPERTY_NAME_GPT_TEST_RESOURCES_FOLDER_PATH);
        String sourceProductsFolderPath = System.getProperty(PROPERTY_NAME_GPT_SOURCE_PRODUCTS_FOLDER_PATH);
        String outputProductsFolderPath = System.getProperty(PROPERTY_NAME_GPT_OUTPUT_PRODUCTS_FOLDER_PATH);
        String gptTestFolderName = System.getProperty(PROPERTY_NAME_GPT_TEST_RESOURCE_FOLDER_NAME);
        if (gptTestResourcesFolderPath == null || sourceProductsFolderPath == null
                || outputProductsFolderPath == null || gptTestFolderName == null) {

            assumeTrue("Missing properties", false);
        }
        String gptTestFileNames = System.getProperty(PROPERTY_NAME_GPT_TEST_RESOURCE_FILE_NAMES); // may be null
        String startPathToRemove = System.getProperty(PROPERTY_NAME_GRAPH_START_PATH_TO_REMOVE); // the start path may be null

        File resourcesTestsFolder = new File(gptTestResourcesFolderPath, "tests");
        validateFolderOnDisk(resourcesTestsFolder);

        File sourceProductsFolder = new File(sourceProductsFolderPath);
        validateFolderOnDisk(sourceProductsFolder);

        File outputProductsFolder = new File(outputProductsFolderPath);
        validateFolderOnDisk(outputProductsFolder);

        File resourcesGraphsFolder = new File(gptTestResourcesFolderPath, "graphs");
        validateFolderOnDisk(resourcesGraphsFolder);

        File resourcesExpectedOutputsFolder = new File(gptTestResourcesFolderPath, "expectedOutputs");
        validateFolderOnDisk(resourcesExpectedOutputsFolder);

        File testFolder = new File(resourcesTestsFolder, gptTestFolderName);
        validateFolderOnDisk(testFolder);

        String[] testFileNames;
        if (gptTestFileNames == null) {
            testFileNames = testFolder.list();
        } else {
            testFileNames = gptTestFileNames.split(",");
        }
        if (testFileNames.length == 0) {
            throw new IllegalArgumentException("The test file array is empty.");
        }
        boolean failIfMissingSourceData = false;
        boolean failIfException = false;
        boolean logExceptionStackTrace = false;

        logger.info("Test files from folder '" + testFolder.getName() + "'.");

        List<Throwable> assertErrors = new ArrayList();
        for (int k=0; k<testFileNames.length; k++) {
            if (k > 0) {
                logger.info(""); // write an empty line
            }
            String testingFileName = testFileNames[k].trim();
            if (StringUtils.isNullOrEmpty(testingFileName)) {
                throw new NullPointerException("The testing file name is null or empty.");
            }
            File testFile = new File(testFolder, testingFileName);
            validateFileOnDisk(testFile);
            try {
                processFile(testFile, sourceProductsFolder, outputProductsFolder, resourcesGraphsFolder,
                            resourcesExpectedOutputsFolder, startPathToRemove, failIfMissingSourceData);
            } catch (Exception e) {
                if (failIfException) {
                    throw e;
                } else {
                    String message = "Failed to run the test file '" + testFile.getName() +"' from the folder '" + gptTestFolderName +"'.";
                    logException(message.toString(), e, logExceptionStackTrace);
                }
            } catch (AssertionError e) {
                assertErrors.add(e);
                String message = "Failed to test the values of the file '" + testFile.getName() +"' from the folder '" + gptTestFolderName +"'.";
                logException(message.toString(), e, logExceptionStackTrace);
            }
        }

        // check the errors
        if (assertErrors.size() > 0) {
            StringBuilder failMessage = new StringBuilder();
            for (int i=0; i<assertErrors.size(); i++) {
                if (i > 0) {
                    failMessage.append("\n");
                }
                failMessage.append(assertErrors.get(i).getMessage());
            }
            fail(failMessage.toString());
        }
    }

    private static void logException(String message, Throwable exception, boolean logExceptionStackTrace) {
        if (logExceptionStackTrace) {
            logger.log(Level.SEVERE, message, exception);
        } else {
            StringBuilder messageTrace = new StringBuilder(message);
            messageTrace.append("\n\t")
                    .append(extractExceptionMessage(exception, 2));
            logger.log(Level.SEVERE, messageTrace.toString());
        }
    }

    private static String extractExceptionMessage(Throwable exception, int elementCountToLog) {
        StringBuilder message = new StringBuilder(exception.getMessage());
        StackTraceElement[] elements = exception.getStackTrace();
        if (elements != null && elements.length > 0) {
            for (int i=0; i<elementCountToLog && i<elements.length; i++) {
                message.append("\n\tat ")
                        .append(elements[i].getClassName())
                        .append(".")
                        .append(elements[i].getMethodName())
                        .append("(")
                        .append(elements[i].getFileName())
                        .append(":")
                        .append(elements[i].getLineNumber())
                        .append(")");
            }
        }
        return message.toString();
    }

    private static void processFile(File testFile, File sourceProductsFolder, File outputProductsFolder,
                                   File resourcesGraphsFolder, File resourcesExpectedOutputsFolder,
                                   String startPathToRemove, boolean failIfMissingSourceData)
                                   throws Exception {


        logger.info("Test file '" + testFile.getName() + "'.");

        ObjectMapper mapper = new ObjectMapper();
        InputGraphData[] inputGraphData = mapper.readValue(testFile, InputGraphData[].class);

        for (int i = 0; i < inputGraphData.length; i++) {

            logger.info("Run GPT for id '" + inputGraphData[i].getId() + "' using the graph '" + inputGraphData[i].getGraphPath() + "'.");

            boolean canContinue = true;
            Map<String, String> gptGraphParameters = new HashMap();
            for (Map.Entry<String, String> entry : inputGraphData[i].getInputs().entrySet()) {
                String parameterValue = entry.getValue();
                if (startPathToRemove != null && parameterValue.startsWith(startPathToRemove)) {
                    parameterValue = parameterValue.substring(startPathToRemove.length());
                }
                File sourceProductFile = new File(sourceProductsFolder, parameterValue);
                if (!sourceProductFile.exists()) {
                    String message = "The source product file '" + sourceProductFile.getAbsolutePath()+"' does not exist.";
                    if (failIfMissingSourceData) {
                        throw new FileNotFoundException(message);
                    } else {
                        canContinue = false;
                        logger.log(Level.SEVERE, message);
                    }
                }
                gptGraphParameters.put(entry.getKey(), sourceProductFile.getAbsolutePath());
            }
            if (!canContinue) {
                continue;
            }
            for (Map.Entry<String, String> entry : inputGraphData[i].getParameters().entrySet()) {
                gptGraphParameters.put(entry.getKey(), entry.getValue());
            }

            if (inputGraphData[i].getOutputs().length > 1) {
                throw new IllegalStateException("The output count must be 1.");
            }
            GraphOutputParameters outputParameters = inputGraphData[i].getOutputs()[0];

            File outputFolder = new File(outputProductsFolder, inputGraphData[i].getId());
            File outputProductFolder = new File(outputFolder, outputParameters.getOutputName());
            gptGraphParameters.put(outputParameters.getParameter(), outputProductFolder.getAbsolutePath());

            File graphFile = new File(resourcesGraphsFolder, inputGraphData[i].getGraphPath());
            validateFileOnDisk(graphFile);

            runGPT(graphFile, gptGraphParameters);

            File productFile = new File(outputFolder, outputParameters.getOutputName() + ".dim");
            Product product = ProductIO.readProduct(productFile);
            if (product == null) {
                String message = "The product can not be read from file '" + productFile.getAbsolutePath()+"' does not exist.";
                if (failIfMissingSourceData) {
                    throw new FileNotFoundException(message);
                } else {
                    logger.log(Level.SEVERE, message);
                    continue;
                }
            } else {
                File expectedValuesFile = new File(resourcesExpectedOutputsFolder, outputParameters.getExpected());
                ExpectedDataset expectedDataset = mapper.readValue(expectedValuesFile, ExpectedDataset.class);
                StringBuilder assertMessagePrefix = new StringBuilder();
                assertMessagePrefix.append("Test file '")
                        .append(testFile.getName())
                        .append("', id '")
                        .append(inputGraphData[i].getId())
                        .append("', output name '")
                        .append(outputParameters.getOutputName())
                        .append("': ");
                assertExpectedContent(product, expectedDataset.getExpectedContent(), assertMessagePrefix.toString());
            }
        }
    }

    private static void runGPT(File graphFile, Map<String, String> gptGraphParameters) throws Exception {
        String[] args = new String[gptGraphParameters.size() + 1];
        args[0] = graphFile.getAbsolutePath();
        int index = 1;
        for (Map.Entry<String, String> entry : gptGraphParameters.entrySet()) {
            args[index++] = "-P"+entry.getKey()+"="+entry.getValue();
        }
        GPT.run(args);
    }

    private static void assertExpectedContent(Product product, ExpectedContent expectedContent, String productId) throws IOException {
        ContentAssert contentAssert = new ContentAssert(expectedContent, productId, product);
        contentAssert.assertProductContent();
    }

    private static void initLogger() throws Exception {
        // Suppress ugly (and harmless) JAI error messages saying that a JAI is going to continue in pure Java mode.
        System.setProperty("com.sun.media.jai.disableMediaLib", "true");  // disable native libraries for JAI

        logger = Logger.getLogger(ProductReaderAcceptanceTest.class.getSimpleName());
        removeRootLogHandler();
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(new CustomLogFormatter());
        logger.addHandler(consoleHandler);
        String logFilePath = System.getProperty(PROPERTY_NAME_LOG_FILE_PATH);
        if (logFilePath != null) {
            File logFile = new File(logFilePath);
            FileOutputStream fos = new FileOutputStream(logFile);
            StreamHandler streamHandler = new StreamHandler(fos, new CustomLogFormatter());
            logger.addHandler(streamHandler);
        }
    }

    private static void removeRootLogHandler() {
        Logger rootLogger = LogManager.getLogManager().getLogger("");
        Handler[] handlers = rootLogger.getHandlers();
        for (Handler handler : handlers) {
            rootLogger.removeHandler(handler);
        }
    }

    private static void validateFolderOnDisk(File folderToValidate) throws FileNotFoundException, NotDirectoryException {
        if (folderToValidate.exists()) {
            if (!folderToValidate.isDirectory()) {
                throw new NotDirectoryException("The path '" + folderToValidate.getAbsolutePath()+"' exists and it does not represent a folder.");
            }
        } else {
            throw new FileNotFoundException("The folder '" + folderToValidate.getAbsolutePath()+"' does not exist.");
        }
    }

    private static void validateFileOnDisk(File fileToValidate) throws FileNotFoundException, NotDirectoryException {
        if (fileToValidate.exists()) {
            if (!fileToValidate.isFile()) {
                throw new NotDirectoryException("The path '" + fileToValidate.getAbsolutePath()+"' exists and it does not represent a file.");
            }
        } else {
            throw new FileNotFoundException("The file '" + fileToValidate.getAbsolutePath()+"' does not exist.");
        }
    }

    private static class CustomLogFormatter extends Formatter {

        private final static String LINE_SEPARATOR = System.getProperty("line.separator", "\r\n");

        @Override
        public synchronized String format(LogRecord record) {
            StringBuilder sb = new StringBuilder();
            String message = formatMessage(record);
            sb.append(record.getLevel().getName());
            sb.append(": ");
            sb.append(message);
            sb.append(LINE_SEPARATOR);
            if (record.getThrown() != null) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                record.getThrown().printStackTrace(pw);
                pw.close();
                sb.append(sw.toString());
            }
            return sb.toString();
        }
    }

    private static class GraphOutputParameters {

        private String parameter;
        private String outputName;
        private String expected;

        public GraphOutputParameters() {
        }

        public String getParameter() {
            return parameter;
        }

        public void setParameter(String parameter) {
            this.parameter = parameter;
        }

        public String getOutputName() {
            return outputName;
        }

        public void setOutputName(String outputName) {
            this.outputName = outputName;
        }

        public String getExpected() {
            return expected;
        }

        public void setExpected(String expected) {
            this.expected = expected;
        }
    }

    private static class InputGraphData {

        private String id;
        private String author;
        private String description;
        private String frequency;
        private String graphPath;
        private Map<String, String> inputs;
        private Map<String, String> parameters;
        private GraphOutputParameters[] outputs;

        public InputGraphData() {
        }

        public Map<String, String> getParameters() {
            return parameters;
        }

        public void setParameters(Map<String, String> parameters) {
            this.parameters = parameters;
        }

        public GraphOutputParameters[] getOutputs() {
            return outputs;
        }

        public void setOutputs(GraphOutputParameters[] outputs) {
            this.outputs = outputs;
        }

        public String getId() {
            return id;
        }

        public String getAuthor() {
            return author;
        }

        public String getDescription() {
            return description;
        }

        public String getFrequency() {
            return frequency;
        }

        public String getGraphPath() {
            return graphPath;
        }

        public Map<String, String> getInputs() {
            return inputs;
        }

        public void setId(String id) {
            this.id = id;
        }

        public void setAuthor(String author) {
            this.author = author;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public void setFrequency(String frequency) {
            this.frequency = frequency;
        }

        public void setGraphPath(String graphPath) {
            this.graphPath = graphPath;
        }

        public void setInputs(Map<String, String> inputs) {
            this.inputs = inputs;
        }
    }

}
