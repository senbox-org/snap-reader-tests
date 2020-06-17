package org.esa.snap.dataio;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.gpt.RunGPTProductReaderTest;
import org.esa.snap.lib.openjpeg.activator.OpenJPEGInstaller;
import org.esa.snap.runtime.LogUtils;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.fail;

public class RunProductReaderTest {

    private static final String PROPERTYNAME_FAIL_ON_MISSING_DATA = "snap.reader.tests.failOnMissingData";
    private static final String PROPERTY_NAME_DATA_DIR = "snap.reader.tests.data.dir";

    private static final String PROPERTY_NAME_CASS_NAME = "snap.reader.tests.class.name";
    private static final boolean FAIL_ON_MISSING_DATA = Boolean.parseBoolean(System.getProperty(PROPERTYNAME_FAIL_ON_MISSING_DATA, "true"));
    private static final String INDENT = "\t";

    private static TestDefinitionList testDefinitionList;
    private static File dataRootDir;

    private static final Logger logger = Logger.getLogger(RunProductReaderTest.class.getName());

    @Rule
    public ErrorCollector errorCollector = new ErrorCollector();
    private static final ProductList testProductList = new ProductList();

    public RunProductReaderTest() {
    }

    @BeforeClass
    public static void initialize() throws Exception {
        LogUtils.initLogger();

        logFailOnMissingDataMessage();

        assertTestDataDirectory();
        testDefinitionList = loadProductReaderTestDefinitions();

        createGlobalProductList();

        OpenJPEGInstaller.install();
    }

    @Test
    public void testReadIntendedProductContent() throws IOException {
        logger.info("Test product content for " + testDefinitionList.size() + " reader plugins.");

        for (TestDefinition testDefinition : testDefinitionList) {
            logger.info("Start testing reader plugin: " + testDefinition.getProductReaderPlugin().getClass().getSimpleName() + ".");
            long startTime = System.currentTimeMillis();

            final List<String> intendedProductIds = testDefinition.getDecodableProductIds();
            for (String productId : intendedProductIds) {
                final TestProduct testProduct = testProductList.getById(productId);
                String reason = "Test file not defined for ID=" + productId;
                errorCollector.checkThat(reason, testProduct, is(notNullValue()));
                if (testProduct == null) {
                    continue;
                }
                if (testProduct.exists()) {
                    final File testProductFile = getTestProductFile(testProduct);

                    final ProductReader productReader = testDefinition.getProductReaderPlugin().createReaderInstance();

                    final Product product = productReader.readProductNodes(testProductFile, null);
                    try {
                        assertExpectedContent(testDefinition, productId, product);
                    } catch (Throwable t) {
                        errorCollector.addError(new Throwable("[" + productId + "] " + t.getMessage(), t));
                    } finally {
                        if (product != null) {
                            product.dispose();
                        }
                    }
                } else {
                    logProductNotExistent(2, testProduct);
                }
            }
            double elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000.0d;
            logger.info("Finish testing reader plugin: " + testDefinition.getProductReaderPlugin().getClass().getSimpleName() + ". The time elapsed is " + elapsedSeconds + " seconds.");
        }
    }

    private static void createGlobalProductList() {
        for (TestDefinition testDefinition : testDefinitionList) {
            final List<TestProduct> allPluginProducts = testDefinition.getAllProducts();
            for (TestProduct testProduct : allPluginProducts) {
                if (!testIfIdAlreadyRegistered(testProduct)) {
                    testProductList.add(testProduct);
                }
            }
        }
    }

    private static boolean testIfIdAlreadyRegistered(TestProduct testProduct) {
        final String id = testProduct.getId();
        final TestProduct storedProduct = testProductList.getById(id);
        if (storedProduct != null) {
            if (storedProduct.isDifferent(testProduct)) {
                fail("Test file with ID=" + id + " already defined with different settings");
            }

            return true;
        }

        return false;
    }

