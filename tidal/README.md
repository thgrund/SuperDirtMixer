# SuperDirtMixer introduction for TidalCycles

In this document you find everything you need to know, to control the SuperDirtMixer from TidalCycles with patterns.

To start follow these steps:

- The SuprtDirtMixer needs to be installed and launched.
- Execute the TidalCycles code in the section "BootTidal". Whit these additions you should be able to change the SuperDirtMixer values within TidalCycles

## Limitations

- You can not change the value of the bypass buttons from TidalCycles.
- When you hush the stream, then the last values before the hush are still active. You can not reset to the point, before your TidalCycles code was executed. Use the `reset all` button or load a different preset, if it's needed.
- You could change the values without defining a new target. The only downside would be, that the mixer is not able to respond to these changes and is not able to update the ui. On the bright side you keep the previous state in the SuperDirtMixer before you executed your code.

## BootTidal

This is the TidalCycles code you need to execute. I prefer to add it to the `BootTidal.hs` file, then it will be used every time you launch TidalCycles.

```haskell
-- New stream definition 
let superDirtMixerTarget = Target {
	oName = "superDirtMixer"
	, oAddress = "localhost"
	, oPort = 57121
	, oLatency = 0.2
	, oSchedule = Live
	, oWindow = Nothing
	, oHandshake = False
	, oBusPort = Nothing }
    oscplay = OSC "/SuperDirtMixer" Named {requiredArgs = ["orbit"]}
    oscmap = [(superDirtMixerTarget, [oscplay])]

superDirtMixer <- startStream defaultConfig oscmap

let x1 = streamReplace superDirtMixer 1 . (|< orbit 0)
    x2 = streamReplace superDirtMixer 2 . (|< orbit 1)
    x3 = streamReplace superDirtMixer 3 . (|< orbit 2)
    x4 = streamReplace superDirtMixer 4 . (|< orbit 3)
    x5 = streamReplace superDirtMixer 5 . (|< orbit 4)
    x6 = streamReplace superDirtMixer 6 . (|< orbit 5)
    x7 = streamReplace superDirtMixer 7 . (|< orbit 6)
    x8 = streamReplace superDirtMixer 8 . (|< orbit 7)
    x9 = streamReplace superDirtMixer 9 . (|< orbit 8)
    x10 = streamReplace superDirtMixer 10 . (|< orbit 9)
    x11 = streamReplace superDirtMixer 11 . (|< orbit 10)
    x12 = streamReplace superDirtMixer 12 . (|< orbit 11)
    x13 = streamReplace superDirtMixer 13 . (|< orbit 12)
    x14 = streamReplace superDirtMixer 14 . (|< orbit 13)

-- At least this is what you should add regarding the stream functions in the BootTidal file. This will stop the SuperDirtMixer stream when when you stop the TidalCycles stream.
hush = do
    streamHush tidal
    streamHush superDirtMixer
    
:{
-- SuperDirtMixer related functions

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
    cpAttack = pF "cpAttack"
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
    masterGain = pF "masterGain"
:}
```

## TidalCycle Example

To see what happens in the following example, the `fx` button of orbit 1 needs to be active in the SuperDirtMixer and the bypass button for the equalizer and the compressor needs to be off.

```haskell
x1 $
  stack [
  	 -- Equalizer 
     every 2 rev $ loShelfFreq "100"
        <| loShelfGain
        (segment 64 $ range "-8" 16 sine)
    ,loPeakGain "<-10 -5 8>"
        <| (slow 2 $ loPeakFreq         
        (segment 64 $ range 100 5000 sine))
    ,  midPeakGain "<10>"
        <| midPeakFreq
        (slow 2 $ segment 64 $ range 400 2000 isaw)
    , hiPeakGain "<12 -6 -8>"
        <| hiPeakFreq
        (slow 2 $ segment 64 $ range 8000 1000 cosine)
    , loPassFreq "<7000 5000 10000 1000> "
    , loPassBypass "<1 0>"
    , hiShelfFreq "7000"
      <| hiShelfGain
      (segment 64 $ range "-8" 16 sine)
    -- Compressor
    , cpThresh "<-40 -20 -10 0>" <| cpRatio "1 2 3 4" # cpGain "<10 4 2 0>"
    , pan "[0 0.25 0.5 0.75 1]" # masterGain "<0 0.5 1>" # room "<1 0.5 0>"
  ]

--x1 $ silence
hush
```



