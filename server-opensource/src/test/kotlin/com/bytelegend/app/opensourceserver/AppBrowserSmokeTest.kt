package com.bytelegend.app.opensourceserver

import com.bytelegend.app.servershared.mock.mockPlayer
import com.bytelegend.app.testfixtures.AbstractBrowserTest
import com.bytelegend.app.testfixtures.fadeInLayerDisappeared
import com.bytelegend.app.testfixtures.startGame
import com.bytelegend.app.testfixtures.waitUntil
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.extension.ExtendWith
import org.openqa.selenium.By
import org.openqa.selenium.chrome.ChromeOptions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.env.Environment
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@SpringBootTest(classes = [GameApp::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AppBrowserSmokeTest : AbstractBrowserTest() {
    @Autowired
    lateinit var environment: Environment
    override val gameServerPort: Int by lazy {
        environment.getProperty("local.server.port")!!.toInt()
    }

    @BeforeEach
    fun appBrowserSmokeTestBeforeEach() {
        browserWebDriverContainers.add(createBrowser({ ChromeOptions() }, buildTmpDir))
    }

    @Timeout(120)
    @Test
    fun `anonymous access and login`() {
        browserWebDriverContainers[0].start()
        webDriver.startGame(gameServerPort)

        webDriver.findElement(By.id("login-link")).click()

        webDriver.waitUntil(10000) {
            findElements(By.className("avatar-img")).run {
                isNotEmpty() && get(0).getAttribute("src") == mockPlayer.avatarUrl && fadeInLayerDisappeared()
            }
        }

        webDriver.findElement(By.className("avatar-img")).click()
        for (it in webDriver.findElements(By.className("dropdown-item"))) {
            if (it.text == "Sign out") {
                it.click()
                break
            }
        }
        webDriver.waitUntil(10000) {
            webDriver.findElements(By.id("login-link")).isNotEmpty()
        }
    }
}
