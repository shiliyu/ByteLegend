import com.bytelegend.app.client.api.AbstractStaticLocationSprite
import com.bytelegend.app.client.api.GameObjectContainer
import com.bytelegend.app.client.api.GameRuntime
import com.bytelegend.app.client.api.GameScriptHelpers
import com.bytelegend.app.client.api.HERO_ID
import com.bytelegend.app.client.api.ScriptsBuilder
import com.bytelegend.app.shared.BEGINNER_GUIDE_FINISHED_STATE
import com.bytelegend.app.shared.COFFEE
import com.bytelegend.app.shared.HumanReadableCoordinate
import com.bytelegend.app.shared.JAVA_ISLAND
import com.bytelegend.app.shared.JAVA_ISLAND_NEWBIE_VILLAGE_PUB
import com.bytelegend.app.shared.NEWBIE_VILLAGE_OLD_MAN_GOT_COFFEE
import com.bytelegend.app.shared.START_BYTELEGEND_MISSION_ID
import com.bytelegend.app.shared.objects.GameMapPoint
import kotlinx.browser.window

val gameRuntime = window.asDynamic().gameRuntime.unsafeCast<GameRuntime>()

fun main() {
    gameRuntime.sceneContainer.getSceneById(JAVA_ISLAND).apply {
        val helpers = GameScriptHelpers(this)
        objects {
            mapEntrance {
                destMapId = JAVA_ISLAND_NEWBIE_VILLAGE_PUB
            }

            npc {
                val oldManId = "JavaIslandNewbieVillageOldMan"
                val oldManStartPoint = objects.getById<GameMapPoint>("$oldManId-point").point
                val oldManDestination = objects.getById<GameMapPoint>("$oldManId-destination").point
                id = oldManId
                sprite = "$oldManId-sprite"
                onInit = {
                    if (gameRuntime.heroPlayer.states.containsKey(NEWBIE_VILLAGE_OLD_MAN_GOT_COFFEE)) {
                        helpers.getCharacter(oldManId).gridCoordinate = oldManDestination
                    } else {
                        helpers.getCharacter(oldManId).gridCoordinate = oldManStartPoint
                    }
                }
                onClick = helpers.standardNpcSpeech(oldManId) {
                    if (gameRuntime.heroPlayer.states.containsKey(NEWBIE_VILLAGE_OLD_MAN_GOT_COFFEE)) {
                        scripts {
                            speech(oldManId, "NiceDayHub", arrow = false)
                        }
                    } else if (gameRuntime.heroPlayer.items.contains(COFFEE)) {
                        scripts {
                            speech(oldManId, "ThankYouForYourCoffee")
                            addState(NEWBIE_VILLAGE_OLD_MAN_GOT_COFFEE)
                            removeItem(COFFEE)
                            characterMove(oldManId, oldManDestination)
                        }
                    } else {
                        scripts {
                            speech(oldManId, "CanYouPleaseGrabACoffee", arrow = false)
                        }
                    }
                }
            }

            npc {
                val guardId = "JavaIslandNewbieVillagePubGuard"
                val guardStartPoint = objects.getById<GameMapPoint>("JavaNewbieVilllagePubEntranceGuard-point").point
                val guardMoveDestPoint = objects.getById<GameMapPoint>("JavaNewbieVilllagePubEntranceGuard-destination").point
                id = guardId
                sprite = "JavaIslandNewbieVillagePubGuard-sprite"
                onInit = {
                    when {
                        gameRuntime.heroPlayer.states.containsKey(BEGINNER_GUIDE_FINISHED_STATE) -> {
                            helpers.getCharacter(guardId).gridCoordinate = guardStartPoint
                            scripts {
                                speech(guardId, "DoYouPreferToBeMediocre")
                            }
                        }
                        playerMissions.missionAccomplished(START_BYTELEGEND_MISSION_ID) -> {
                            helpers.getCharacter(guardId).gridCoordinate = guardMoveDestPoint
                        }
                        else -> {
                            helpers.getCharacter(guardId).gridCoordinate = guardStartPoint
                        }
                    }
                }

                onClick = helpers.standardNpcSpeech(guardId) {
                    if (helpers.getCharacter(guardId).gridCoordinate == guardStartPoint) {
                        when {
                            gameRuntime.heroPlayer.states.containsKey(BEGINNER_GUIDE_FINISHED_STATE) && playerMissions.missionAccomplished(START_BYTELEGEND_MISSION_ID) -> {
                                // Player star first but hasn't finished beginner guide, show them
                                scripts {
                                    startBeginnerGuide()
                                    speech(guardId, "NiceJob", arrayOf("1", "0"))
                                    characterMove(guardId, guardMoveDestPoint)
                                }
                            }
                            gameRuntime.heroPlayer.states.containsKey(BEGINNER_GUIDE_FINISHED_STATE) -> {
                                scripts {
                                    talkAboutFirstStar(guardId, objects)
                                    startBeginnerGuide()
                                }
                            }
                            playerMissions.missionAccomplished(START_BYTELEGEND_MISSION_ID) -> {
                                // mission accomplished, let's celebrate!
                                scripts {
                                    speech(guardId, "NiceJob", arrayOf("1", "0"))
                                    characterMove(guardId, guardMoveDestPoint)
                                }
                            }
                            else -> {
                                scripts {
                                    speech(guardId, "StarCondition", arrayOf("1", "0"))
                                    speech(HERO_ID, "WhereToFindStar")
                                    speech(
                                        guardId, "IDontKnowTakeALookAtStarBytelegend",
                                        arrayOf(
                                            HumanReadableCoordinate(objects.getById<AbstractStaticLocationSprite>("star-bytelegend").gridCoordinate).toString()
                                        ),
                                        arrow = false
                                    )
                                }
                            }
                        }
                    } else {
                        scripts {
                            speech(guardId, "NiceDayHub", arrow = false)
                        }
                    }
                }
            }
        }
    }
}

fun ScriptsBuilder.talkAboutFirstStar(guardId: String, objects: GameObjectContainer) {
    speech(guardId, "StarCondition", arrayOf("1", "0"))
    speech(HERO_ID, "WhereToFindStar")
    speech(
        guardId, "IDontKnowTakeALookAtStarBytelegend",
        arrayOf(
            HumanReadableCoordinate(objects.getById<AbstractStaticLocationSprite>("star-bytelegend").gridCoordinate).toString()
        )
    )
}
