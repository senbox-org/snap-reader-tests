package org.esa.snap.dataio;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.lib.openjpeg.activator.OpenJPEGInstaller;
import org.esa.snap.runtime.LogUtils;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.fail;

public class RunProductReaderTest {

    private static final Logger logger = Logger.getLogger(RunProductReaderTest.class.getName());

    private static final String PROPERTY_NAME_FAIL_ON_MISSING_DATA = "snap.reader.tests.failOnMissingData";
    private static final String PROPERTY_NAME_DATA_DIR = "snap.reader.tests.data.dir";
    private static final String PROPERTY_NAME_CASS_NAME = "snap.reader.tests.class.name";
    private static final String PROPERTY_NAME_PRODUCT_ID = "snap.reader.tests.product.id";

    private static final boolean FAIL_ON_MISSING_DATA = Boolean.parseBoolean(System.getProperty(PROPERTY_NAME_FAIL_ON_MISSING_DATA, "true"));

    private static TestDefinitionList testDefinitionList;
    private static File dataRootDir;

    @Rule
    public ErrorCollector errorCollector = new ErrorCollector();

    public RunProductReaderTest() {
    }

    @BeforeClass
    public static void initialize() throws Exception {
        assertRequiredTestDataDirectory();

        LogUtils.initLogger();

        String readerPlugInClassNameToTest = System.getProperty(PROPERTY_NAME_CASS_NAME);
        String productIdToTest = System.getProperty(PROPERTY_NAME_PRODUCT_ID);

        if (logger.isLoggable(Level.FINE)) {
            StringBuilder message = new StringBuilder();
            message.append("Input test parameters: data folder path: ")
                    .append(dataRootDir.getAbsolutePath())
                    .append(", reader plugin class: ")
                    .append(readerPlugInClassNameToTest)
                    .append(", product id :")
                    .append(productIdToTest)
                    .append(".");
            logger.log(Level.FINE, message.toString());
        }

        SystemUtils.init3rdPartyLibs(RunProductReaderTest.class);

        if (!FAIL_ON_MISSING_DATA) {
            logger.warning("Tests will not fail if test data is missing!");
        }

        testDefinitionList = loadProductReaderTestDefinitions(readerPlugInClassNameToTest, productIdToTest);

        OpenJPEGInstaller.install();
    }

    @Test
    public void testReadIntendedProductContent() throws IOException {
        logger.info("Test product content for " + testDefinitionList.size() + " reader plugins.");

        for (TestDefinition testDefinition : testDefinitionList) {
            final List<String> intendedProductIds = testDefinition.getDecodableProductIds();
            String readerPluginClassName = testDefinition.getProductReaderPlugin().getClass().getSimpleName();

            logger.info("Test products content: reader plugin class: " + readerPluginClassName+ ", product count: " + intendedProductIds.size()+".");

            for (String productId : intendedProductIds) {
                TestProduct foundTestProduct = null;
                for (TestProduct testProduct : testDefinition.getAllProducts()) {
                    if (testProduct.getId().equalsIgnoreCase(productId)) {
                        foundTestProduct = testProduct;
                        break;
                    }
                }
                if (foundTestProduct == null) {
                    String message = "The test product with id '"+ productId+"' does not exist.";
                    if (FAIL_ON_MISSING_DATA) {
                        fail(message);
                    } else {
                        logger.info(message);
                    }
                } else if (foundTestProduct.exists()) {
                    logger.info("Start testing the product content: reader plugin class: " + readerPluginClassName+ ", product id: " + foundTestProduct.getId()+", product relative path: " + foundTestProduct.getRelativePath()+".");

                    long startTime = System.currentTimeMillis();

                    final File testProductFile = new File(dataRootDir, foundTestProduct.getRelativePath());

                    long startTimeReadProduct = System.currentTimeMillis();

                    final ProductReader productReader = testDefinition.getProductReaderPlugin().createReaderInstance();
                    final Product product = productReader.readProductNodes(testProductFile, null);
                    try {
                        if (logger.isLoggable(Level.FINE)) {
                            double elapsedSecondsReadProduct = (System.currentTimeMillis() - startTimeReadProduct) / 1000.0d;
                            StringBuilder message = new StringBuilder();
                            message.append("Finish reading the product id: ")
                                    .append(foundTestProduct.getId())
                                    .append(", relative path: ")
                                    .append(foundTestProduct.getRelativePath())
                                    .append(", reader class: ")
                                    .append(productReader.getClass().getName())
                                    .append(", size: ")
                                    .append(product.getSceneRasterWidth())
                                    .append("x")
                                    .append(product.getSceneRasterHeight())
                                    .append(", elapsed time: ")
                                    .append(elapsedSecondsReadProduct)
                                    .append(" seconds.");
                            logger.log(Level.FINE, message.toString());
                        }

                        assertExpectedContent(testDefinition, foundTestProduct, product);
                    } catch (Throwable t) {
                        errorCollector.addError(new Throwable("[" + productId + "] " + t.getMessage(), t));
                    } finally {
                        if (product != null) {
                            product.dispose();
                        }
                    }

                    if (logger.isLoggable(Level.FINE)) {
                        double elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000.0d;
                        StringBuilder message = new StringBuilder();
                        message.append("Finish testing the product content id: ")
                                .append(foundTestProduct.getId())
                                .append(", relative path: ")
                                .append(foundTestProduct.getRelativePath())
                                .append(", elapsed time: ")
                                .append(elapsedSeconds)
                                .append(" seconds.");
                        logger.log(Level.FINE, message.toString());
                    }
                } else {
                    logger.info("The test product with id '"+ foundTestProduct.getId()+"' is missing from the disc.");
                }
            }
        }
    }

