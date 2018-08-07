import java.util.*
import kotlin.collections.HashMap

class Portal(var mapX: Int, var mapY: Int, var direction: Direction) {
    override fun hashCode(): Int {
        return Objects.hash(mapX, mapY, direction)
    }

    override fun equals(other: Any?): Boolean {
        return other?.hashCode() == hashCode()
    }

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

class Player(var position: Vector,
             var direction: Vector,
             var camPlane: Vector,
             var game: Game) {

    val hitBox = 8

    val mapPosX: Int
        get() = position.x.toInt()

    val mapPosY: Int
        get() = position.y.toInt()

    fun rotate(degrees: Double) {
        if (degrees == 0.0) return
        direction.rotate(degrees)
        camPlane.rotate(degrees)
    }

    fun move(speed: Double) {
        if (speed == 0.0) return

        var nextPos = newPosition(speed)
        val directionMoved = directionMoved(nextPos)

        if (directionMoved != null) {
            val portal = game.portalManager.getPortal(nextPos.x.toInt(), nextPos.y.toInt(), directionMoved)
            if (portal != null) {
                walkThroughPortal(portal, directionMoved, speed)
                return
            }
        }

        // the player did not walk through a portal, but if they are walking near one we want to let them get closer
        // than the hitbox allows
        val nexPosWithBounds = newPosition(speed * hitBox)
        val directionMovedWithBounds = directionMoved(nexPosWithBounds)
        if (directionMovedWithBounds != null) {
            var moved = false
            var portal = game.portalManager.getPortal(nexPosWithBounds.x.toInt(), mapPosY, directionMovedWithBounds)
            if (portal != null) {
                position.x = nextPos.x
                moved = true
            }

            portal = game.portalManager.getPortal(mapPosX, nexPosWithBounds.y.toInt(), directionMovedWithBounds)
            if (portal != null) {
                position.y = nextPos.y
                moved = true
            }

            if (moved) return
        }

        moveWithBoundsCheck(speed)
    }

    private fun moveWithBoundsCheck(speed: Double) {
        if (game.map[(position.x + direction.x * speed * hitBox).toInt()][mapPosY] == 0) position.x += direction.x * speed
        if (game.map[mapPosX][(position.y + direction.y * speed * hitBox).toInt()] == 0) position.y += direction.y * speed
    }

    private fun newPosition(speed: Double): Vector {
        return Vector(position.x + direction.x * speed,
                      position.y + direction.y * speed)
    }

    private fun directionMoved(pos: Vector): Direction? {
        val xDir = pos.x.toInt() - mapPosX
        val yDir = pos.y.toInt() - mapPosY

        when {
            xDir < 0 -> return Direction.SOUTH
            xDir > 0 -> return Direction.NORTH
            yDir < 0 -> return Direction.EAST
            yDir > 0 -> return Direction.WEST
        }
        return null
    }

    private fun walkThroughPortal(portal: Portal, directionMoved: Direction, speed: Double) {
        /*
           1. Move the player to the new position
           2. rotate the position vector for the block the player is in
           3. offset by the block value in the direction you are walking
           4. apply the requested movement
        */

        val playerOffsetVector = Vector(position.x - mapPosX,
                                        position.y - mapPosY)
        var rotateRayDeg = Direction.degreeRelationship(directionMoved, portal.direction)

        val mapOffsetVector = Vector(0.0, 0.0)
        Portal.displaceVectorForRotation(mapOffsetVector, rotateRayDeg)

        direction.rotate(rotateRayDeg)
        camPlane.rotate(rotateRayDeg)
        playerOffsetVector.rotate(rotateRayDeg)

        var newX = portal.mapX + playerOffsetVector.x + mapOffsetVector.x + direction.x * speed
        var newY = portal.mapY + playerOffsetVector.y + mapOffsetVector.y + direction.y * speed

        if (game.map[newX.toInt()][mapPosY] == 0) position.x = newX
        if (game.map[mapPosX][newY.toInt()] == 0) position.y = newY


    }
}

class PortalManager {

    val portalMap = HashMap<Portal, Portal>()
    val portalLocMap = HashMap<Pair<Int, Int>, Portal>()

    fun addPortal(x1: Int, y1: Int, d1: Direction, x2: Int, y2: Int, d2: Direction) {
        val p1 = Portal(x1,y1, d1)
        val p2 = Portal(x2,y2, d2)
        portalMap[p1] = p2
        portalMap[p2] = p1
        portalLocMap[Pair(x1, y1)] = p1
        portalLocMap[Pair(x2, y2)] = p2
    }

