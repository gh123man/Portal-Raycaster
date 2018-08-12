package functions

import kotlin.math.pow

class HalfCircle: CachedFunction(10000) {
    override fun function(x: Double): Double = Math.sqrt((-0.5).pow(2) - (x - 0.5).pow(2))
}

class FakeSSAOCurve: CachedFunction(500) {
    override fun function(x: Double): Double = ((x - 0.5) / 0.5).pow(10)
}