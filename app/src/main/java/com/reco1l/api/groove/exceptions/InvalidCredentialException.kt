package com.reco1l.api.groove.exceptions

import com.reco1l.api.ApiException
import com.reco1l.api.groove.Credential
import com.reco1l.api.groove.Credential.Companion.PASSWORD
import com.reco1l.api.groove.Credential.Companion.USERNAME

class InvalidCredentialException(@Credential val credential: Int) : ApiException()
{
    override val message: String
        get() = when (credential)
        {
            USERNAME -> "Username is not valid!"
            PASSWORD -> "Password is not valid!"
            else -> "One or both credentials are invalid"
        }
}
