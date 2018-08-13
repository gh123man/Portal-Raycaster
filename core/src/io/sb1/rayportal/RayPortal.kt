package io.sb1.rayportal

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import io.sb1.rayportal.engine.Game
import io.sb1.rayportal.engine.RenderUtil
import com.badlogic.gdx.graphics.g2d.BitmapFont



class RayPortal : ApplicationAdapter() {
    internal lateinit var batch: SpriteBatch
    internal lateinit var img: Texture
    private lateinit var game: Game
    private lateinit var buffer: Pixmap
    private lateinit var font: BitmapFont
    lateinit var texture: Texture
    var frame = 0
    var resCut = 1

    override fun create() {

        batch = SpriteBatch()
        img = Texture("badlogic.jpg")

        buffer = Pixmap(Gdx.graphics.width / resCut, Gdx.graphics.height / resCut, Pixmap.Format.RGB888)
        buffer.blending = Pixmap.Blending.None
        buffer.filter = null
        font = BitmapFont()
        game = Game(RenderUtil(buffer, Gdx.graphics.width / resCut, Gdx.graphics.height / resCut))
        texture = Texture(buffer)
    }

    override fun render() {
       // game.tick(0)
        game.player.rotate(20.0 * Gdx.graphics.deltaTime)
        //Gdx.gl.glClearColor(1f, 0f, 0f, 1f)
        //Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        batch.begin()

        game.tick(frame ++)
        texture.draw(buffer, 0, 0)
        batch.draw(texture, 0f, 0f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())

        val fps = Gdx.graphics.framesPerSecond
        font.draw(batch, "FPS: $fps", 3f, Gdx.graphics.height.toFloat() - 3.0f)
        batch.end()
    }

    override fun dispose() {
        batch.dispose()
        img.dispose()
    }
}
