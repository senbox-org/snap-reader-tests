package org.esa.snap.dataio;


import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.IndexCoding;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.datamodel.SampleCoding;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.util.StringUtils;
import org.geotools.geometry.DirectPosition2D;
import org.junit.Assert;
import org.opengis.referencing.operation.MathTransform;

import java.awt.geom.AffineTransform;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

class ContentAssert {

    private final ExpectedContent expectedContent;
    private final String productId;
    private final Product product;

    ContentAssert(ExpectedContent expectedContent, String productId, Product product) {
        this.expectedContent = expectedContent;
        this.productId = productId;
        this.product = product;
    }

    void assertProductContent() {
        assertExpectedProductProperties(expectedContent, productId, product);
        testExpectedGeoCoding(expectedContent, productId, product);
        testExpectedFlagCoding(expectedContent, productId, product);
        testExpectedIndexCoding(expectedContent, productId, product);
        testExpectedTiePointGrids(expectedContent, productId, product);
        testExpectedBands(expectedContent, productId, product);
        testExpectedMasks(expectedContent, productId, product);
        testExpectedMetadata(expectedContent, productId, product);
    }

    private static void assertExpectedProductProperties(ExpectedContent expectedContent, String productId, Product product) {
        if (expectedContent.isSceneWidthSet()) {
            Assert.assertEquals(productId + " SceneWidth", expectedContent.getSceneWidth(), product.getSceneRasterWidth());
        }
        if (expectedContent.isSceneHeightSet()) {
            Assert.assertEquals(productId + " SceneHeight", expectedContent.getSceneHeight(), product.getSceneRasterHeight());
        }
        if (expectedContent.isStartTimeSet()) {
            Assert.assertEquals(productId + " StartTime", expectedContent.getStartTime(), product.getStartTime().format());
        }
        if (expectedContent.isEndTimeSet()) {
            Assert.assertEquals(productId + " EndTime", expectedContent.getEndTime(), product.getEndTime().format());
        }
    }

    private static void testExpectedGeoCoding(ExpectedContent expectedContent, String productId, Product product) {
        if (expectedContent.isGeoCodingSet()) {
            final ExpectedGeoCoding expectedGeoCoding = expectedContent.getGeoCoding();
            final GeoCoding geoCoding = product.getSceneGeoCoding();
            assertNotNull(productId + " has no GeoCoding", geoCoding);

            final Double reverseAccuracy = expectedGeoCoding.getReverseAccuracy();
            final ExpectedGeoCoordinate[] coordinates = expectedGeoCoding.getCoordinates();
            if (coordinates == null) {
                return;
            }
            for (ExpectedGeoCoordinate coordinate : coordinates) {
                final PixelPos expectedPixelPos = coordinate.getPixelPos();
                final GeoPos expectedGeoPos = coordinate.getGeoPos();
                final GeoPos actualGeoPos = geoCoding.getGeoPos(expectedPixelPos, null);
                final String message = productId + " GeoPos at Pixel(" + expectedPixelPos.getX() + "," + expectedPixelPos.getY() + ")";
                assertEquals(message, expectedGeoPos.getLat(), actualGeoPos.getLat(), 1e-6);
                assertEquals(message, expectedGeoPos.getLon(), actualGeoPos.getLon(), 1e-6);

                if (reverseAccuracy >= 0) {
                    final PixelPos actualPixelPos = geoCoding.getPixelPos(actualGeoPos, null);
                    assertEquals(productId + " Pixel.X at GeoPos(" + actualGeoPos.getLat() + "," + actualGeoPos.getLon() + ")",
                            expectedPixelPos.getX(), actualPixelPos.getX(), reverseAccuracy);
                    assertEquals(productId + " Pixel.Y at GeoPos(" + actualGeoPos.getLat() + "," + actualGeoPos.getLon() + ")",
                            expectedPixelPos.getY(), actualPixelPos.getY(), reverseAccuracy);
                }
            }
        }
    }

    private static void testExpectedFlagCoding(ExpectedContent expectedContent, String productId, Product product) {
        final ProductNodeGroup<FlagCoding> flagCodingGroup = product.getFlagCodingGroup();
        for (ExpectedSampleCoding expectedFlagCoding : expectedContent.getFlagCodings()) {
            final String name = expectedFlagCoding.getName();
            final FlagCoding actualFlagCoding = flagCodingGroup.get(name);
            final String msgPrefix = productId + " FlagCoding '" + name;
            assertNotNull(msgPrefix + "' does not exist", flagCodingGroup.contains(name));
            assertEqualSampleCodings(msgPrefix, expectedFlagCoding, actualFlagCoding);
        }
    }

