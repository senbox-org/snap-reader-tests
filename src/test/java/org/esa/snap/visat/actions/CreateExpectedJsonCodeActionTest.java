package org.esa.snap.visat.actions;

import com.fasterxml.jackson.databind.ObjectWriter;
import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.PinDescriptor;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Placemark;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.dataio.ExpectedMetadata;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.OngoingStubbing;

import javax.media.jai.operator.ConstantDescriptor;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * @author Marco Peters
 */
public class CreateExpectedJsonCodeActionTest {

    private static final String JSON_CODE_RESOURCE = "EXPECTED_JSON_CODE.json";
    private static final String JSON_CODE_PINS_RESOURCE = "EXPECTED_JSON_PINS_CODE.json";
    private static final String LF = System.getProperty("line.separator");
    private static String EXPECTED_JSON_CODE;
    private static String EXPECTED_JSON_PINS_CODE;
    private static Product product;
    private static final int WIDTH = 10;
    private static final int HEIGHT = 20;
    private static final Point[] EXPECTED_PIXEL_POSES = new Point[]{
            new Point(7, 6),
            new Point(0, 0),
            new Point(5, 6),
            new Point(8, 8),
            new Point(0, 2),
            new Point(8, 11),
            new Point(2, 10)
    };

    @BeforeClass
    public static void setUpClass() throws Exception {
        Assume.assumeFalse("Cannot run in headless", GraphicsEnvironment.isHeadless());
        EXPECTED_JSON_CODE = loadJSONCode(JSON_CODE_RESOURCE);
        EXPECTED_JSON_PINS_CODE = loadJSONCode(JSON_CODE_PINS_RESOURCE);
    }

