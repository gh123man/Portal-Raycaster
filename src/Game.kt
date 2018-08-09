import java.util.*
import kotlin.collections.HashMap



class PortalManager {

    private val portalMap = HashMap<Portal, Portal>()
    private val portalLocMap = HashMap<Pair<Int, Int>, Portal>()

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


class Game(private val renderUtil: RenderUtil) {


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


        for (x in 0..renderUtil.width) {
           //paintLine(x, 0, height - 1, 0)

            val rays = castRay(x)
            val lastRay = rays.last()

            var shade: Int
            var intensity = if (lastRay.side == 0) 255 else 150

            intensity -= (lastRay.distance * 2).toInt()
            if (intensity < 0) {
                intensity = 0
            }

            if (lastRay.wallHitDirection == Direction.NORTH) {
                shade = Color.color(0, 0, intensity)
            } else if (lastRay.wallHitDirection == Direction.SOUTH) {
                shade = Color.color(0, intensity, 0)
            } else if (lastRay.wallHitDirection == Direction.EAST) {
                shade = Color.color(intensity, 0, 0)
            } else {
                shade = Color.color(0, intensity, intensity)
            }

            var (drawStart, drawEnd) = renderUtil.calcDrawStartEnd(lastRay)
            renderUtil.paintLine(x, drawStart, drawEnd, shade)

            // Reverse the list so start with the furthest away ray
            val reversedRays = rays.reversed()
            for (i in reversedRays.indices) {

                val first = reversedRays[i]
                val second = reversedRays.getOrElse(i + 1) {
                    lastRay
                }

                val (_, firstDrawEnd) = renderUtil.calcDrawStartEnd(first)
                var (_, secondDrawEnd) = renderUtil.calcDrawStartEnd(second)

                if (second == lastRay) {
                    secondDrawEnd = renderUtil.height - 1
                }

                paintFloor(first, x, firstDrawEnd, secondDrawEnd)
            }
        }
    }

    private fun castRay(x: Int): List<Ray> {
        val camX: Double = 2 * x / renderUtil.width.toDouble() - 1 // x-coordinate in camera space
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

    private fun castPortalRay(ray: Ray): Ray? {
        val prevOrigin = ray.origin.copy()
        val portal = portalManager.getPortal(ray.mapX, ray.mapY, ray.wallHitDirection) ?: return null
        // Below here means the ray hit a portal wall

        // amount we need to rotate the ray to come out of the exit portal
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

        // offset the new origin to position it at the exit portal
        origin.x += portal.mapX
        origin.y += portal.mapY

        // rotate the ray direction and cast a new ray
        return castWall(ray.direction.copy().rotate(rotateRayDeg), origin, portal.mapX, portal.mapY)
    }


    private fun castWall(rayDirection: Vector, origin: Vector, mapXOrigin: Int, mapYOrigin: Int): Ray {

        var mapX = mapXOrigin
        var mapY = mapYOrigin

        var perpWallDist: Double

        var sideDistX: Double
        var sideDistY: Double

        var deltaDistX = Math.abs(1 / rayDirection.x)
        var deltaDistY = Math.abs(1 / rayDirection.y)

        var stepX: Int
        var stepY: Int

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
        perpWallDist = if (side == 0) {
            (mapX - origin.x + (1 - stepX) / 2) / rayDirection.x
        } else {
            (mapY - origin.y + (1 - stepY) / 2) / rayDirection.y
        }

        val wallHitDirection = if (side == 0 && rayDirection.x >= 0) {
            Direction.NORTH
        } else if (side == 0 && rayDirection.x < 0) {
            Direction.SOUTH
        } else if (side == 1 && rayDirection.y >= 0) {
            Direction.WEST
        } else {
            Direction.EAST
        }

        var wallX = if (side == 0)  origin.y + perpWallDist * rayDirection.y
                    else            origin.x + perpWallDist * rayDirection.x
        wallX -= Math.floor(wallX)

        return Ray(mapX, mapY, stepX, stepY, perpWallDist, side, wallX, origin, rayDirection, wallHitDirection)
    }

    private fun  paintFloor(ray: Ray, x: Int, wallDrawEnd: Int, stopHeight: Int) {
        var floorXWall: Double
        var floorYWall: Double

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

        val distWall = ray.distance
        var currentDist: Double

        var drawEnd = wallDrawEnd
        if (drawEnd < 0) drawEnd = stopHeight

        for (y in drawEnd until stopHeight) {
            currentDist = renderUtil.height / (2.0 * y - renderUtil.height)

            val weight = currentDist / distWall

            val currentFloorX = weight * floorXWall + (1.0 - weight) * ray.origin.x
            val currentFloorY = weight * floorYWall + (1.0 - weight) * ray.origin.y

            val checkerBoardPattern = ((currentFloorX).toInt() + (currentFloorY).toInt()) % 2

            val floorColor = if(checkerBoardPattern == 0) Color.color(119, 119, 119)
                             else                         Color.color(51, 51, 51)

            val ceilingColor = if(checkerBoardPattern == 0) Color.color(119, 119, 119)
                               else                         Color.color(226, 226, 226)

            renderUtil.buffer[(y * renderUtil.width + x)] = floorColor
            renderUtil.buffer[(renderUtil.height - y) * renderUtil.width + x] = ceilingColor

        }
    }
}