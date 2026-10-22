package org.librecans.app.ui

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.ListItemShapes
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.librecans.app.ui.components.segmentShapes
import org.librecans.app.ui.theme.LibreCansTheme

/** Rows of a group read as one block: big outer corners, soft inner ones, nothing detached. */
@RunWith(AndroidJUnit4::class)
class SegmentShapesTest {
    @get:Rule
    val compose = createComposeRule()

    private val outer = 28f
    private val inner = 12f

    private fun shapesFor(count: Int): List<ListItemShapes> {
        lateinit var shapes: List<ListItemShapes>
        compose.setContent { LibreCansTheme(dynamicColor = false) { shapes = List(count) { segmentShapes(it, count) } } }
        compose.waitForIdle()
        return shapes
    }

    /** Corner radii in dp, clockwise from top start. */
    private fun Shape.corners(): List<Float> {
        val shape = this as CornerBasedShape
        val size = Size(1000f, 1000f)
        val density = Density(1f)
        return listOf(shape.topStart, shape.topEnd, shape.bottomEnd, shape.bottomStart).map { it.toPx(size, density) }
    }

    @Test
    fun `a single row is rounded all around`() {
        assertThat(shapesFor(1).single().shape.corners()).containsExactly(outer, outer, outer, outer).inOrder()
    }

    @Test
    fun `only the outer edges of a group are rounded`() {
        val (first, middle, last) = shapesFor(3)
        assertThat(first.shape.corners()).containsExactly(outer, outer, inner, inner).inOrder()
        assertThat(middle.shape.corners()).containsExactly(inner, inner, inner, inner).inOrder()
        assertThat(last.shape.corners()).containsExactly(inner, inner, outer, outer).inOrder()
    }

    @Test
    fun `checked and selected rows stay connected to their neighbours`() {
        shapesFor(3).forEach { shapes ->
            assertThat(shapes.selectedShape).isEqualTo(shapes.shape)
            assertThat(shapes.focusedShape).isEqualTo(shapes.shape)
            assertThat(shapes.hoveredShape).isEqualTo(shapes.shape)
        }
    }

    @Test
    fun `a pressed row rounds off completely`() {
        val middle = shapesFor(3)[1]
        assertThat(middle.pressedShape.corners()).containsExactly(outer, outer, outer, outer).inOrder()
    }
}
