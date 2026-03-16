package com.spotifyhub.ui.nowplaying.backdrop

data class BackdropSeed(
    val x: Float,
    val y: Float,
    val z: Float,
    val w: Float,
) {
    fun toUniformArray(): FloatArray = floatArrayOf(x, y, z, w)
}

object BackdropSeedFactory {
    fun from(key: String): BackdropSeed {
        var state = 1125899906842597L
        key.forEach { character ->
            state = (state * 1315423911L) xor character.code.toLong()
        }

        return BackdropSeed(
            x = nextFloat(state),
            y = nextFloat(state),
            z = nextFloat(state),
            w = nextFloat(state),
        )
    }

    private fun nextFloat(input: Long): Float {
        var value = input * 2862933555777941757L + 3037000493L
        value = value xor (value ushr 17)
        return ((value ushr 40) and 0xFFFFFFL).toFloat() / 0xFFFFFF.toFloat()
    }
}