    private static void assertEqualSampleCodings(String msgPrefix, ExpectedSampleCoding expectedSampleCoding, SampleCoding actualSampleCoding) {
        final ExpectedSample[] expectedSamples = expectedSampleCoding.getSamples();
        Assert.assertEquals(msgPrefix + "' number of samples", expectedSamples.length, actualSampleCoding.getNumAttributes());
        for (ExpectedSample expectedSample : expectedSamples) {
            final String expectedSampleName = expectedSample.getName();
            final MetadataAttribute actualSample = actualSampleCoding.getAttribute(expectedSampleName);
            assertNotNull(msgPrefix + " sample '" + expectedSampleName + "' does not exist", actualSample);
            assertEquals(msgPrefix + " sample '" + expectedSampleName + "' Value",
                    expectedSample.getValue(), actualSample.getData().getElemUInt());

            final String expectedSampleDescription = expectedSample.getDescription();
            if (StringUtils.isNotNullAndNotEmpty(expectedSampleDescription)) {
                assertEquals(msgPrefix + " sample '" + expectedSampleName + "' Description",
                        expectedSampleDescription, actualSample.getDescription());
            }
        }
    }

    private static void testExpectedIndexCoding(ExpectedContent expectedContent, String productId, Product product) {
        final ProductNodeGroup<IndexCoding> indexCodingGroup = product.getIndexCodingGroup();
        for (ExpectedSampleCoding expectedIndexCoding : expectedContent.getIndexCodings()) {
            final String name = expectedIndexCoding.getName();
            final IndexCoding actualIndexCoding = indexCodingGroup.get(name);
            final String msgPrefix = productId + " IndexCoding '" + name;
            assertNotNull(msgPrefix + "' does not exist", actualIndexCoding);
            assertEqualSampleCodings(msgPrefix, expectedIndexCoding, actualIndexCoding);
        }
    }

    private static void testExpectedTiePointGrids(ExpectedContent expectedContent, String productId, Product product) {
        final ExpectedTiePointGrid[] tiePointGrids = expectedContent.getTiePointGrids();
        for (ExpectedTiePointGrid tiePointGrid : tiePointGrids) {
            testExpectedTiePointGrid(productId, tiePointGrid, product);
        }
    }

    private static void testExpectedBands(ExpectedContent expectedContent, String productId, Product product) {
        final ExpectedBand[] expectedBands = expectedContent.getBands();
        for (final ExpectedBand expectedBand : expectedBands) {
            testExpectedBand(productId, expectedBand, product);
        }
    }

    private static void testExpectedTiePointGrid(String productId, ExpectedTiePointGrid expectedTiePointGrid, Product product) {
        final TiePointGrid tiePointGrid = product.getTiePointGrid(expectedTiePointGrid.getName());
        assertNotNull("missing tie-point grid '" + expectedTiePointGrid.getName() + " in product '" + product.getFileLocation(), tiePointGrid);

        final String assertMessagePrefix = productId + " " + tiePointGrid.getName();

        if (expectedTiePointGrid.isDescriptionSet()) {
            assertEquals(assertMessagePrefix + " Description", expectedTiePointGrid.getDescription(), tiePointGrid.getDescription());
        }

        if (expectedTiePointGrid.isOffsetXSet()) {
            assertEquals(assertMessagePrefix + " OffsetX", expectedTiePointGrid.getOffsetX(), tiePointGrid.getOffsetX(), 1e-6);
        }

        if (expectedTiePointGrid.isOffsetYSet()) {
            assertEquals(assertMessagePrefix + " OffsetY", expectedTiePointGrid.getOffsetY(), tiePointGrid.getOffsetY(), 1e-6);
        }

        if (expectedTiePointGrid.isSubSamplingXSet()) {
            assertEquals(assertMessagePrefix + " SubSamplingX", expectedTiePointGrid.getSubSamplingX(), tiePointGrid.getSubSamplingX(), 1e-6);
        }

        if (expectedTiePointGrid.isSubSamplingYSet()) {
            assertEquals(assertMessagePrefix + " SubSamplingY", expectedTiePointGrid.getSubSamplingY(), tiePointGrid.getSubSamplingY(), 1e-6);
        }
        final ExpectedPixel[] expectedPixel = expectedTiePointGrid.getExpectedPixels();
        for (ExpectedPixel pixel : expectedPixel) {
            float bandValue = tiePointGrid.getSampleFloat(pixel.getX(), pixel.getY());
            if (!tiePointGrid.isPixelValid(pixel.getX(), pixel.getY())) {
                bandValue = Float.NaN;
            }
            Assert.assertEquals(assertMessagePrefix + " Pixel(" + pixel.getX() + "," + pixel.getY() + ")", pixel.getValue(), bandValue, 1e-6);
        }
    }

