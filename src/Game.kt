import java.util.concurrent.Callable
import java.util.concurrent.Executors
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

    private val threads = 16
    private val maxPortalDepth = 100

    val map = arrayOf(

            // X goes down y goes across
            intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
            intArrayOf(1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1),
            intArrayOf(1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1),
            intArrayOf(1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1),
            intArrayOf(1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1),
            intArrayOf(1, 0, 0, 0, 0, 0, 0, 1, 1, 1, 2, 0, 1, 1, 1),
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
            Vector(0.0, (renderUtil.width.toDouble() / renderUtil.height.toDouble()) / 2),
            this
    )

    val portalManager = PortalManager()

    init {
        portalManager.addPortal(0, 13, Direction.SOUTH, 5, 13, Direction.NORTH)
        portalManager.addPortal(0, 4, Direction.SOUTH, 5, 9, Direction.NORTH)
        portalManager.addPortal(5, 0, Direction.EAST, 7, 0, Direction.EAST)
        portalManager.addPortal(14, 5, Direction.NORTH, 14, 6, Direction.NORTH)
        portalManager.addPortal(0, 2, Direction.SOUTH, 3, 0, Direction.EAST)
    }

    val executor = Executors.newFixedThreadPool(threads)
    val tasks = arrayListOf<Callable<Unit>>()

    fun tick(frame: Int) {

        if (tasks.count() >= threads) {
            executor.invokeAll(tasks)
            return
        }

        val chunk = renderUtil.width / threads
        for (index in 0 until threads)
            tasks.add(Callable {
                val start = index * chunk
                val end = start + chunk
                for (x in start until end) {
                    val rays = castRay(x)
                    renderUtil.paint(rays, x)
                }
            })

        executor.invokeAll(tasks)

    }

    private fun castRay(x: Int): List<Ray> {
        val camX: Double = 2 * x / renderUtil.width.toDouble() - 1 // x-coordinate in camera space
        val rayDirection = Vector(player.direction.x + player.camPlane.x * camX, player.direction.y + player.camPlane.y * camX)

        var ray = castWall(rayDirection, Vector(player.position.x, player.position.y), player.mapPosX, player.mapPosY)
        var rays = arrayListOf(ray)

        while (rays.count() < maxPortalDepth) {
            var newRay = castPortalRay(rays.last())
            if (newRay == null) {
                break
            } else {
                rays.add(newRay)
            }
        }
        return rays
    }

    private fun castPortalRay(wallRay: Ray): Ray? {
        val prevOrigin = wallRay.origin.copy()
        val portal = portalManager.getPortal(wallRay.mapX, wallRay.mapY, wallRay.wallHitDirection) ?: return null
        // Below here means the wallRay hit a portal wall

        // amount we need to rotate the wallRay to come out of the exit portal
        var rotateRayDeg = Direction.degreeRelationship(wallRay.wallHitDirection, portal.direction)

        // find the offset of the player from the wallRay hit destination
        // then apply that offset to the exit portal origin
        var origin = Vector(prevOrigin.x - wallRay.mapX,
                            prevOrigin.y - wallRay.mapY)

        // if a portal hits a wall on the south side (example), we need to correct the exit coordinates to castWall the wallRay from the north side
        // so the player can pass though the portal block
        val wallCorrection = Vector(if (wallRay.side == 0) wallRay.stepX else 0,
                                    if (wallRay.side == 1) wallRay.stepY else 0)

        origin.add(wallCorrection)

        // rotate the origin in relation to the portal
        origin.rotate(rotateRayDeg)

        // offset the portal by one block depending on how much it rotates
        Portal.displaceVectorForRotation(origin, rotateRayDeg)

        // offset the new origin to position it at the exit portal
        origin.x += portal.mapX
        origin.y += portal.mapY

        // rotate the wallRay direction and cast a new wallRay
        return castWall(wallRay.direction.copy().rotate(rotateRayDeg), origin, portal.mapX, portal.mapY)
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
}