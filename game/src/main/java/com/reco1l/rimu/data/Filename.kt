package com.reco1l.rimu.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity
data class Filename(

    @PrimaryKey
    val name: String,

    val type: String,

    val withVariants: Boolean,

    val withHyphen: Boolean

)

@Dao
interface IFilenameDAO
