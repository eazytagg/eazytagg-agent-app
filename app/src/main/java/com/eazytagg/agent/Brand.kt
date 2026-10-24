package com.eazytagg.agent

/** Which build this is. "pd" = Popular Dynamics door staff app, otherwise the EazyTagg Agent app. */
object Brand {
    val isPd: Boolean get() = BuildConfig.BRAND == "pd"
}
