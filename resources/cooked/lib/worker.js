'use strict';

let canvas = null
let lastFpsUpdateTime = null
let framesSinceLastUpdate = 0
let images = {}

self.setInterval(function () {
    const now = new Date().getTime();
    if (lastFpsUpdateTime !== null && now - lastFpsUpdateTime > 100) {
        var fps = Math.floor(1000 * framesSinceLastUpdate / (now - lastFpsUpdateTime))
        postMessage({name: "UpdateFpsCounter", payload: fps})
        lastFpsUpdateTime = now
        framesSinceLastUpdate = 0
    }
}, 500)

self.onmessage = function (message) {
    if (message.data.name === 'UpdateCanvas') {
        canvas = message.data.payload.getContext("2d")
    } else if (message.data.name === "RenderSprites") {
        render(JSON.parse(message.data.payload))
    } else if (message.data.name === "AddImageBitmap") {
        console.log(`message: ${message}`)
        console.log(`payload: ${message.data.payload.bitmap}`)
        images[message.data.payload.id] = message.data.payload.bitmap
    }
}

function requestSpritesThenRender() {
    postMessage({name: "RequestSprites"})
}

function render(sprites) {
    if (lastFpsUpdateTime === null) {
        lastFpsUpdateTime = new Date().getTime()
    }
    framesSinceLastUpdate++

    // console.log(`canvas: ${canvas}, image:${JSON.stringify(images)}`)
    sprites.forEach((it) => {
        if (canvas !== null && images[it.imageId] !== undefined) {
            canvas.drawImage(
                images[it.imageId],
                it.sx, it.sy, it.sw, it.sh,
                it.dx, it.dy, it.dw, it.dh,
            )
        }
    })

    requestAnimationFrame(requestSpritesThenRender)
}