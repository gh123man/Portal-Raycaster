
enum class Direction {
    NORTH, SOUTH, WEST, EAST
}


class Ray(var mapX: Int,
          var mapY: Int,
          var x: Double,
          var y: Double,
          var distance: Double,
          var side: Int,
          var direction: Direction
)

class Vector(var x: Double, var y: Double)
class Player(var position: Vector,
             var direction: Vector,
             var camPlane: Vector,
             val map: Array<IntArray>) {

    val hitBox = 10

    val mapPosX: Int
        get() = position.x.toInt()

    val mapPosY: Int
        get() = position.y.toInt()

    fun rotate(speed: Double) {
        val oldDirX = direction.x
        direction.x = direction.x * Math.cos(speed) - direction.y * Math.sin(speed)
        direction.y = oldDirX * Math.sin(speed) + direction.y * Math.cos(speed)

        val oldPlaneX = camPlane.x
        camPlane.x = camPlane.x * Math.cos(speed) - camPlane.y * Math.sin(speed)
        camPlane.y = oldPlaneX * Math.sin(speed) + camPlane.y * Math.cos(speed)
    }

    fun move(speed: Double) {
        // 12 is the hitbox size
        if(map[(position.x + direction.x * speed * hitBox).toInt()][mapPosY] == 0) position.x += direction.x * speed
        if(map[mapPosX][(position.y + direction.y * speed * hitBox).toInt()] == 0) position.y += direction.y * speed
    }
}


class Game(val buffer: IntArray,
           val width: Int,
           val height: Int) {

    val map = arrayOf(
            intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
            intArrayOf(1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1),
            intArrayOf(1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1),
            intArrayOf(1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1),
            intArrayOf(1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1),
            intArrayOf(1, 0, 0, 0, 0, 0, 0, 1, 1, 1, 2, 1, 0, 1, 1),
            intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1),
            intArrayOf(1, 0, 0, 5, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1),
            intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1),
            intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1),
            intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1),
            intArrayOf(1, 0, 0, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1),
            intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1),
            intArrayOf(1, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1),
            intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1)
    )

    val player = Player(
            Vector(2.0, 2.0),
            Vector(-1.0, 0.0),
            Vector(0.0, 0.66),
            map
    )


    fun tick(frame: Int) {

/*
        for (i in 1..width - 1) {
            paintLine(frame / 10, height % i, height -1, frame * i)
        }
        */

        //player.rotate(0.001)

        for (x in 0..width) {
            val ray = castRay(x)

            // cal wall height
            val wallHeight = (height / ray.distance).toInt()
            var drawStart = -(wallHeight / 2) + (height / 2)
            if (drawStart < 0) drawStart = 0

            var drawEnd = (wallHeight / 2) + (height / 2)
            if (drawEnd >= height) drawEnd = height - 1

            var shade = 0
            var intensity = if (ray.side == 0) 255 else 150

            intensity -= (ray.distance * 8).toInt()

/*
            if (map[ray.mapX][ray.mapY] == 1) {
                shade = color(0, 0, intensity)
            }
            if (map[ray.mapX][ray.mapY] == 2) {
                shade = color(0, intensity, 0)
            }
            if (map[ray.mapX][ray.mapY] == 3) {
                shade = color(intensity, 0, 0)
            }
            if (map[ray.mapX][ray.mapY] == 4) {
                shade = color(0, intensity, intensity)
            }
            if (map[ray.mapX][ray.mapY] == 5) {
                shade = color(intensity, 0, intensity)
            }
            */

            if (ray.direction == Direction.NORTH) {
                shade = color(0, 0, intensity)
            } else if (ray.direction == Direction.SOUTH) {
                shade = color(0, intensity, 0)
            } else if (ray.direction == Direction.EAST) {
                shade = color(intensity, 0, 0)
            } else {
                shade = color(0, intensity, intensity)
            }


            paintLine(x, drawStart, drawEnd, shade)

        }
    }

    fun paintLine(x: Int, start: Int, end: Int, color: Int) {
        for (y in start..end - 1) {
            buffer[y * width + x] = color
        }
    }

    fun color(Red: Int, Green: Int, Blue: Int): Int {
        var Red = Red
        var Green = Green
        var Blue = Blue
        Red = Red shl 16 and 0x00FF0000 //Shift red 16-bits and mask out other stuff
        Green = Green shl 8 and 0x0000FF00 //Shift Green 8-bits and mask out other stuff
        Blue = Blue and 0x000000FF //Mask out anything not blue.
        return -0x1000000 or Red or Green or Blue //0xFF000000 for 100% Alpha. Bitwise OR everything together.
    }

    fun castRay(x: Int): Ray {

        var mapX = player.mapPosX
        var mapY = player.mapPosY

        val camX: Double = 2 * x / width.toDouble() - 1 // x-coordinate in camera space
        val rayDirX = player.direction.x + player.camPlane.x * camX
        val rayDirY = player.direction.y + player.camPlane.y * camX
        var perpWallDist: Double

        var sideDistX: Double
        var sideDistY: Double

        var deltaDistX = Math.abs(1 / rayDirX)
        var deltaDistY = Math.abs(1 / rayDirY)

        var stepX = 0 // Direction to fire ray
        var stepY = 0

        var hit = 0
        var side = 0

        if (rayDirX < 0) {
            stepX = -1
            sideDistX = (player.position.x - mapX) * deltaDistX
        } else {
            stepX = 1
            sideDistX = (mapX + 1 - player.position.x) * deltaDistX
        }

        if (rayDirY < 0) {
            stepY = -1
            sideDistY = (player.position.y - mapY) * deltaDistY
        } else {
            stepY = 1
            sideDistY = (mapY + 1 - player.position.y) * deltaDistY
        }

        // DDA
        while (hit == 0) {
            if (sideDistX < sideDistY) {
                sideDistX += deltaDistX
                mapX += stepX
                side = 0
            } else {
                sideDistY += deltaDistY
                mapY += stepY
                side = 1
            }

            if (map[mapX][mapY] > 0) {
                hit = 1
            }
        }

        // Get ray len
        if (side == 0) {
            perpWallDist = (mapX - player.position.x + (1 - stepX) / 2) / rayDirX
        } else {
            perpWallDist = (mapY - player.position.y + (1 - stepY) / 2) / rayDirY
        }

        var direction: Direction
        if (side == 0 && rayDirX >= 0) {
            direction = Direction.NORTH
        } else if (side == 0 && rayDirX < 0) {
            direction = Direction.SOUTH
        } else if (side == 1 && rayDirY >= 0) {
            direction = Direction.EAST
        } else {
            direction = Direction.WEST
        }

        return Ray(mapX, mapY, sideDistX, sideDistY, perpWallDist, side, direction)
    }

}