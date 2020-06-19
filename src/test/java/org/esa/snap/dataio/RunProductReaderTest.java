package org.esa.snap.dataio;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.esa.s2tbx.dataio.gdal.GDALLibraryInstaller;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.dataio.netcdf.NetCdfActivator;
import org.esa.snap.jp2.reader.OpenJPEGLibraryInstaller;
import org.esa.snap.runtime.LogUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.fail;

public class RunProductReaderTest {

    private static final Logger logger = Logger.getLogger(RunProductReaderTest.class.getName());

    private static final String PROPERTY_NAME_FAIL_ON_MISSING_DATA = "snap.reader.tests.failOnMissingData";
    private static final String PROPERTY_NAME_DATA_DIR = "snap.reader.tests.data.dir";
    private static final String PROPERTY_NAME_CASS_NAME = "snap.reader.tests.class.name";
    private static final String PROPERTY_NAME_PRODUCT_IDS = "snap.reader.tests.product.ids";

    private static List<TestDefinition> testDefinitionList;
    private static File dataRootDir;
    private static boolean isFailOnMissingData;

    private ErrorCollector errorCollector = new ErrorCollector();

    public RunProductReaderTest() {
    }

    @BeforeClass
    public static void initialize() throws Exception {
        LogUtils.initLogger();

        String dataDirPropertyValue = System.getProperty(PROPERTY_NAME_DATA_DIR);
        if (dataDirPropertyValue == null) {
            fail("The test data property '"+PROPERTY_NAME_DATA_DIR+"' representing the folder which contains the source products is not set.");
        }

        String readerPlugInClassNameToTest = System.getProperty(PROPERTY_NAME_CASS_NAME);
        String productIdsToTest = System.getProperty(PROPERTY_NAME_PRODUCT_IDS);
        String failOnMissingData = System.getProperty(PROPERTY_NAME_FAIL_ON_MISSING_DATA);

        if (logger.isLoggable(Level.FINE)) {
            StringBuilder message = new StringBuilder();
            message.append("Input test parameters: data folder path: ")
                    .append(dataDirPropertyValue)
                    .append(", reader plugin class: ")
                    .append(readerPlugInClassNameToTest)
                    .append(", product ids: ")
                    .append(productIdsToTest)
                    .append(", fail on missing data: ")
                    .append(failOnMissingData)
                    .append(".");
            logger.log(Level.FINE, message.toString());
        }

        dataRootDir = new File(dataDirPropertyValue);
        if (!dataRootDir.exists()) {
            fail("The path '" + dataDirPropertyValue+"' representing the folder which contains the source products does not exist.");
        }
        if (!dataRootDir.isDirectory()) {
            fail("The path '" + dataDirPropertyValue+"' exists and it does not denote a folder.");
        }

        isFailOnMissingData = true;
        if (failOnMissingData != null) {
            isFailOnMissingData = Boolean.parseBoolean(failOnMissingData);
        }

        SystemUtils.init3rdPartyLibs(RunProductReaderTest.class);

        if (!isFailOnMissingData) {
            logger.warning("Tests will not fail if test data is missing!");
        }

        testDefinitionList = loadProductReaderTestDefinitions(readerPlugInClassNameToTest, productIdsToTest);

        OpenJPEGLibraryInstaller.install();
        GDALLibraryInstaller.install();
        NetCdfActivator.activate();
    }

    /**
     * The required parameters to run the method:
     * -Dsnap.reader.tests.data.dir=\\CV-DEV-SRV01\Satellite_Imagery\TestingJUnitFiles
     *
     * Optional parameters:
     * -Dsnap.reader.tests.class.name=org.esa.s2tbx.dataio.muscate.MuscateProductReaderPlugin
     * -Dsnap.reader.tests.product.ids=MUSCATE-Zip-31TFK,MUSCATE-v18,MUSCATE-v20
     * -Dsnap.reader.tests.failOnMissingData=false
     *
     * @throws Exception
     */
    @Test
    public void testReadIntendedProductContent() throws IOException {
        logger.info("Test product content for " + testDefinitionList.size() + " reader plugins.");

        for (int k=0; k<testDefinitionList.size(); k++) {
            TestDefinition testDefinition = testDefinitionList.get(k);
            if (k > 0) {
                logger.info(""); // write an empty line
            }
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
                    if (isFailOnMissingData) {
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
                    if (product == null) {
                        String message = "The product can not be read from file '" + testProductFile.getAbsolutePath()+"' does not exist.";
                        if (isFailOnMissingData) {
                            fail(message);
                        } else {
                            logger.log(Level.SEVERE, message);
                        }
                    } else {
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
                            product.dispose();
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
            if (isFailOnMissingData) {
                fail(message);
            } else {
                logger.log(Level.SEVERE, message);
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

    private static List<TestDefinition> loadProductReaderTestDefinitions(String readerPlugInClassNameToTest, String productIdsToTest) throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        final Iterable<ProductReaderPlugIn> readerPlugIns = SystemUtils.loadServices(ProductReaderPlugIn.class);

        String[] productIds = null;
        if (productIdsToTest != null) {
            productIds = productIdsToTest.split(",");
            if (productIds.length == 0) {
                throw new IllegalArgumentException("The product ids array is empty.");
            } else {
                for (int k=0; k<productIds.length; k++) {
                    productIds[k] = productIds[k].trim();
                }
            }
        }

        List<TestDefinition> testDefinitionList = new ArrayList<>();
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
                boolean canTestProduct;
                if (productIds == null) {
                    canTestProduct = true;
                } else {
                    canTestProduct = false;
                    for (int k=0; k<productIds.length && !canTestProduct; k++) {
                        if (productIds[k].equalsIgnoreCase(testProduct.getId())) {
                            canTestProduct = true;
                        }
                    }
                }
                if (canTestProduct) {
                    final File productFile = new File(dataRootDir, testProduct.getRelativePath());
                    if (!productFile.exists()) {
                        testProduct.exists(false);
                        String message = "The product '" + productFile.getAbsolutePath()+"' of the reader plugin '"+ readerPlugInClass.getSimpleName()+"' does not exist.";
                        if (isFailOnMissingData) {
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
}
