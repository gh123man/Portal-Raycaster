package io.sb1.rayportal.engine

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import functions.FakeSSAOCurve
import functions.HalfCircle

data class PaintContext(val wallHeight: Int,
                        val start: Int,
                        val end: Int,
                        val clipped: Boolean)

class RenderUtil(private val buffer: Pixmap,
                 val width: Int,
                 val height: Int) {

    val halfCircle = HalfCircle()
    var wallShadeCurve = FakeSSAOCurve()

    fun calcDrawStartEnd(ray: Ray): PaintContext {
        val wallHeight = (height / ray.distance).toInt()
        var drawStart = -(wallHeight / 2) + (height / 2)
        if (drawStart < 0) drawStart = 0

        var drawEnd = (wallHeight / 2) + (height / 2) + 1
        var clipped = false
        if (drawEnd >= height) {
            clipped = true
            drawEnd = height - 1
        }
        return PaintContext(wallHeight, drawStart, drawEnd, clipped)
    }

    fun paint(rays: List<Ray>, x: Int) {

        // DEBUG
        //paintLine(x, 0, height - 1, 0)

        if (rays.count() > 1) {
            paintPortals(rays, x)
        } else {
            val lastRay = rays.last()
            paintWall(lastRay, x)
            paintFloor(lastRay, x, calcDrawStartEnd(lastRay).end, height - 1)
        }
    }

    /**
     * Paints a single wall column with shading from the context of a ray
     */
    fun paintPortals(rays: List<Ray>, x: Int) {

        // First paint the furthest wall
        val lastRay = rays.last()
        paintWall(lastRay, x)

        // Reverse the list so start with the furthest away ray
        val reversedRays = rays.reversed()
        for (i in reversedRays.indices) {

            val currentWallRay = reversedRays[i]
            val nextWallRay = reversedRays.getOrElse(i + 1) {
                reversedRays[i]
            }

            // Measure the distance between the bottom of the further away portal and the closer portal (next portal)
            val (_, _, currentDrawEnd, _) = calcDrawStartEnd(currentWallRay) // further away wall

            // Draw the last floor up to the player/camera
            if (currentWallRay == nextWallRay) {
                paintFloor(currentWallRay, x, currentDrawEnd, height - 1)
            } else {
                // this is a view through a portal. mask portal walls here
                var (wallHeight, nextDrawStart, nextDrawEnd, _) = calcDrawStartEnd(nextWallRay) // closer wall to player
                paintFloor(currentWallRay, x, currentDrawEnd, nextDrawEnd)
                paintMask(nextWallRay, x)

            }
        }
    }

    fun paintMask(ray: Ray, x: Int) {

        var (wallHeight, start, end, _) = calcDrawStartEnd(ray)

        // start and end are clamped to screen space height whne the wall is taller than the window
        // so we need to calculate the wall height in screenspace to prevent the circle from warping when
        // the player gets near the edge
        val halfRealWallHeight = (end - start) / 2
        val halfCircleYPos = (halfCircle.yForX(ray.wallX) * wallHeight).toInt()

        // Top half Circle
        var circleEndTop = end - halfCircleYPos - halfRealWallHeight
        for (y in start until circleEndTop) {
            buffer.drawPixel(x, y, wallSSAO(y, start, end, wallColor(ray)))
        }

        // Bottom half circle
        var circleStartBottom = start + halfCircleYPos + halfRealWallHeight
        for (y in circleStartBottom until end) {
            buffer.drawPixel(x, y, wallSSAO(y, start, end, wallColor(ray)))
        }
    }

    // reduce the wall color against a parabolic function to darken the start and end
    fun wallSSAO(y: Int, start: Int, end: Int, color: Int): Int {
        return color
        // Convert start, end, and y into a value in the range 0 to 1
        val yRange = 1 / ((end.toDouble() - start) / (y + 1 - start))

        // Convert to int to reduce color
        val amount = (wallShadeCurve.yForX(yRange).toFloat()) * -1f

        return Color(color).add(0f, 0f, 0f, 0f).toIntBits()

        //return ColorUtil.darken(color, amount)
    }

    fun wallColor(ray: Ray): Int {
        var intensity = if (ray.side == 0) 255 else 150

        intensity -= (ray.distance * 2).toInt()
        if (intensity < 0) {
            intensity = 0
        }

        return if (ray.wallHitDirection == Direction.NORTH) {
            Color.toIntBits(0, 0, 0, intensity)
        } else if (ray.wallHitDirection == Direction.SOUTH) {
            Color.toIntBits(0, 0, intensity, 0)
           // ColorUtil.color(0, intensity, 0)
        } else if (ray.wallHitDirection == Direction.EAST) {
            Color.toIntBits(0, intensity, 0, 0)
            //ColorUtil.color(intensity, 0, 0)
        } else {
            Color.toIntBits(0, intensity, intensity, 0)
            //ColorUtil.color(0, intensity, intensity)
        }
    }

    /**
     * Paints a single wall column with shading from the context of a ray
     */
    fun paintWall(ray: Ray, x: Int) {
        var (_, drawStart, drawEnd, _) = calcDrawStartEnd(ray)
        for (y in drawStart until drawEnd) {
            buffer.drawPixel(x, y, wallSSAO(y, drawStart, drawEnd, wallColor(ray)))
        }
    }

    /**
     * Paints a single floor column given the context of 1 ray. (portals excluded)
     */
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
            currentDist = height / (2.0 * y - height)

            val weight = currentDist / distWall

            val currentFloorX = weight * floorXWall + (1.0 - weight) * ray.origin.x
            val currentFloorY = weight * floorYWall + (1.0 - weight) * ray.origin.y

            val checkerBoardPattern = ((currentFloorX).toInt() + (currentFloorY).toInt()) % 2

            val floorColor = if(checkerBoardPattern == 0) Color.argb8888(0f, 1f, 1f, 1f)
            else                         Color.rgb888(0.2f, 0.2f, 0.2f)

            val ceilingColor = if(checkerBoardPattern == 0) Color.toIntBits(119, 119, 119, 0)
            else                         Color.toIntBits(226, 226, 226, 0)

            buffer.drawPixel(x, y, floorColor)
            buffer.drawPixel(x, (height - y), ceilingColor)
        }
    }

}