    private static void testExpectedBand(String productId, ExpectedBand expectedBand, Product product) {
        final Band band = product.getBand(expectedBand.getName());
        assertNotNull("missing band '" + expectedBand.getName() + " in product '" + productId, band);

        final String messagePrefix = productId + " " + band.getName();
        if (expectedBand.isDescriptionSet()) {
            Assert.assertEquals(messagePrefix + " Description", expectedBand.getDescription(), band.getDescription());
        }

        if (expectedBand.isGeophysicalUnitSet()) {
            Assert.assertEquals(messagePrefix + " Unit", expectedBand.getGeophysicalUnit(), band.getUnit());
        }

        if (expectedBand.isNoDataValueSet()) {
            final double expectedNDValue = Double.parseDouble(expectedBand.getNoDataValue());
            Assert.assertEquals(messagePrefix + " NoDataValue", expectedNDValue, band.getGeophysicalNoDataValue(), 1e-6);
        }

        if (expectedBand.isNoDataValueUsedSet()) {
            final boolean expectedNDUsedValue = Boolean.parseBoolean(expectedBand.isNoDataValueUsed());
            Assert.assertEquals(messagePrefix + " NoDataValueUsed", expectedNDUsedValue, band.isNoDataValueUsed());
        }

        if (expectedBand.isSpectralWavelengthSet()) {
            final float expectedSpectralWavelength = Float.parseFloat(expectedBand.getSpectralWavelength());
            Assert.assertEquals(messagePrefix + " SpectralWavelength", expectedSpectralWavelength, band.getSpectralWavelength(), 1e-6);
        }

        if (expectedBand.isSpectralBandWidthSet()) {
            final float expectedSpectralBandwidth = Float.parseFloat(expectedBand.getSpectralBandwidth());
            Assert.assertEquals(messagePrefix + " SpectralBandWidth", expectedSpectralBandwidth, band.getSpectralBandwidth(), 1e-6);
        }

        final ExpectedPixel[] expectedPixel = expectedBand.getExpectedPixels();
        for (ExpectedPixel pixel : expectedPixel) {
            int pixelX = pixel.getX();
            int pixelY = pixel.getY();
            String pixelString = " Pixel(" + pixel.getX() + "," + pixel.getY() + ")";
            try {
                if (product.isMultiSizeProduct() && product.isSceneCrsASharedModelCrs()) {
                    // todo - [multiSize] it is not generic enough but sufficient for now (mp - 20151120)
                    final MathTransform sceneI2mTransform = product.getSceneGeoCoding().getImageToMapTransform();
                    final AffineTransform bandM2iTransform = band.getImageToModelTransform().createInverse();

                    final DirectPosition2D sceneModelPos = new DirectPosition2D();
                    sceneI2mTransform.transform(new DirectPosition2D(pixelX, pixelY), sceneModelPos);
                    final DirectPosition2D bandImagePos = new DirectPosition2D();
                    bandM2iTransform.transform(sceneModelPos, bandImagePos);
                    pixelX = (int) Math.floor(bandImagePos.getX());
                    pixelY = (int) Math.floor(bandImagePos.getY());
                    pixelString = pixelString + " transf(" + pixelX + "," + pixelY + ")";
                }
                float bandValue;
                if (band.isPixelValid(pixelX, pixelY)) {
                    bandValue = band.getSampleFloat(pixelX, pixelY);
                } else {
                    bandValue = Float.NaN;
                }
                Assert.assertEquals(messagePrefix + pixelString, pixel.getValue(), bandValue, 1e-6);
            } catch (Exception e) {
                final StringWriter stackTraceWriter = new StringWriter();
                e.printStackTrace(new PrintWriter(stackTraceWriter));
                Assert.fail(messagePrefix + pixelString + "- caused " + e.getClass().getSimpleName() + "\n" +
                            stackTraceWriter.toString());
            }
        }
    }

