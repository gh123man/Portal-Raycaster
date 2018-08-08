import java.awt.Color
import java.awt.Graphics
import java.awt.image.BufferStrategy
import java.io.IOException
import javax.swing.JFrame
import java.awt.image.BufferedImage
import java.awt.GraphicsEnvironment
import java.util.HashMap
import java.awt.GraphicsDevice
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.awt.image.DataBufferInt
import kotlin.collections.Map.Entry

fun main(args: Array<String>) {
    RaycasterDisplay().run()
}


class RaycasterDisplay @Throws(IOException::class)
constructor() : JFrame(), Runnable, KeyListener {


    private val screen: GraphicsDevice

    private var running: Boolean = false
    private val thread: Thread
    private val frame: BufferedImage

    private val windowBarHeight: Int
    private var fps: Long = 0
    private var fpsLimit: Long = 5
    private var frameTimeLimit = 1
    var buffer: IntArray
    var game: Game

    val targetWidth = 800
    val targetHeight = 600

    @Volatile var move = 0.0
    @Volatile var turn = 0.0

    init {

        addKeyListener(this)
        val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
        screen = ge.defaultScreenDevice

        frame = BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB)

        buffer = (frame.raster.dataBuffer as DataBufferInt).data
        game = Game(buffer, targetWidth, targetHeight)


        setSize(targetWidth, targetHeight)
        isVisible = true
        isResizable = false
        title = "game"
        JFrame.setDefaultLookAndFeelDecorated(true)
        background = Color.BLACK
        setLocationRelativeTo(null)
        defaultCloseOperation = JFrame.EXIT_ON_CLOSE

        windowBarHeight = (targetHeight - contentPane.size.getHeight()).toInt()
        setSize(targetWidth, targetHeight + windowBarHeight)

        thread = Thread(this)
        thread.isDaemon = true
        thread.start()
    }

    override fun run() {
        running = true
        runDemo()
    }

    fun stop() {
        running = false
        screen.fullScreenWindow = null
    }

    fun runDemo() {
        var start: Long
        var diff: Long
        var sleepTime: Long
        var framePerTick = 1000 / 60
        var tick = 0

        while (running) {
            start = System.nanoTime()
           // frame.graphics.clearRect(0, 0, width, height)
            tick ++
            game.player.move(move)
            game.player.rotate(turn)
            game.tick(tick)

            drawFrame()

            diff = (System.nanoTime() - start) / 1_000_000
            sleepTime = framePerTick - diff
            if (sleepTime < 0) {
                sleepTime = 0
            }

            //println("sleep: $sleepTime diff: $diff fpt: $framePerTick")

            fps = 1000 / (diff + sleepTime)
            try {
                Thread.sleep(sleepTime)
            } catch (e: InterruptedException) {
            }
        }

        System.exit(0)
    }

    private fun drawFrame() {
        var bs: BufferStrategy? = bufferStrategy
        if (bs == null) {
            createBufferStrategy(3)
            bs = bufferStrategy
        }

        val g = bs!!.drawGraphics
        g.drawImage(frame, 0, 0, width, height, null)
        drawDebugInfo(g)
        bs.show()
    }

    private fun drawDebugInfo(g: Graphics) {
        val debugInfo = "$fps fps"
        g.color = Color.YELLOW
        g.drawString(debugInfo, 20  , windowBarHeight + g.fontMetrics.height)
    }

    fun toggleFullscreen() {
        /*if (Settings.fullScreen === false) {
            screen.fullScreenWindow = null
            setSize(WIDTH, HEIGHT)
        } else {
            screen.fullScreenWindow = this
        }
        */
    }

    override fun keyTyped(e: KeyEvent?) {
    }

    override fun keyPressed(e: KeyEvent) {
        if (e.keyCode == KeyEvent.VK_W) {
            move = 0.05
        }

        if (e.keyCode == KeyEvent.VK_S) {
            move = -0.05
        }

        if (e.keyCode == KeyEvent.VK_A) {
            turn = 2.0
        }

        if (e.keyCode == KeyEvent.VK_D) {
            turn = -2.0
        }
    }

    override fun keyReleased(e: KeyEvent?) {
        if (e == null) return

        if (e.keyCode == KeyEvent.VK_W) {
            move = 0.0
        }

        if (e.keyCode == KeyEvent.VK_S) {
            move = 0.0
        }

        if (e.keyCode == KeyEvent.VK_A) {
            turn = 0.0
        }

        if (e.keyCode == KeyEvent.VK_D) {
            turn = 0.0
        }
    }


}