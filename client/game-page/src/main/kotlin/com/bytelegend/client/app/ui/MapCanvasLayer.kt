@file:Suppress("UnsafeCastFromDynamic")

package com.bytelegend.client.app.ui

import com.bytelegend.app.client.api.Timestamp
import com.bytelegend.app.client.api.getImageElement
import com.bytelegend.app.shared.PixelBlock
import com.bytelegend.client.app.engine.GameAnimationEventListener
import kotlinext.js.js
import kotlinx.html.CANVAS
import kotlinx.html.classes
import kotlinx.html.id
import kotlinx.html.style
import org.w3c.dom.CanvasRenderingContext2D
import react.RBuilder
import react.RState
import react.dom.RDOMBuilder
import react.dom.canvas

// https://codepen.io/vasilly/pen/NRKyWL
interface MapCanvasProps : GameProps {
    var id: String
    var classes: Set<String>
    var pixelBlock: PixelBlock
}

/*
-----------------------------------------------------------------------------------------------
|    The whole game map                                                                       |
|                                                                                             |
|      ----------------------------------------------------------------------------------     |
|      |   The browser window                                                            |    |
|      |                                                                                 |    |
|      |                                                                                 |    |
|      |      ----------------------------------------------------------------------     |    |
|      |      |  The map canvas                                                    |     |    |
|      |      |                                                                    |     |    |
|      |      |                                                                    |     |    |
|      |      |                                                                    |     |    |
|      |      |                                                                    |     |    |
|      |      |                                                                    |     |    |
|      |      |                                                                    |     |    |
|      |      ----------------------------------------------------------------------     |    |
|      |                                                                                 |    |
|      -----------------------------------------------------------------------------------    |
|                                                                                             |
|---------------------------------------------------------------------------------------------|

Map canvas (aka. viewport) locates in the middle of the browser windows, and acts as the main UI of the game.
Basically, it draws part of the game map and responds to various game events:

1. "window.animate": this is the main loop of HTML canvas animation.

 */

abstract class AbstractMapCanvas<S : RState> : GameUIComponent<MapCanvasProps, S>() {
    lateinit var canvas: CanvasRenderingContext2D
//    private val windowAnimationEventListener: GameAnimationEventListener = { onPaint(it) }

//    protected abstract fun onPaint(lastAnimationTime: Timestamp)

    protected fun RBuilder.mapCanvas(canvasConfig: RDOMBuilder<CANVAS>.() -> Unit) {
        canvas {
            canvasConfig()

            +"Canvas not supported"
            ref {
                game.renderer.canvas = it
            }
        }
    }

    fun CanvasRenderingContext2D.drawImage(
        imageId: String,
        sx: Int,
        sy: Int,
        sw: Int,
        sh: Int,
        dx: Int,
        dy: Int,
        dw: Int,
        dh: Int,
        opacity: Double = 1.0
    ) {
        save()
        globalAlpha = opacity
        drawImage(
            getImageElement(imageId),
            sx.toDouble(), sy.toDouble(), sw.toDouble(), sh.toDouble(),
            dx.toDouble(), dy.toDouble(), dw.toDouble(), dh.toDouble()
        )
        restore()
    }
}

class MapCanvasLayer : AbstractMapCanvas<RState>() {
    override fun RBuilder.render() {
        mapCanvas {
            val mapCanvasZIndex = Layer.MapCanvas.zIndex()
            attrs {
                id = "map-canvas-layer"
                if (!mapCoveredByCanvas) {
                    classes = setOf("canvas-border")
                }
                width = canvasPixelSize.width.toString()
                height = canvasPixelSize.height.toString()
                style = js {
                    zIndex = mapCanvasZIndex
                    position = "absolute"
                    top = "${canvasCoordinateInGameContainer.y}px"
                    left = "${canvasCoordinateInGameContainer.x}px"
                }
            }
        }
    }

//    override fun onPaint() {
////        val start = Timestamp.now()
////        canvas.clearRect(0.0, 0.0, canvasPixelSize.width.toDouble(), canvasPixelSize.height.toDouble())
//
//        val sprites = game.activeScene.objects.getDrawableSprites()
////        canvas.commit()
////        console.log("paint takes ${Timestamp.now() -start}ms")
//    }
}
