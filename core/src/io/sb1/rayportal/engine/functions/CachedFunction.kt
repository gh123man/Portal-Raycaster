package functions

abstract class CachedFunction(private val size: Int) {

    private var cache = DoubleArray(size + 1).map { Double.POSITIVE_INFINITY }.toDoubleArray()

    abstract fun function(x: Double): Double

    fun yForX(x: Double): Double {
        val index = (x * size).toInt()
        val cachedVal = cache[index]
        if (cachedVal != Double.POSITIVE_INFINITY) {
            return cachedVal
        }
        val y = function(x)
        cache[index] = y
        return y
    }
}