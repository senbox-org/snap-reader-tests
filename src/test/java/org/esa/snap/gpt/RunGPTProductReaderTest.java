package org.esa.snap.gpt;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.main.GPT;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.dataio.ContentAssert;
import org.esa.snap.dataio.ExpectedContent;
import org.esa.snap.dataio.ExpectedDataset;
import org.esa.snap.lib.openjpeg.activator.OpenJPEGInstaller;
import org.esa.snap.runtime.EngineConfig;
import org.esa.snap.runtime.LogUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.*;
import java.nio.file.NotDirectoryException;
import java.util.*;
import java.util.logging.*;
import java.util.logging.Formatter;

import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

/**
 * Created by jcoravu on 1/4/2020.
 */
public class RunGPTProductReaderTest {

    private final static String LINE_SEPARATOR = System.getProperty("line.separator", "\r\n");

    private static final String PROPERTY_NAME_LOG_FILE_PATH = "gpt.tests.log.file";
    private static final String PROPERTY_NAME_GPT_TEST_RESOURCES_FOLDER_PATH = "gpt.test.resources.dir";
    private static final String PROPERTY_NAME_GPT_SOURCE_PRODUCTS_FOLDER_PATH = "gpt.source.products.dir";
    private static final String PROPERTY_NAME_GPT_OUTPUT_PRODUCTS_FOLDER_PATH = "gpt.output.products.dir";
    private static final String PROPERTY_NAME_GPT_TEST_RESOURCE_FOLDER_NAME = "gpt.test.resource.dir.name";
    private static final String PROPERTY_NAME_GPT_TEST_RESOURCE_FILE_NAMES = "gpt.test.resource.file.names";
    private static final String PROPERTY_NAME_GRAPH_START_PATH_TO_REMOVE = "graph.start.path.to.remove";
    private static final String PROPERTY_NAME_FAIL_ON_MISSING_DATA = "gpt.tests.failOnMissingData";
    private static final String PROPERTY_NAME_FAIL_ON_EXCEPTION = "gpt.tests.failOnException";
    private static final String PROPERTY_NAME_LOG_EXCEPTION_STACK_TRACE = "gpt.tests.logExceptionStackTrace";

    private static final Logger logger = Logger.getLogger(RunGPTProductReaderTest.class.getName());

    public RunGPTProductReaderTest() {
    }

