package com.spotifyhub.ui.nowplaying.backdrop

data class BackdropSeed(
    val x: Float,
    val y: Float,
    val z: Float,
    val w: Float,
) {
    val uniformArray: FloatArray = floatArrayOf(x, y, z, w)
}

object BackdropSeedFactory {
    fun from(key: String): BackdropSeed {
        var state = 1125899906842597L
        key.forEach { character ->
            state = (state * 1315423911L) xor character.code.toLong()
        }

        return BackdropSeed(
            x = nextFloat(state).also { state = advanceState(state) },
            y = nextFloat(state).also { state = advanceState(state) },
            z = nextFloat(state).also { state = advanceState(state) },
            w = nextFloat(state),
        )
    }

    private fun advanceState(input: Long): Long {
        var value = input * 2862933555777941757L + 3037000493L
        value = value xor (value ushr 17)
        return value
    }

    private fun nextFloat(input: Long): Float {
        val value = advanceState(input)
        return ((value ushr 40) and 0xFFFFFFL).toFloat() / 0xFFFFFF.toFloat()
    }
}
