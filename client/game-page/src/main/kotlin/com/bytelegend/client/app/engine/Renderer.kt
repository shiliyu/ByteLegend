@file:Suppress("UnsafeCastFromDynamic")

package com.bytelegend.client.app.engine

import com.bytelegend.app.client.api.GameScene
import com.bytelegend.app.client.api.ImageResourceData
import com.bytelegend.app.client.api.Sprite
import com.bytelegend.client.app.ui.UPDATE_FPS_EVENT
import kotlinx.browser.window
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement

interface Renderer {
    var canvas: dynamic

    fun transferImageBitmap(image: ImageResourceData)

    fun start(initScene: GameScene)
}

class DirectRenderer : Renderer {
    private lateinit var canvasContext: CanvasRenderingContext2D
    override var canvas: dynamic = null
        set(value) {
            field = value
            canvasContext = value.unsafeCast<HTMLCanvasElement>().getContext("2d").asDynamic()
        }

    override fun transferImageBitmap(image: ImageResourceData) {
        TODO("Not yet implemented")
    }


    override fun start(initScene: GameScene) {
        TODO("Not yet implemented")
    }
}

class OfflineCanvasRenderer(
    private val game: Game,
    private val renderWorker: dynamic
) : Renderer {
    init {
        renderWorker.onmessage = this::onRenderWorkerMessage
    }

    override var canvas: dynamic = null
        set(value) {
            if (value != null) {
                field = value
                try {
                    val offscreen = value.transferControlToOffscreen()
                    renderWorker.postMessage(js("{name:'UpdateCanvas', payload:offscreen}"), js("[offscreen]"))
                } catch (e: Throwable) {
                    if (e.message?.contains("Cannot transfer control from a canvas for more than one time") == false) {
                        throw e
                    }
                }
            }
        }

    override fun transferImageBitmap(image: ImageResourceData) {
        window.asDynamic().createImageBitmap(image.htmlElement).then { bitmap ->
            console.log("bitmap: $bitmap")
            val id = image.imageId
            renderWorker.postMessage(js("{name:'AddImageBitmap', payload: {id: id, bitmap: bitmap}}"), js("[bitmap]"))
        }
    }

    override fun start(initScene: GameScene) {
        renderSprites(initScene)
    }

    /**
     * In worker render loop, it sends a message to main thread,
     * the main thread collects all sprites and sends the data to worker,
     * worker receives the data and renders.
     */
    fun onRenderWorkerMessage(message: dynamic) {
        if (message.data.name == "RequestSprites") {
            renderSprites(game.activeScene)
        } else if (message.data.name == "UpdateFpsCounter") {
            game.eventBus.emit(UPDATE_FPS_EVENT, message.data.payload)
        }
    }

    private fun renderSprites(scene: GameScene) {
        val sprites = scene.objects.getDrawableSprites().toJson()
        renderWorker.postMessage(js("{name:'RenderSprites', payload:sprites}"))
    }

    fun List<Sprite>.toJson(): String {
        return JSON.stringify(map { it.toJsObject() }.toTypedArray())
    }
}