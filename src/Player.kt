class Player(var position: Vector,
             var direction: Vector,
             var camPlane: Vector,
             var game: Game) {

    val hitBox = 2

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