    private static void assertExpectedContent(TestDefinition testDefinition, TestProduct testProduct, Product product) {
        final ExpectedContent expectedContent = testDefinition.getExpectedContent(testProduct.getId());
        if (expectedContent == null) {
            String message = "No expected content for product id '" + testProduct.getId()+"'.";
            if (FAIL_ON_MISSING_DATA) {
                fail(message);
            } else {
                logger.info(message);
            }
        } else {
            long startTime = System.currentTimeMillis();

            final ContentAssert contentAssert = new ContentAssert(expectedContent, testProduct.getId(), product);
            contentAssert.assertProductContent();

            if (logger.isLoggable(Level.FINE)) {
                double elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000.0d;
                StringBuilder message = new StringBuilder();
                message.append("Finish testing expected content: product id: ")
                        .append(testProduct.getId())
                        .append(", relative path: ")
                        .append(testProduct.getRelativePath())
                        .append(", elapsed time: ")
                        .append(elapsedSeconds)
                        .append(" seconds.");
                logger.log(Level.FINE, message.toString());
            }
        }
    }

    private static TestDefinitionList loadProductReaderTestDefinitions(String readerPlugInClassNameToTest, String productIdToTest) throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        final Iterable<ProductReaderPlugIn> readerPlugIns = SystemUtils.loadServices(ProductReaderPlugIn.class);

        TestDefinitionList testDefinitionList = new TestDefinitionList();
        for (ProductReaderPlugIn readerPlugIn : readerPlugIns) {
            final Class<? extends ProductReaderPlugIn> readerPlugInClass = readerPlugIn.getClass();
            if (readerPlugInClassNameToTest != null && !readerPlugInClass.getName().startsWith(readerPlugInClassNameToTest)) {
                continue;
            }

            final String path = readerPlugInClass.getName().replace(".", "/");
            final String dataResourceName =  "/" + path + "-data.json";
            final URL dataResource = readerPlugInClass.getResource(dataResourceName);
            if (dataResource == null) {
                logger.warning("The reader plugin class '"+readerPlugInClass.getSimpleName() + "' does not define test data.");
                continue;
            }

            final ProductList productList = mapper.readValue(dataResource, ProductList.class);

            final TestDefinition testDefinition = new TestDefinition();
            testDefinition.setProductReaderPlugin(readerPlugIn);
            for (TestProduct testProduct : productList.getAll()) {
                if (productIdToTest == null || productIdToTest.equalsIgnoreCase(testProduct.getId())) {
                    final File productFile = new File(dataRootDir, testProduct.getRelativePath());
                    if (!productFile.exists()) {
                        testProduct.exists(false);
                        String message = "The product '" + productFile.getAbsolutePath()+"' of the reader plugin '"+ readerPlugInClass.getSimpleName()+"' does not exist.";
                        if (FAIL_ON_MISSING_DATA) {
                            fail(message);
                        } else {
                            logger.info(message);
                        }
                    }
                    testDefinition.getAllProducts().add(testProduct);
                    final String fileResourceName = testProduct.getId() + ".json";
                    final URL fileResource = readerPlugInClass.getResource(fileResourceName);
                    if (fileResource == null) {
                        fail("The resource file '" + fileResourceName+"' of the reader plugin '"+ readerPlugInClass.getSimpleName()+"' does not exist.");
                    }
                    final ExpectedDataset expectedDataset = mapper.readValue(fileResource, ExpectedDataset.class);
                    testDefinition.addExpectedDataset(expectedDataset);
                }
            }
            testDefinitionList.add(testDefinition);
        }
        return testDefinitionList;
    }

    private static void assertRequiredTestDataDirectory() {
        String dataDirPropertyValue = System.getProperty(PROPERTY_NAME_DATA_DIR);
        if (dataDirPropertyValue == null) {
            fail("The test data property '"+PROPERTY_NAME_DATA_DIR+"' representing the folder which contains the source products is not set.");
        }
        dataRootDir = new File(dataDirPropertyValue);
        if (!dataRootDir.exists()) {
            fail("The path '" + dataDirPropertyValue+"' representing the folder which contains the source products does not exist.");
        }
        if (!dataRootDir.isDirectory()) {
            fail("The path '" + dataDirPropertyValue+"' exists and it does not denote a folder.");
        }
    }
}
