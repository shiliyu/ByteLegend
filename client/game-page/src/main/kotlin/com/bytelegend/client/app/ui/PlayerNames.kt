package com.bytelegend.client.app.ui

import com.bytelegend.app.shared.entities.Player
import com.bytelegend.client.app.engine.DefaultGameScene
import com.bytelegend.client.app.engine.GAME_CLOCK_50HZ_EVENT
import com.bytelegend.client.app.obj.CharacterSprite
import react.RBuilder
import react.RComponent
import react.RProps
import react.RState

interface PlayerNamesProps : GameProps
interface PlayerNamesState : RState

class PlayerNames : GameUIComponent<PlayerNamesProps, PlayerNamesState>() {
    override fun RBuilder.render() {
        if (!game.heroPlayer.isAnonymous && !game._hero!!.outOfCanvas()) {
            renderOne(game.heroPlayer, game._hero!!)
        }

        game.activeScene.unsafeCast<DefaultGameScene>()
            .players
            .getDrawableCharacters()
            .forEach {
                renderOne(it.player, it)
            }
    }

    private fun RBuilder.renderOne(player: Player, sprite: CharacterSprite) {
        val imageBlockOnCanvas = sprite.getImageBlockOnCanvas()
        val x = imageBlockOnCanvas.x + canvasCoordinateInGameContainer.x + activeScene.map.tileSize.width / 2
        val y = imageBlockOnCanvas.y + canvasCoordinateInGameContainer.y - 10
        val name = player.nickname!!
        child(PlayerNameSpan::class) {
            attrs.x = x
            attrs.y = y
            attrs.name = name
        }
    }

    override fun componentDidMount() {
        super.componentDidMount()
        props.game.eventBus.on(GAME_CLOCK_50HZ_EVENT, gameUiUpdateEventListener)
    }

    override fun componentWillUnmount() {
        super.componentWillUnmount()
        props.game.eventBus.remove(GAME_CLOCK_50HZ_EVENT, gameUiUpdateEventListener)
    }
}

interface PlayerNameSpanProps : RProps {
    // coordinate in game container
    var x: Int
    var y: Int
    var name: String
}

class PlayerNameSpan : RComponent<PlayerNameSpanProps, RState>() {
    override fun RBuilder.render() {
        absoluteDiv(
            left = props.x,
            top = props.y,
            zIndex = Layer.PlayerNames.zIndex(),
            classes = setOf("white-text-black-shadow", "nickname-span")
        ) {
            +props.name
        }
    }

    override fun shouldComponentUpdate(nextProps: PlayerNameSpanProps, nextState: RState): Boolean {
        return props.x != nextProps.x || props.y != nextProps.y || props.name != nextProps.name
    }
}
