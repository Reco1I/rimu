package game.rimu.data

import androidx.room.Entity


@Entity
data class Filename(

    val name: String,

    val type: String,

    val withVariants: Boolean,

    val withHyphen: Boolean

)