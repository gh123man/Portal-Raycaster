
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

class Ray(var mapX: Int,
          var mapY: Int,
          var stepX: Int,
          var stepY: Int,
          var distance: Double,
          var side: Int,
          var origin: Vector,
          var wallHitDirection: Direction
)

class Vector(var x: Double, var y: Double) {

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

    fun copy(): Vector {
        return Vector(x, y)
    }
}
