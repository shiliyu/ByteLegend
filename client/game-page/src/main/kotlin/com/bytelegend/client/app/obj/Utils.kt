package com.bytelegend.client.app.obj

import com.bytelegend.app.client.api.GameScene
import com.bytelegend.app.shared.PixelBlock
import com.bytelegend.app.shared.PixelCoordinate
import kotlinx.browser.window
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLImageElement

fun isFirefox(): Boolean {
    return window.navigator.userAgent.lowercase().indexOf("firefox") > -1
}

fun uuid(): String {
    // https://stackoverflow.com/questions/105034/how-to-create-a-guid-uuid
    return js(
        """
        'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
            var r = Math.random() * 16 | 0, v = c == 'x' ? r : (r & 0x3 | 0x8);
            return v.toString(16);
        })
    """
    )
}

internal fun PixelCoordinate.outOfCanvas(gameScene: GameScene): Boolean {
    return (this - gameScene.canvasState.getCanvasCoordinateInMap()).let {
        it.x > gameScene.canvasState.getCanvasPixelSize().width ||
            it.y > gameScene.canvasState.getCanvasPixelSize().height ||
            it.x < 0 ||
            it.y < 0
    }
}

internal fun CanvasRenderingContext2D.disableShadow() = setShadow("white", 0, 0, 0)

internal fun CanvasRenderingContext2D.setShadow(
    color: String,
    offsetX: Number,
    offsetY: Number,
    blur: Number
) {
    shadowColor = color
    shadowOffsetX = offsetX.toDouble()
    shadowOffsetY = offsetY.toDouble()
    shadowBlur = blur.toDouble()
}

internal fun CanvasRenderingContext2D.quadraticCurveTo(x: Int, y: Int, xc: Int, yc: Int) {
    quadraticCurveTo(x.toDouble(), y.toDouble(), xc.toDouble(), yc.toDouble())
}

// https://stackoverflow.com/questions/7054272/how-to-draw-smooth-curve-through-n-points-using-javascript-html5-canvas
internal fun CanvasRenderingContext2D.drawCurve(curve: GameCurveSprite, gameScene: GameScene) {
    save()
    beginPath()

    // Firefox is INCREDIBLY SLOW when drawing shadows.
    if (!isFirefox()) {
        shadowColor = "rgba(0,0,0,0.8)"
        shadowOffsetX = 10.0
        shadowOffsetY = 10.0
        shadowBlur = 4.0
    }
    strokeStyle = "rgba(0,0,0,0.5)"
    lineWidth = 5.0
    setLineDash(arrayOf(32.0, 12.0))

    val originX = gameScene.canvasState.getCanvasCoordinateInMap().x
    val originY = gameScene.canvasState.getCanvasCoordinateInMap().y
    moveTo((curve.points[0].x - originX).toDouble(), (curve.points[0].y - originY).toDouble())

    for (i in 1 until curve.points.size - 2) {
        val xc = (curve.points[i].x + curve.points[i + 1].x) / 2
        val yc = (curve.points[i].y + curve.points[i + 1].y) / 2
        quadraticCurveTo(curve.points[i].x - originX, curve.points[i].y - originY, xc - originX, yc - originY)
    }
    quadraticCurveTo(
        curve.points[curve.points.size - 2].x - originX, curve.points[curve.points.size - 2].y - originY,
        curve.points[curve.points.size - 1].x - originX, curve.points[curve.points.size - 1].y - originY
    )
    stroke()
    restore()
}

internal fun CanvasRenderingContext2D.drawImage(tileset: HTMLImageElement, srcBlock: PixelBlock, destBlock: PixelBlock) =
    drawImage(
        tileset,
        srcBlock.x.toDouble(), srcBlock.y.toDouble(), srcBlock.width.toDouble(), srcBlock.height.toDouble(),
        destBlock.x.toDouble(), destBlock.y.toDouble(), destBlock.width.toDouble(), destBlock.height.toDouble(),
    )
