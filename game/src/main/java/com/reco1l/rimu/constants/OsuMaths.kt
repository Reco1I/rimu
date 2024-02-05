package com.reco1l.rimu.constants


fun approachRateToSeconds(ar: Float) = if (ar <= 5) 1.8 - 0.12 * ar else 1.95 - 0.15 * ar