    @Before
    public void setUp() throws Exception {
        product = new Product("Hans Wurst", "T", WIDTH, HEIGHT);
        product.setStartTime(ProductData.UTC.parse("23-AUG-1983 12:10:10"));
        product.setEndTime(ProductData.UTC.parse("23-AUG-1983 12:14:41"));
        product.setSceneGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, WIDTH, HEIGHT, 0.0, 0.0, 1.0, -1.0));
        ProductReader readerMock = Mockito.mock(ProductReader.class);
        ProductReaderPlugIn plugInMock = Mockito.mock(ProductReaderPlugIn.class);
        Mockito.when(readerMock.getReaderPlugIn()).thenReturn(plugInMock);
        Mockito.when(plugInMock.getDecodeQualification(Mockito.any())).thenReturn(DecodeQualification.INTENDED);
        product.setProductReader(readerMock);
        final Band band1 = product.addBand("band_1", ProductData.TYPE_INT32);
        band1.setSourceImage(ConstantDescriptor.create((float) WIDTH, (float) HEIGHT, new Integer[]{1}, null));
        band1.setDescription("description_1");
        band1.setUnit("abc");
        band1.setGeophysicalNoDataValue(1);
        band1.setNoDataValueUsed(true);
        final Band band2 = product.addBand("band_2", ProductData.TYPE_FLOAT32);
        band2.setDescription("description_2");
        band2.setUnit("m/w^2");
        band2.setSourceImage(ConstantDescriptor.create((float) WIDTH, (float) HEIGHT, new Float[]{2.0f}, null));
        final MetadataElement metadataRoot = product.getMetadataRoot();
        final MetadataElement test1Element = new MetadataElement("test_1");
        final MetadataElement abc_1 = new MetadataElement("ABC");
        abc_1.addAttribute(new MetadataAttribute("Name", ProductData.createInstance("ABC_1"), true));
        test1Element.addElement(abc_1);
        final MetadataElement abc_2 = new MetadataElement("ABC");
        abc_2.addAttribute(new MetadataAttribute("Name", ProductData.createInstance("ABC_2"), true));
        test1Element.addElement(abc_2);
        final MetadataElement abc_3 = new MetadataElement("ABC");
        abc_3.addAttribute(new MetadataAttribute("Name", ProductData.createInstance("ABC_3"), true));
        test1Element.addElement(abc_3);
        final MetadataElement abc_4 = new MetadataElement("ABC");
        abc_4.addAttribute(new MetadataAttribute("Name", ProductData.createInstance("ABC_4"), true));
        test1Element.addElement(abc_4);
        metadataRoot.addElement(test1Element);
        MetadataElement test2Element = new MetadataElement("test_2");
        test2Element.addAttribute(new MetadataAttribute("attrib", ProductData.createInstance("abc"), true));
        test2Element.addAttribute(new MetadataAttribute("attrib", ProductData.createInstance("def"), true));
        test2Element.addAttribute(new MetadataAttribute("attrib", ProductData.createInstance("ghi"), true));
        metadataRoot.addElement(test2Element);

    }

    private static String loadJSONCode(String jsonCodeResource) throws IOException {
        final InputStream resourceAsStream = CreateExpectedJsonCodeActionTest.class.getResourceAsStream(jsonCodeResource);

        final InputStreamReader reader = new InputStreamReader(resourceAsStream);
        BufferedReader r = new BufferedReader(reader);
        String line = r.readLine();
        final StringBuilder sb = new StringBuilder();
        while (line != null) {
            sb.append(line);
            line = r.readLine();
            if (line != null) {
                sb.append(LF);
            }
        }
        return sb.toString();
    }

    @Test
    public void testCreatedJson() throws Exception {
        final CreateExpectedJsonCodeAction jsonCodeCommand = new CreateExpectedJsonCodeAction(product);
        final Random mock = createMockedRandom();
        final String actualJsonCode = jsonCodeCommand.createJsonCode(mock);
        assertEquals(EXPECTED_JSON_CODE, actualJsonCode);
    }

    @Test
    public void testCreatedJsonWith_Pins() throws Exception {
        final CreateExpectedJsonCodeAction jsonCodeCommand = new CreateExpectedJsonCodeAction(product);
        GeoCoding geoCoding = product.getSceneGeoCoding();
        PixelPos pinPos1 = new PixelPos(3, 7);
        Placemark pin1 = Placemark.createPointPlacemark(PinDescriptor.getInstance(), "P1", "L1", "T1", pinPos1,
                                                        geoCoding.getGeoPos(pinPos1, null), geoCoding);
        PixelPos pinPos2 = new PixelPos(8, 16.5f);
        Placemark pin2 = Placemark.createPointPlacemark(PinDescriptor.getInstance(), "P2", "L2", "T2", pinPos2,
                                                        geoCoding.getGeoPos(pinPos2, null), geoCoding);
        product.getPinGroup().add(pin1);
        product.getPinGroup().add(pin2);

        final String actualJsonCode = jsonCodeCommand.createJsonCode(new Random(12345));
        assertEquals(EXPECTED_JSON_PINS_CODE, actualJsonCode);
    }

    @Test
    public void testFillClipboardWithJsonCode() throws Exception {
        final Clipboard clipboard = new Clipboard("testClipboard");
        final CreateExpectedJsonCodeAction jsonCodeCommand = new CreateExpectedJsonCodeAction(product);
        jsonCodeCommand.setClipboard(clipboard);
        jsonCodeCommand.fillClipboardWithJsonCode(createMockedRandom());

        assertTrue(clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor));

        String actualJsonCode = (String) clipboard.getData(DataFlavor.stringFlavor);
        assertNotNull(actualJsonCode);
        assertEquals(EXPECTED_JSON_CODE, actualJsonCode);
    }

    @Test
    public void testIdGeneration() {
        final CreateExpectedJsonCodeAction jsonCodeCommand = new CreateExpectedJsonCodeAction(product);
        final Product testProduct = new Product("blah test.1", "d", 3, 3);
        assertEquals("blah_test_1", jsonCodeCommand.generateID(testProduct));
        testProduct.setName("B  L..A H");
        assertEquals("B__L__A_H", jsonCodeCommand.generateID(testProduct));
    }

    private Random createMockedRandom() {
        final Random mock = Mockito.mock(Random.class);
        OngoingStubbing<Float> ongoingStubbing = when(mock.nextFloat());
        for (Point coord : EXPECTED_PIXEL_POSES) {
            ongoingStubbing = ongoingStubbing.thenReturn(coord.x / (float) WIDTH);
            ongoingStubbing = ongoingStubbing.thenReturn(coord.y / (float) HEIGHT);
        }
        return mock;
    }


    @Test
    public void testJsonWriter() throws Exception {
        ObjectWriter jsonWriter = CreateExpectedJsonCodeAction.getConfiguredJsonWriter();
        ExpectedMetadata expectedMetadata = new ExpectedMetadata();
        expectedMetadata.setPath("DSD/DSD.21/Name");
        expectedMetadata.setValue("");
        StringWriter writer = new StringWriter(500);
        jsonWriter.writeValue(writer, expectedMetadata);

        String expectedString = "{" + CreateExpectedJsonCodeAction.LF +
                          "    \"path\": \"DSD/DSD.21/Name\"," + CreateExpectedJsonCodeAction.LF +
                          "    \"value\": \"\"" + CreateExpectedJsonCodeAction.LF +
                          "}";
        String actualString = writer.getBuffer().toString();
        assertEquals(expectedString, actualString);

    }
}
