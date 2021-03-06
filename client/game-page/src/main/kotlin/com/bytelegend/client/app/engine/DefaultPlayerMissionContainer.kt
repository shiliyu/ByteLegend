package com.bytelegend.client.app.engine

import com.bytelegend.app.client.api.Banner
import com.bytelegend.app.client.api.EventBus
import com.bytelegend.app.client.api.EventListener
import com.bytelegend.app.client.api.GameCanvasState
import com.bytelegend.app.client.api.GameRuntime
import com.bytelegend.app.client.api.GameScene
import com.bytelegend.app.client.api.ModalController
import com.bytelegend.app.client.api.PlayerMissionContainer
import com.bytelegend.app.client.misc.playAudio
import com.bytelegend.app.shared.GridCoordinate
import com.bytelegend.app.shared.PixelCoordinate
import com.bytelegend.app.shared.PixelSize
import com.bytelegend.app.shared.entities.MissionAnswer
import com.bytelegend.app.shared.entities.PlayerMission
import com.bytelegend.app.shared.objects.GameMapMission
import com.bytelegend.app.shared.protocol.ItemsStatesUpdateEventData
import com.bytelegend.app.shared.protocol.MISSION_UPDATE_EVENT
import com.bytelegend.app.shared.protocol.MissionUpdateEventData
import com.bytelegend.app.shared.protocol.STAR_UPDATE_EVENT
import com.bytelegend.app.shared.protocol.StarUpdateEventData
import com.bytelegend.client.app.script.ASYNC_ANIMATION_CHANNEL
import com.bytelegend.client.app.script.DefaultGameDirector
import com.bytelegend.client.app.script.STAR_BYTELEGEND_MISSION_ID
import com.bytelegend.client.app.script.effect.itemPopupEffect
import com.bytelegend.client.app.script.effect.starFlyEffect
import com.bytelegend.client.app.ui.NumberIncrementEvent
import com.bytelegend.client.app.ui.STAR_INCREMENT_EVENT
import com.bytelegend.client.app.ui.determineRightSideBarTopLeftCornerCoordinateInGameContainer
import com.bytelegend.client.app.ui.menu.determineMenuCoordinateInGameContainer
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.instance

