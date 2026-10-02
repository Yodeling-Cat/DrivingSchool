package com.spiraclestudios.autoskola.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape

/**
 * Where the question bank keeps its traffic-situation drawings: top-down views of roads and
 * junctions, drawn edge to edge with no margin of their own.
 */
private const val SITUATION_DIR = "obr3/ds/"

/**
 * Whether a picture is one of those drawings.
 *
 * They are scenes that run to their own edges, so a rounded corner would cut the road off; a sign,
 * a pictogram or a sign element sits on a plain background and is rounded like every other card.
 * The folder is matched with its trailing separator so a likewise-named folder is not one of them.
 */
internal fun isSituationDrawing(uri: String?): Boolean = uri?.replace('\\', '/')?.contains(SITUATION_DIR) ?: false

/**
 * How a picture of the bank is clipped, wherever it is drawn: a drawing fills its frame and is cut
 * square, so no road is cut off at a corner; everything else is rounded like the cards around it.
 */
internal fun pictureShape(uri: String?): Shape =
    if (isSituationDrawing(uri)) RectangleShape else RoundedCornerShape(ImageCornerRadius)
