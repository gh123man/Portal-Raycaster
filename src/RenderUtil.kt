class RenderUtil(val buffer: IntArray,
                 val width: Int,
                 val height: Int) {

    fun calcDrawStartEnd(ray: Ray): Pair<Int, Int> {
        val wallHeight = (height / ray.distance).toInt()
        var drawStart = -(wallHeight / 2) + (height / 2)
        if (drawStart < 0) drawStart = 0

        var drawEnd = (wallHeight / 2) + (height / 2) + 1
        if (drawEnd >= height) drawEnd = height - 1
        return Pair(drawStart, drawEnd)
    }

    fun paintLine(x: Int, start: Int, end: Int, color: Int) {
//        var shadePx = 16 + start
        for (y in start until end) {
            // Shading testing
//            if (y < shadePx) {
//                buffer[y * width + x] = Color.darken(color, (start - shadePx) * -1)
//                shadePx --
//                continue
//            }
            buffer[y * width + x] = color
        }
    }

}