    fun getPortal(x: Int, y: Int, d: Direction): Portal? {
        return portalMap[Portal(x, y, d)]
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

    val player = Player(
            Vector(1.5, 2.5),
            Vector(-1.0, 0.0),
            Vector(0.0, 0.66),
            this
    )

    val portalManager = PortalManager()

    init {
        portalManager.addPortal(0, 4, Direction.SOUTH, 5, 10, Direction.NORTH)
        portalManager.addPortal(5, 0, Direction.EAST, 7, 0, Direction.EAST)
        portalManager.addPortal(14, 5, Direction.NORTH, 14, 6, Direction.NORTH)
        portalManager.addPortal(0, 2, Direction.SOUTH, 3, 0, Direction.EAST)
    }

    fun tick(frame: Int) {


        for (x in 0..width) {
           //paintLine(x, 0, height - 1, 0)

            val rays = castRay(x)
            val lastRay = rays.last()

            var shade = 0
            var intensity = if (lastRay.side == 0) 255 else 150

            intensity -= (lastRay.distance * 2).toInt()
            if (intensity < 0) {
                intensity = 0
            }

            if (lastRay.wallHitDirection == Direction.NORTH) {
                shade = color(0, 0, intensity)
            } else if (lastRay.wallHitDirection == Direction.SOUTH) {
                shade = color(0, intensity, 0)
            } else if (lastRay.wallHitDirection == Direction.EAST) {
                shade = color(intensity, 0, 0)
            } else {
                shade = color(0, intensity, intensity)
            }

            var (drawStart, drawEnd) = calcDrawStartEnd(lastRay)
            paintLine(x, drawStart, drawEnd, shade)

            // Reverse the list so start with the furthest away ray
            val reversedRays = rays.reversed()
            for (i in reversedRays.indices) {

                val first = reversedRays[i]
                val second = reversedRays.getOrElse(i + 1) {
                    lastRay
                }

                val (_, firstDrawEnd) = calcDrawStartEnd(first)
                var (_, secondDrawEnd) = calcDrawStartEnd(second)

                if (second == lastRay) {
                    secondDrawEnd = height - 1
                }

                paintFloor(first, x, firstDrawEnd, secondDrawEnd)
            }
        }
    }

    fun calcDrawStartEnd(ray: Ray): Pair<Int, Int> {
        val wallHeight = (height / ray.distance).toInt()
        var drawStart = -(wallHeight / 2) + (height / 2)
        if (drawStart < 0) drawStart = 0

        var drawEnd = (wallHeight / 2) + (height / 2) + 1
        if (drawEnd >= height) drawEnd = height - 1
        return Pair(drawStart, drawEnd)
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

    fun castRay(x: Int): List<Ray> {
        val camX: Double = 2 * x / width.toDouble() - 1 // x-coordinate in camera space
        val rayDirection = Vector(player.direction.x + player.camPlane.x * camX, player.direction.y + player.camPlane.y * camX)

        var ray = castWall(rayDirection, Vector(player.position.x, player.position.y), player.mapPosX, player.mapPosY)
        var rays = arrayListOf(ray)

        // TODO: Cap this?
        while (true) {
            var newRay = castPortalRay(rays.last())
            if (newRay == null) {
                return rays
            } else {
                rays.add(newRay)
            }
        }
    }

    fun castPortalRay(ray: Ray): Ray? {
        val prevOrigin = ray.origin.copy()
        val portal = portalManager.getPortal(ray.mapX, ray.mapY, ray.wallHitDirection) ?: return null
        // Below here means the ray hit a portal wall

        // If we need to rotate (its not a mirror) the ammount of degrees we need to rotate
        var rotateRayDeg = Direction.degreeRelationship(ray.wallHitDirection, portal.direction)

        // find the offset of the player from the ray hit destination
        // then apply that offset to the exit portal origin
        var origin = Vector(prevOrigin.x - ray.mapX,
                            prevOrigin.y - ray.mapY)

        // if a portal hits a wall on the south side (example), we need to correct the exit coordinates to castWall the ray from the north side
        // so the player can pass though the portal block
        val wallCorrection = Vector(if (ray.side == 0) ray.stepX else 0,
                                    if (ray.side == 1) ray.stepY else 0)

        origin.add(wallCorrection)

        // rotate the origin in relation to the portal
        origin.rotate(rotateRayDeg)

        // offset the portal by one block depending on how much it rotates
        Portal.displaceVectorForRotation(origin, rotateRayDeg)

        origin.x += portal.mapX
        origin.y += portal.mapY

        return castWall(ray.direction.copy().rotate(rotateRayDeg), origin, portal.mapX, portal.mapY)
    }


    fun castWall(rayDirection: Vector, origin: Vector, mapXOrigin: Int, mapYOrigin: Int): Ray {

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

        var wallX = 0.0
        if (side == 0) wallX = origin.y + perpWallDist * rayDirection.y
        else wallX = origin.x + perpWallDist * rayDirection.x
        wallX -= Math.floor(wallX)

        return Ray(mapX, mapY, stepX, stepY, perpWallDist, side, wallX, origin, rayDirection, wallHitDirection)
    }

    fun  paintFloor(ray: Ray, x: Int, wallDrawEnd: Int, stopHeight: Int) {
        var floorXWall = 0.0
        var floorYWall = 0.0

        if(ray.side == 0 && ray.direction.x > 0) {
            floorXWall = ray.mapX.toDouble()
            floorYWall = ray.mapY + ray.wallX
        } else if(ray.side == 0 && ray.direction.x < 0) {
            floorXWall = ray.mapX + 1.0
            floorYWall = ray.mapY + ray.wallX
        } else if(ray.side == 1 && ray.direction.y > 0) {
            floorXWall = ray.mapX + ray.wallX
            floorYWall = ray.mapY.toDouble()
        } else {
            floorXWall = ray.mapX + ray.wallX
            floorYWall = ray.mapY + 1.0
        }

        var distWall = ray.distance
        var currentDist = 0.0

        var drawEnd = wallDrawEnd
        if (drawEnd < 0) drawEnd = stopHeight

        for (y in drawEnd..stopHeight - 1) {
            currentDist = height / (2.0 * y - height)

            var weight = currentDist / distWall

            var currentFloorX = weight * floorXWall + (1.0 - weight) * ray.origin.x
            var currentFloorY = weight * floorYWall + (1.0 - weight) * ray.origin.y

            var checkerBoardPattern = ((currentFloorX).toInt() + (currentFloorY).toInt()) % 2

            var floorColor = 0
            if(checkerBoardPattern == 0) floorColor = color(119, 119, 119)
            else floorColor = color(51, 51, 51)

            var ceilingColor  = 0
            if(checkerBoardPattern == 0) ceilingColor =  color(119, 119, 119)
            else ceilingColor = color(226, 226, 226)

            buffer[(y * width + x)] = floorColor
            buffer[(height - y) * width + x] = ceilingColor

        }
    }
}