    @BeforeClass
    public static void initialize() throws Exception {
        LogUtils.initLogger();

        OpenJPEGInstaller.install();
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
     * -Dgpt.tests.failOnMissingData=false
     * -Dgpt.tests.failOnException=false
     * -Dgpt.tests.logExceptionStackTrace=false
     *
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

        boolean failIfMissingSourceData = true;
        String failOnMissingDataValue = System.getProperty(PROPERTY_NAME_FAIL_ON_MISSING_DATA);
        if (failOnMissingDataValue != null) {
            failIfMissingSourceData = Boolean.parseBoolean(failOnMissingDataValue);
        }

        boolean failIfException = true;
        String failOnExceptionValue = System.getProperty(PROPERTY_NAME_FAIL_ON_EXCEPTION);
        if (failOnExceptionValue != null) {
            failIfException = Boolean.parseBoolean(failOnExceptionValue);
        }

        boolean logExceptionStackTrace = true;
        String logExceptionStackTraceValue = System.getProperty(PROPERTY_NAME_LOG_EXCEPTION_STACK_TRACE);
        if (logExceptionStackTraceValue != null) {
            logExceptionStackTrace = Boolean.parseBoolean(logExceptionStackTraceValue);
        }

        File resourcesTestsFolder = new File(gptTestResourcesFolderPath, "tests");
        validateFolderOnDisk(resourcesTestsFolder);

        File sourceProductsFolder = new File(sourceProductsFolderPath);
        validateFolderOnDisk(sourceProductsFolder);

        File outputProductsFolder = new File(outputProductsFolderPath);
        if (outputProductsFolder.exists()) {
            // the output folder exists and delete it
            FileUtils.deleteTree(outputProductsFolder);
            if (outputProductsFolder.exists()) {
                throw new IllegalStateException("The output folder '" + outputProductsFolder.getAbsolutePath() + "' could not be deleted.");
            }
        }
        boolean created = outputProductsFolder.mkdirs();
        if (!created) {
            throw new IllegalStateException("The output folder '" + outputProductsFolder.getAbsolutePath() + "' could not be created.");
        }

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
            messageTrace.append("\n")
                    .append(extractExceptionMessage(exception, 2));
            logger.log(Level.SEVERE, messageTrace.toString());
        }
    }

    private static String extractExceptionMessage(Throwable exception, int elementCountToLog) {
        StringBuilder message = new StringBuilder();
        message.append(exception.getClass().getName())
                .append(": ")
                .append(exception.getMessage());
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

        long startTime = System.currentTimeMillis();
        logger.info("Test the file '" + testFile.getName() + "' from the folder '" + testFile.getParent()+"'.");

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

            Map<String, String>[] outputParametersArray = inputGraphData[i].getOutputs();
            if (outputParametersArray.length > 1) {
                throw new IllegalStateException("The output count must be 1.");
            }
            Map<String, String> outputParameters = outputParametersArray[0];
            String parameterValue = outputParameters.get("parameter");
            if (StringUtils.isNullOrEmpty(parameterValue)) {
                throw new NullPointerException("The parameter value is null or empty.");
            }
            String outputNameValue = outputParameters.get("outputName");
            if (StringUtils.isNullOrEmpty(outputNameValue)) {
                throw new NullPointerException("The output name value is null or empty.");
            }
            String expectedValuesRelativeFilePath = outputParameters.get("expected");
            if (StringUtils.isNullOrEmpty(expectedValuesRelativeFilePath)) {
                throw new NullPointerException("The expected relative file path is null or empty.");
            }

            File outputFolder = new File(outputProductsFolder, inputGraphData[i].getId());
            File outputProductFolder = new File(outputFolder, outputNameValue);
            gptGraphParameters.put(parameterValue, outputProductFolder.getAbsolutePath());

            File graphFile = new File(resourcesGraphsFolder, inputGraphData[i].getGraphPath());
            validateFileOnDisk(graphFile);

            runGPT(graphFile, gptGraphParameters);

            double elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000.0d;
            logger.info("Finish processing the graph file '" + graphFile.getAbsolutePath() +"'. The time elapsed is " + elapsedSeconds + " seconds.");

            File productFile = new File(outputFolder, outputNameValue + ".dim");
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
                File expectedValuesFile = new File(resourcesExpectedOutputsFolder, expectedValuesRelativeFilePath);
                ExpectedDataset expectedDataset = mapper.readValue(expectedValuesFile, ExpectedDataset.class);
                StringBuilder assertMessagePrefix = new StringBuilder();
                assertMessagePrefix.append("Test file '")
                        .append(testFile.getName())
                        .append("', id '")
                        .append(inputGraphData[i].getId())
                        .append("', output name '")
                        .append(outputNameValue)
                        .append("': ");
                assertExpectedContent(product, expectedDataset.getExpectedContent(), assertMessagePrefix.toString());
            }
        }

        double elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000.0d;
        logger.info("Finish testing the file '" + testFile.getName() + "' from the folder '" + testFile.getParent()+"'. The time elapsed is " + elapsedSeconds + " seconds.");
    }

    private static void runGPT(File graphFile, Map<String, String> gptGraphParameters) throws Exception {
        String[] args = new String[gptGraphParameters.size() + 1];
        args[0] = graphFile.getAbsolutePath();
        int index = 1;
        for (Map.Entry<String, String> entry : gptGraphParameters.entrySet()) {
            args[index++] = "-P"+entry.getKey()+"="+entry.getValue();
        }

        PrintStream original = System.out;
        PrintStream emptyOutputPrintStream = new PrintStream(new OutputStream() {
            @Override
            public void write(int b) {
                // do nothing
            }
        });
        System.setOut(emptyOutputPrintStream);
        try {
            GPT.run(args);
        } finally {
            System.setOut(original);
        }
    }

    private static void assertExpectedContent(Product product, ExpectedContent expectedContent, String productId) throws IOException {
        ContentAssert contentAssert = new ContentAssert(expectedContent, productId, product);
        contentAssert.assertProductContent();
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

    private static class InputGraphData {

        private String id;
        private String author;
        private String description;
        private String frequency;
        private String graphPath;
        private Map<String, String> inputs;
        private Map<String, String> parameters;
        private Map<String, String>[] outputs;

        public InputGraphData() {
        }

        public Map<String, String> getParameters() {
            return parameters;
        }

        public void setParameters(Map<String, String> parameters) {
            this.parameters = parameters;
        }

        public Map<String, String>[] getOutputs() {
            return outputs;
        }

        public void setOutputs(Map<String, String>[] outputs) {
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
