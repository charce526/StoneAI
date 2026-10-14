package cn.xzbim.stoneai.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ImageProcessorTest {

    @Test
    fun testCalculateTargetDimensions_largeLandscapeImage() {
        val (w, h) = ImageProcessor.calculateTargetDimensions(4032, 3024, maxEdge = 1600)
        assertEquals(1600, w)
        assertEquals(1200, h)
    }

    @Test
    fun testCalculateTargetDimensions_largePortraitImage() {
        val (w, h) = ImageProcessor.calculateTargetDimensions(3024, 4032, maxEdge = 1600)
        assertEquals(1200, w)
        assertEquals(1600, h)
    }

    @Test
    fun testCalculateTargetDimensions_smallImageNoScaling() {
        val (w, h) = ImageProcessor.calculateTargetDimensions(900, 600, maxEdge = 1600)
        assertEquals(900, w)
        assertEquals(600, h)
    }

    @Test
    fun testCalculateInSampleSize() {
        val sampleSize = ImageProcessor.calculateInSampleSize(4032, 3024, 1600, 1200)
        assertEquals(2, sampleSize)
    }
}