class DefaultPlayerMissionContainer(
    di: DI,
    private val missions: MutableMap<String, PlayerMission>
) : PlayerMissionContainer {
    private val eventBus: EventBus by di.instance()
    private val game: GameRuntime by di.instance()
    private val gameControl: GameControl by di.instance()
    private val modalController: ModalController by lazy {
        game.modalController
    }
    var gameScene: DefaultGameScene? = null

    private val starUpdateEventListener: EventListener<StarUpdateEventData> = this::onStarUpdate
    private val missionUpdateEventListener: EventListener<MissionUpdateEventData> = this::onMissionUpdate

    override fun missionAccomplished(missionId: String): Boolean {
        return missions[missionId]?.accomplished == true
    }

    override fun missionStar(missionId: String): Int {
        return missions[missionId]?.star ?: 0
    }

    override fun missionAnswers(missionId: String): List<MissionAnswer> {
        return missions[missionId]?.answers ?: emptyList()
    }

    private fun isCanvasInvisible(): Boolean {
        return modalController.visible || !gameControl.isWindowVisible
    }

    fun onItemsUpdate(eventData: ItemsStatesUpdateEventData) {
        val mission = gameScene!!.objects.getByIdOrNull<GameMission>(eventData.missionId)?.gameMapMission ?: return

        if (isCanvasInvisible()) {
            gameScene?.scripts(ASYNC_ANIMATION_CHANNEL, false) {
                eventData.onFinishSpec.items.add.forEach { item ->
                    this.unsafeCast<DefaultGameDirector>().suspendAnimation {
                        itemPopup(item, mission)
                    }
                }
            }
        } else {

            GlobalScope.launch {
                eventData.onFinishSpec.items.add.forEach { item ->
                    itemPopup(item, mission)
                }
            }
        }
    }

    private fun onStarUpdate(eventData: StarUpdateEventData) {
        val currentMap: String = gameScene?.map?.id ?: return
        if (currentMap == eventData.map) {
            // star/mission change happens on current map
            // respond to the event
            val mission = gameScene!!.objects.getById<GameMission>(eventData.missionId).gameMapMission
            val canvasState = gameScene!!.canvasState
            val endCoordinateInGameContainer: PixelCoordinate =
                canvasState.determineRightSideBarTopLeftCornerCoordinateInGameContainer()
            val startCoordinateInGameContainer: PixelCoordinate =
                if (mission.id == STAR_BYTELEGEND_MISSION_ID)
                // See MenuItem, from the GitHub menu icon
                    canvasState.determineMenuCoordinateInGameContainer()
                else
                    canvasState.calculateCoordinateInGameContainer(mission.point)

            if (isCanvasInvisible()) {
                gameScene?.scripts(ASYNC_ANIMATION_CHANNEL, false) {
                    this.unsafeCast<DefaultGameDirector>().suspendAnimation {
                        starFlyThenIncrement(
                            canvasState.gameContainerSize,
                            startCoordinateInGameContainer,
                            endCoordinateInGameContainer,
                            eventData
                        )
                    }
                }
            } else {
                // fly then add the star
                GlobalScope.launch {
                    starFlyThenIncrement(
                        canvasState.gameContainerSize,
                        startCoordinateInGameContainer,
                        endCoordinateInGameContainer,
                        eventData
                    )
                }
            }
        } else if (gameScene!!.gameRuntime.sceneContainer.getSceneByIdOrNull(eventData.map) == null &&
            gameScene!!.isActive
        ) {
            // the corresponding scene is not loaded, let activeScene respond
            // only add star
            if (isCanvasInvisible()) {
                // if modal is visible, add to script list
                gameScene?.scripts(ASYNC_ANIMATION_CHANNEL, false) {
                    this.unsafeCast<DefaultGameDirector>().suspendAnimation {
                        starIncrement(eventData)
                    }
                }
            } else {
                // fly then add the star
                GlobalScope.launch {
                    starIncrement(eventData)
                }
            }
        }
    }

    private suspend fun itemPopup(item: String, mission: GameMapMission) {
        game.bannerController.showBanner(
            Banner(
                game.i("GetItem", "Coffee"),
                "success",
                true,
                true
            )
        )
        playAudio("popup")
        val canvasState = gameScene!!.canvasState
        itemPopupEffect(
            item,
            canvasState.gameContainerSize,
            canvasState.calculateCoordinateInGameContainer(mission.point),
            canvasState.determineRightSideBarTopLeftCornerCoordinateInGameContainer() + PixelCoordinate(
                0,
                200
            ), /* items box offset */
            3.0
        )
    }

    private suspend fun starFlyThenIncrement(
        gameContainerSize: PixelSize,
        startCoordinateInGameContainer: PixelCoordinate,
        endCoordinateInGameContainer: PixelCoordinate,
        eventData: StarUpdateEventData
    ) {
        playAudio("starfly")
        starFlyEffect(
            gameContainerSize,
            startCoordinateInGameContainer,
            endCoordinateInGameContainer,
            3
        )
        game.eventBus.emit(STAR_INCREMENT_EVENT, NumberIncrementEvent(eventData.change, eventData.newValue))
    }

    private fun starIncrement(eventData: StarUpdateEventData) {
        playAudio("starfly")
        game.eventBus.emit(STAR_INCREMENT_EVENT, eventData)
    }

    private fun onMissionUpdate(eventData: MissionUpdateEventData) {
        val currentMap: String = gameScene?.map?.id ?: return
        if (currentMap == eventData.map) {
            missions[eventData.newValue.id!!] = eventData.newValue
        }
    }

    fun init(gameScene: GameScene) {
        this.gameScene = gameScene.unsafeCast<DefaultGameScene>()
        eventBus.on(STAR_UPDATE_EVENT, starUpdateEventListener)
        eventBus.on(MISSION_UPDATE_EVENT, missionUpdateEventListener)
    }

    fun close() {
        eventBus.remove(MISSION_UPDATE_EVENT, missionUpdateEventListener)
        eventBus.remove(STAR_UPDATE_EVENT, starUpdateEventListener)
    }
}

fun GameCanvasState.calculateCoordinateInGameContainer(mapCoordinate: GridCoordinate): PixelCoordinate {
    return mapCoordinate * tileSize - getCanvasCoordinateInMap() + getCanvasCoordinateInGameContainer()
}
