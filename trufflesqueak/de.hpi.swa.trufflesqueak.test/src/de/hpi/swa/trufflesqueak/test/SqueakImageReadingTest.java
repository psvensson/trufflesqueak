package de.hpi.swa.trufflesqueak.test;

import org.junit.Test;

import de.hpi.swa.trufflesqueak.util.SqueakImageChunk;

public class SqueakImageReadingTest extends AbstractSqueakTestCase {
    @Test
    public void testFloatDecoding() {
        SqueakImageChunk chunk = newFloatChunk();
        chunk.data().add(0);
        chunk.data().add(1072693248);
        assertEquals(1.0, chunk.asObject());

        chunk = newFloatChunk();
        chunk.data().add((int) 2482401462L);
        chunk.data().add(1065322751);
        assertEquals(0.007699011184197404, chunk.asObject());

        chunk = newFloatChunk();
        chunk.data().add(876402988);
        chunk.data().add(1075010976);
        assertEquals(4.841431442464721, chunk.asObject());
    }

    private static SqueakImageChunk newFloatChunk() {
        SqueakImageChunk chunk = new SqueakImageChunk(
                        null,
                        image,
                        2, // 2 words
                        10, // float format, 32-bit words without padding word
                        34, // classid of BoxedFloat64
                        3833906, // identityHash for 1.0
                        0 // position
        );
        chunk.setSqClass(image.floatClass);
        return chunk;
    }
}
