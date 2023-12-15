/*
 * @author Reco1l
 */

package com.reco1l.api.groove

import androidx.annotation.IntDef

@IntDef(value = [
    Credential.USERNAME,
    Credential.PASSWORD
])
annotation class Credential
{
    companion object
    {
        const val USERNAME = 0
        const val PASSWORD = 1
    }
}