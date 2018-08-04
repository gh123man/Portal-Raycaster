import java.util.*


class Portal(var mapX: Int, var mapY: Int, var direction: Direction) {
    override fun hashCode(): Int {
        return Objects.hash(mapX, mapY, direction)
    }

    override fun equals(other: Any?): Boolean {
        return other?.hashCode() == hashCode()
    }
}

class Player(var position: Vector,
             var direction: Vector,
             var camPlane: Vector,
             val map: Array<IntArray>,
             val portalMap: HashMap<Portal, Portal>) {

    val hitBox = 0

    val mapPosX: Int
        get() = position.x.toInt()

    val mapPosY: Int
        get() = position.y.toInt()

    fun rotate(degrees: Double) {
        direction.rotate(degrees)
        camPlane.rotate(degrees)
    }

    fun move(speed: Double) {
        // 12 is the hitbox size
        val newX = position.x + direction.x * speed
        val newY = position.y + direction.y * speed

        val xDir = newX.toInt() - mapPosX
        val yDir = newY.toInt() - mapPosY

        var cardinalDirection: Direction? = null
        when {
            xDir < 0 -> cardinalDirection = Direction.SOUTH
            xDir > 0 -> cardinalDirection = Direction.NORTH
            yDir < 0 -> cardinalDirection = Direction.EAST
            yDir > 0 -> cardinalDirection = Direction.WEST
        }

        val enterX = newX.toInt()
        val enterY = newY.toInt()

        if (cardinalDirection != null) {
            val portal = portalMap[Portal(enterX, enterY, cardinalDirection)]
            if (portal != null) {
                var xOffset = newX - newX.toInt()
                var yOffset = newY - newY.toInt()

                // egress offset
                if (portal.direction == Direction.NORTH) {
                    xOffset -= 1
                } else if (portal.direction == Direction.SOUTH) {
                    xOffset += 1
                } else if (portal.direction == Direction.EAST) {
                    yOffset -= 1
                } else if (portal.direction == Direction.WEST) {
                    yOffset += 1
                }

                val isMirrorPortal = cardinalDirection == portal.direction
                var rotateRayDeg = Direction.degreeRelationship(cardinalDirection, portal.direction)
                
                val newPos = Vector(xOffset, yOffset).rotate(rotateRayDeg)

                position.x = portal.mapX + newPos.x
                position.y = portal.mapY + newPos.y



                // not tested
                if (isMirrorPortal) {
                    if (cardinalDirection == Direction.EAST || cardinalDirection == Direction.WEST) {
                        direction.mirrorX()
                        camPlane.mirrorX()
                    } else {
                        direction.mirrorY()
                        camPlane.mirrorY()
                    }
                    return
                }

                direction.rotate(rotateRayDeg)
                camPlane.rotate(rotateRayDeg)
                return
            }
        }

        moveWithBoundsCheck(speed)
    }

    private fun moveWithBoundsCheck(speed: Double) {
        //println("x $mapPosX y $mapPosY")
        if (map[(position.x + direction.x * speed * hitBox).toInt()][mapPosY] == 0) position.x += direction.x * speed
        if (map[mapPosX][(position.y + direction.y * speed * hitBox).toInt()] == 0) position.y += direction.y * speed
    }
}

