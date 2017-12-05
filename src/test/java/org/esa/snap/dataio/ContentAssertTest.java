package org.esa.snap.dataio;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Marco Peters
 */
public class ContentAssertTest {

    @Test
    public void computeAssertDelta() throws Exception {
        double value = ContentAssert.computeAssertDelta(123.456789123);
        assertEquals(0.0001235, value, 1.0e-8);

        double value2 = ContentAssert.computeAssertDelta(1.345e-3);
        assertEquals(0.000001, value2, 1.0e-8);
    }

}