    private static void assertEqualMasks(String msgPrefix, ExpectedMask expectedMask, Mask actualMask) {
        assertEquals(msgPrefix + " Type", expectedMask.getType(), actualMask.getImageType().getClass());
        assertEquals(msgPrefix + " Color", expectedMask.getColor(), actualMask.getImageColor());
        final String expectedMaskDescription = expectedMask.getDescription();
        if (StringUtils.isNotNullAndNotEmpty(expectedMaskDescription)) {
            assertEquals(msgPrefix + " Description",expectedMaskDescription, actualMask.getDescription());
        }
    }

    private static void testExpectedMasks(ExpectedContent expectedContent, String productId, Product product) {
        ExpectedMask[] expectedMasks = expectedContent.getMasks();
        final ProductNodeGroup<Mask> actualMaskGroup = product.getMaskGroup();
        for (ExpectedMask expectedMask : expectedMasks) {
            final String expectedName = expectedMask.getName();
            final Mask actualMask = actualMaskGroup.get(expectedName);
            final String msgPrefix = productId + " Mask '" + expectedName + "' ";
            assertNotNull(msgPrefix + "' does not exist", actualMask);
            assertEqualMasks(msgPrefix, expectedMask, actualMask);
        }
    }

    private static MetadataElement getMetadataElement(String msgPrefix, String[] pathElementNames, MetadataElement rootElement) {
        MetadataElement element = rootElement;
        final Pattern pattern = Pattern.compile("(.*)\\[(\\d++)\\]");
        for (String elementName : pathElementNames) {
            final Matcher matcher = pattern.matcher(elementName);
            if (matcher.matches()) {
                elementName = matcher.group(1);
                int elemIndex = Integer.parseInt(matcher.group(2)); // following XPath, the index is one based
                assertTrue(msgPrefix + " Index must be >= 1", elemIndex >= 1);
                final MetadataElement[] elements = element.getElements();
                for (MetadataElement elem : elements) {
                    if (elem.getName().equals(elementName)) {
                        elemIndex--;
                        if (elemIndex == 0) {
                            element = elem;
                            break;
                        }
                    }
                }
            } else {
                element = element.getElement(elementName);
            }
            assertNotNull(msgPrefix + " Element '" + elementName + "' not found", element);
        }
        return element;
    }

    private static MetadataAttribute getMetadataAttribute(String msgPrefix, MetadataElement currentElement, String attributeName) {
        final Pattern pattern = Pattern.compile("(.*)\\[(\\d++)\\]");
        final Matcher matcher = pattern.matcher(attributeName);
        if (matcher.matches()) {
            String attributeBaseName = matcher.group(1);
            int attribIndex = Integer.parseInt(matcher.group(2)); // following XPath, the index is one based
            assertTrue(msgPrefix + " Index must be >= 1", attribIndex >= 1);
            final MetadataAttribute[] attributes = currentElement.getAttributes();
            for (MetadataAttribute attrib : attributes) {
                if (attrib.getName().equals(attributeBaseName)) {
                    attribIndex--;
                    if (attribIndex == 0) {
                        return attrib;
                    }
                }
            }
        }
        return currentElement.getAttribute(attributeName);
    }

    private static void testExpectedMetadata(ExpectedContent expectedContent, String productId, Product product) {
        ExpectedMetadata[] expectedMetadataArray = expectedContent.getMetadata();
        for (ExpectedMetadata expectedMetadata : expectedMetadataArray) {
            String path = expectedMetadata.getPath();
            final String[] pathTokens = getPathTokens(path);
            final String[] elementNames = Arrays.copyOf(pathTokens, pathTokens.length - 1);
            final String msgPrefix = productId + " Metadata path '" + path + "' not valid.";
            MetadataElement currentElement = getMetadataElement(msgPrefix, elementNames, product.getMetadataRoot());
            final String attributeName = pathTokens[pathTokens.length - 1];
            final MetadataAttribute attribute = getMetadataAttribute(msgPrefix, currentElement, attributeName);
            assertNotNull(msgPrefix + " Attribute '" + attributeName + "' not found", attribute);
            assertEquals(msgPrefix + " Value", expectedMetadata.getValue(), attribute.getData().getElemString());

        }
    }

    private static String[] getPathTokens(String path) {
        String[] splits = path.split("/");
        ArrayList<String> tokens = new ArrayList<String>();
        for (int i = 0; i < splits.length; i++) {
            String currentSplit = splits[i];
            if (currentSplit.isEmpty()) {
                tokens.add(tokens.remove(tokens.size() - 1) + "/" + splits[++i]);
            } else {
                tokens.add(currentSplit);
            }
        }
        return tokens.toArray(new String[tokens.size()]);
    }
}