    private void logProductNotExistent(int indention, TestProduct testProduct) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indention; i++) {
            sb.append(INDENT);
        }
        logger.info(sb.toString() + "Not existent - " + testProduct.getId());
    }

    private static void assertExpectedContent(TestDefinition testDefinition, String productId, Product product) {
        final ExpectedContent expectedContent = testDefinition.getExpectedContent(productId);
        if (expectedContent == null) {
            logger.info("No expected content for product id '" + productId+"'.");
        } else {
            long startTime = System.currentTimeMillis();
            logger.info("Start testing expected content: product id: " + productId+".");

            final ContentAssert contentAssert = new ContentAssert(expectedContent, productId, product);
            contentAssert.assertProductContent();

            double elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000.0d;
            logger.info("Finish testing expected content: product id: " + productId+"'. The time elapsed is " + elapsedSeconds + " seconds.");
        }
    }

    private File getTestProductFile(TestProduct testProduct) {
        final String relativePath = testProduct.getRelativePath();
        final File testProductFile = new File(dataRootDir, relativePath);

        errorCollector.checkThat("testProductFile exist " + testProduct.getId(), testProductFile.exists(), is(true));
        return testProductFile;
    }

    private static TestDefinitionList loadProductReaderTestDefinitions() throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        final Iterable<ProductReaderPlugIn> readerPlugIns = SystemUtils.loadServices(ProductReaderPlugIn.class);
        final String className = System.getProperty(PROPERTY_NAME_CASS_NAME);

        TestDefinitionList testDefinitionList = new TestDefinitionList();
        for (ProductReaderPlugIn readerPlugIn : readerPlugIns) {
            final Class<? extends ProductReaderPlugIn> readerPlugInClass = readerPlugIn.getClass();
            if (className != null && !readerPlugInClass.getName().startsWith(className)) {
                continue;
            }

            final String path = readerPlugInClass.getName().replace(".", "/");
            final String dataResourceName =  "/" + path + "-data.json";
            final URL dataResource = readerPlugInClass.getResource(dataResourceName);
            if (dataResource == null) {
                logger.warning(readerPlugInClass.getSimpleName() + " does not define test data");
                continue;
            }

            final TestDefinition testDefinition = new TestDefinition();
            testDefinition.setProductReaderPlugin(readerPlugIn);

            final ProductList productList = mapper.readValue(dataResource, ProductList.class);
            testIfProductFilesExists(productList);
            testDefinition.addTestProducts(productList.getAll());

            for (TestProduct product : productList.getAll()) {
                final String fileResourceName = product.getId() + ".json";
                final URL fileResource = readerPlugInClass.getResource(fileResourceName);
                if (fileResource == null) {
                    fail(readerPlugInClass.getSimpleName() + " resource file '" + fileResourceName + "' is missing");
                }
                final ExpectedDataset expectedDataset = mapper.readValue(fileResource, ExpectedDataset.class);
                testDefinition.addExpectedDataset(expectedDataset);
            }
            testDefinitionList.add(testDefinition);
        }
        return testDefinitionList;
    }

    private static void assertTestDataDirectory() {
        final String dataDirProperty = System.getProperty(PROPERTY_NAME_DATA_DIR);
        if (dataDirProperty == null) {
            fail("Data directory path not set");
        }
        dataRootDir = new File(dataDirProperty);
        if (!dataRootDir.isDirectory()) {
            fail("Data directory is not valid: " + dataDirProperty);
        }
    }

    private static void testIfProductFilesExists(ProductList productList) {
        for (TestProduct testProduct : productList) {
            final String relativePath = testProduct.getRelativePath();
            final File productFile = new File(dataRootDir, relativePath);
            if (!productFile.exists()) {
                testProduct.exists(false);
                if (FAIL_ON_MISSING_DATA) {
                    fail("Test product does not exist: " + productFile.getAbsolutePath());
                }
            }
        }
    }

    private static void logFailOnMissingDataMessage() {
        if (!FAIL_ON_MISSING_DATA) {
            logger.warning("Tests will not fail if test data is missing!");
        }
    }
}
