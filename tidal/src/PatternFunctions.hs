module PatternFunctions where

import Sound.Tidal.Context
import Sound.Tidal.Params

-- Equalizer
let hiPassFreq = pF "hiPassFreq"
    hiPassRq   = pF "hiPassRq"
    hiPassBypass   = pF "hiPassBypass"
    loShelfFreq = pF "loShelfFreq"
    loShelfGain = pF "loShelfGain"
    loShelfRs   = pF "loShelfRs"
    loShelfBypass   = pF "loShelfBypass"
    loPeakFreq  = pF "loPeakFreq"
    loPeakGain  = pF "loPeakGain"
    loPeakRq    = pF "loPeakRq"
    loPeakBypass    = pF "loPeakBypass"
    midPeakFreq = pF "midPeakFreq"
    midPeakGain = pF "midPeakGain"
    midPeakRq   = pF "midPeakRq"
    midPeakBypass   = pF "midPeakBypass"
    hiPeakFreq  = pF "hiPeakFreq"
    hiPeakGain  = pF "hiPeakGain"
    hiPeakRq    = pF "hiPeakRq"
    hiPeakBypass    = pF "hiPeakBypass"
    hiShelfFreq = pF "hiShelfFreq"
    hiShelfGain = pF "hiShelfGain"
    hiShelfRs   = pF "hiShelfRs"
    hiShelfBypass   = pF "hiShelfBypass"
    loPassFreq = pF "loPassFreq"
    loPassRq   = pF "loPassRq"
    loPassBypass   = pF "loPassBypass"

-- Compressor
let cpAttack = pF "cpAttack"
    cpRelease = pF "cpRelease"
    cpThresh = pF "cpThresh"
    cpTrim = pF "cpTrim"
    cpGain = pF "cpGain"
    cpRatio = pF "cpRatio"
    cpLookahead = pF "cpLookahead"
    cpSaturate = pF "cpSaturate"
    cpHpf = pF "cpHpf"
    cpKnee = pF "cpKnee"
    cpBias = pF "cpBias"

-- Mixer
let masterGain = pF "masterGain"
