
import java.awt.color.ColorSpace
import kotlin.reflect.jvm.internal.impl.descriptors.Named


enum class Direction {
    NORTH,
    SOUTH,
    WEST,
    EAST;

    companion object {
        private val directionMap = hashMapOf(
                NORTH to 1,
                EAST to 2,
                SOUTH to 3,
                WEST to 4
        )

        fun degreeRelationship(first: Direction, second: Direction): Double {
            if (first == second) return 180.0
            val firstVal = directionMap[first] ?: return 0.0
            val secondVal = directionMap[second] ?: return 0.0
            val diff = firstVal - secondVal
            if (diff == -1 || diff == -3) {
                return 90.0
            } else if (diff == 1 || diff == 3) {
                return -90.0
            }
            return 0.0
        }
    }

}

data class Ray(var mapX: Int,
          var mapY: Int,
          var stepX: Int,
          var stepY: Int,
          var distance: Double,
          var side: Int,
          var wallX: Double,
          var origin: Vector,
          var direction: Vector,
          var wallHitDirection: Direction
)

data class Vector(var x: Double, var y: Double) {

    constructor(x: Int, y: Int): this(x.toDouble(), y.toDouble())

    fun rotate(degrees: Double): Vector {
        val rotRad = Math.toRadians(degrees)
        val oldX = x
        x = x * Math.cos(rotRad) - y * Math.sin(rotRad)
        y = oldX * Math.sin(rotRad) + y * Math.cos(rotRad)
        return this
    }

    fun add(v: Vector): Vector {
        x += v.x
        y += v.y
        return this
    }

    fun sub(v: Vector): Vector {
        x -= v.x
        y -= v.y
        return this
    }

    fun mirrorX(): Vector {
        x *= -1
        return this
    }

    fun mirrorY(): Vector {
        y *= -1
        return this
    }
}

object Color {
    fun color(Red: Int, Green: Int, Blue: Int): Int {
        var Red = Red
        var Green = Green
        var Blue = Blue

        Red = Red shl 16 and 0x00FF0000 //Shift red 16-bits and mask out other stuff
        Green = Green shl 8 and 0x0000FF00 //Shift Green 8-bits and mask out other stuff
        Blue = Blue and 0x000000FF //Mask out anything not blue.
        return -0x1000000 or Red or Green or Blue //0xFF000000 for 100% Alpha. Bitwise OR everything together.
    }

    fun darken(color: Int, amount: Int): Int {
        var r = (color shr 16 and 0xff) - amount
        var g = (color shr 8 and 0xff) - amount
        var b = (color and 0xff) - amount
        r = if (r < 0) 0 else r
        g = if (g < 0) 0 else g
        b = if (b < 0) 0 else b
        return color(r, g, b)
    }

}
