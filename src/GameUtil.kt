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
            if (first === second) return 180.0
            if (first === NORTH) {
                if (second === SOUTH) return 0.0
                return if (second === EAST) 90.0 else -90.0
            }
            if (first === EAST) {
                if (second === WEST) return 0.0
                return if (second === SOUTH) 90.0 else -90.0
            }
            if (first === SOUTH) {
                if (second === NORTH) return 0.0
                return if (second === WEST) 90.0 else -90.0
            }
            if (first === WEST) {
                if (second === EAST) return 0.0
                return if (second === NORTH) 90.0 else -90.0
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

data class Portal(var mapX: Int, var mapY: Int, var direction: Direction) {
    companion object {
        fun displaceVectorForRotation(v: Vector, cardinalRotation: Double) {
            if (cardinalRotation == 180.0) {
                v.x += 1
                v.y += 1
            } else if (cardinalRotation == 90.0) {
                v.x += 1
            } else if (cardinalRotation == -90.0) {
                v.y += 1
            }
        }
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
