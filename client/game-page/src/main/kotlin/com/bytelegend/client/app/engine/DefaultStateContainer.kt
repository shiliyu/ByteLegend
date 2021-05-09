package com.bytelegend.client.app.engine

import com.bytelegend.app.client.api.GameScene
import com.bytelegend.app.client.api.StateContainer
// import com.bytelegend.app.shared.entities.States
import com.bytelegend.client.app.web.WebSocketClient

class DefaultStateContainer(
//    private val states: States
) : StateContainer {
    private lateinit var gameScene: GameScene
    private val webSocketClient: WebSocketClient by lazy {
        gameScene.gameRuntime.unsafeCast<Game>().webSocketClient
    }

    fun init(scene: GameScene) {
        gameScene = scene
    }

    override fun hasState(state: String): Boolean = TODO() // states.states.containsKey(state)

    override fun getState(name: String): String = TODO() // states.states.getValue(name)

    override suspend fun removeState(state: String) {
//        webSocketClient.removeState(gameScene.map.id, state)
//        states.states.remove(state)
    }

    override suspend fun putState(key: String, value: String) {
//        webSocketClient.putState(gameScene.map.id, key, value)
//        states.states.remove(state)
    }
}