class Game(val buffer: IntArray,
           val width: Int,
           val height: Int) {

    val map = arrayOf(

            // X goes down y goes across
            intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
            intArrayOf(1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 2, 1),
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

    val portalMap = HashMap<Portal, Portal>()

    val player = Player(
            Vector(1.5, 2.5),
            Vector(-1.0, 0.0),
            Vector(0.0, 0.66),
            map,
            portalMap
    )

    init {
        portalMap[Portal(0,4, Direction.SOUTH)] = Portal(5,10, Direction.NORTH)
        portalMap[Portal(5,10, Direction.NORTH)] = Portal(0, 4, Direction.SOUTH)

        portalMap[Portal(5,0, Direction.EAST)] = Portal(7,0, Direction.EAST)
        portalMap[Portal(7,0, Direction.EAST)] = Portal(5, 0, Direction.EAST)

        portalMap[Portal(0,2, Direction.SOUTH)] = Portal(3, 0, Direction.EAST)
        portalMap[Portal(3,0, Direction.EAST)] = Portal(0, 2, Direction.SOUTH)
    }

    fun tick(frame: Int) {

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

           // intensity -= (ray.distance * 8).toInt()

            if (ray.wallHitDirection == Direction.NORTH) {
                shade = color(0, 0, intensity)
            } else if (ray.wallHitDirection == Direction.SOUTH) {
                shade = color(0, intensity, 0)
            } else if (ray.wallHitDirection == Direction.EAST) {
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
        val camX: Double = 2 * x / width.toDouble() - 1 // x-coordinate in camera space
        val rayDirection = Vector(player.direction.x + player.camPlane.x * camX, player.direction.y + player.camPlane.y * camX)

        var ray = cast(rayDirection, Vector(player.position.x, player.position.y), player.mapPosX, player.mapPosY)

        // TODO: Cap this?
        while (true) {
            var newRay = castPortalRay(rayDirection, ray)
            if (newRay == null) {
                return ray
            } else {
                ray = newRay
            }
        }
    }

    fun castPortalRay(rayDirection: Vector, ray: Ray): Ray? {
        val prevOrigin = ray.origin // Maybe copy here?
        val portal = portalMap[Portal(ray.mapX, ray.mapY, ray.wallHitDirection)] ?: return null
        // Below here means the ray hit a portal wall

        // Portals share the same wall (no rotation, just flipping)
        val isMirrorPortal = ray.wallHitDirection == portal.direction

        // If we need to rotate (its not a mirror) the ammount of degrees we need to rotate
        var rotateRayDeg = Direction.degreeRelationship(ray.wallHitDirection, portal.direction)

        // find the offset of the player from the ray hit destination
        // then apply that offset to the exit portal origin
        var xOffset = prevOrigin.x - ray.mapX
        var yOffset = prevOrigin.y - ray.mapY

        // if its not a mirror, rotate the ray wallHitDirection
        if (!isMirrorPortal) rayDirection.rotate(rotateRayDeg)

        // if a portal hits a wall on the south side (example), we need to correct the exit coordinates to cast the ray from the north side
        // so the player can pass though the portal block
        var xWallCorrect = if (ray.side == 0) ray.stepX else 0
        var yWallCorrect = if (ray.side == 1) ray.stepY else 0

        // If the ray has to make a 90 degree rotation or is mirrored, we need to wall correct by 2 blocks
        if (Math.abs(rotateRayDeg) != 0.0 || ray.wallHitDirection == portal.direction) {
            xWallCorrect *= 2
            yWallCorrect *= 2
        }

        // find the origin and wall offset
        var origin = Vector(xOffset + xWallCorrect,
                            yOffset + yWallCorrect)

        // rotate it and offset it from the portal coords
        if (!isMirrorPortal) origin.rotate(rotateRayDeg)
        origin.x += portal.mapX
        origin.y += portal.mapY

        // Perform mirroring if its a mirror portal
        if (isMirrorPortal) {
            if (ray.side == 0) {
                rayDirection.mirrorX()
                origin.mirrorX()
            } else {
                rayDirection.mirrorY()
                origin.mirrorY()
            }
        }

        return cast(rayDirection, origin, portal.mapX, portal.mapY)
    }


    fun cast(rayDirection: Vector, origin: Vector, mapXOrigin: Int, mapYOrigin: Int): Ray {

        var mapX = mapXOrigin
        var mapY = mapYOrigin

        var perpWallDist: Double

        var sideDistX: Double
        var sideDistY: Double

        var deltaDistX = Math.abs(1 / rayDirection.x)
        var deltaDistY = Math.abs(1 / rayDirection.y)

        var stepX = 0
        var stepY = 0

        var hit = 0
        var side = 0

        if (rayDirection.x < 0) {
            stepX = -1
            sideDistX = (origin.x - mapX) * deltaDistX
        } else {
            stepX = 1
            sideDistX = (mapX + 1 - origin.x) * deltaDistX
        }

        if (rayDirection.y < 0) {
            stepY = -1
            sideDistY = (origin.y - mapY) * deltaDistY
        } else {
            stepY = 1
            sideDistY = (mapY + 1 - origin.y) * deltaDistY
        }

        var portalsPassed = 0
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
            perpWallDist = (mapX - origin.x + (1 - stepX) / 2) / rayDirection.x
        } else {
            perpWallDist = (mapY - origin.y + (1 - stepY) / 2) / rayDirection.y
        }

        if (portalsPassed > 0) {
            println("dist: $perpWallDist x: $mapX y: $mapY")
        }

        var wallHitDirection: Direction
        if (side == 0 && rayDirection.x >= 0) {
            wallHitDirection = Direction.NORTH
        } else if (side == 0 && rayDirection.x < 0) {
            wallHitDirection = Direction.SOUTH
        } else if (side == 1 && rayDirection.y >= 0) {
            wallHitDirection = Direction.WEST
        } else {
            wallHitDirection = Direction.EAST
        }

        return Ray(mapX, mapY, stepX, stepY, perpWallDist, side, origin, wallHitDirection)
    }

}