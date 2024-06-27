SuperDirtMixerOSCListener {
	var orbitsElements, orbits, fxs, activeOrbit;
	var loadPreset;

	*new { | initGuiElements, initOrbits, initActiveOrbit, initLoadPreset|
        ^super.new.init(initGuiElements, initOrbits, initActiveOrbit, initLoadPreset)
    }

    init { |initGuiElements, initOrbits, initActiveOrbit, initLoadPreset|
        orbitsElements = initGuiElements[\orbits];
		orbits = initOrbits;
		fxs = initGuiElements[\fxs];
		activeOrbit = initActiveOrbit;
		loadPreset = initLoadPreset;
    }

	addMasterLevelOSCFunc { |leftIndicator, rightIndicator|
		OSCFunc({ |msg|
				{
					try {
				        var rmsL = msg[4].ampdb.linlin(-80, 0, 0, 1);
				        var peakL = msg[3].ampdb.linlin(-80, 0, 0, 1, \min);
                        var rmsR = msg[6].ampdb.linlin(-80, 0, 0, 1);
				        var peakR = msg[5].ampdb.linlin(-80, 0, 0, 1, \min);
				        leftIndicator.value = rmsL;
				        leftIndicator.peakLevel = peakL;
						rightIndicator.value = rmsR;
				        rightIndicator.peakLevel = peakR;

					} { |error|
						if(error.isKindOf(PrimitiveFailedError).not) { error.throw }
					};
				}.defer;
	       }, ("/MixerMasterOutLevels").asSymbol, Server.local.addr).fix;
	}

	addMeterResponseOSCFunc { | orbitLevelIndicators |
		OSCFunc({ |msg|
				{
					try {
					    var indicators = orbitLevelIndicators[msg[2]];

                        (0..(~dirt.numChannels - 1)).do({ |item|
						    var baseIndex = ((item + 1) * 2) + 1;
						    var rms = msg[baseIndex + 1].ampdb.linlin(-80, 0, 0, 1);
				            var peak = msg[baseIndex].ampdb.linlin(-80, 0, 0, 1, \min);

					        indicators[item].value = rms;
				            indicators[item].peakLevel = peak;
					    });

					} { |error| };
				}.defer;
			}, ("/rms")).fix;
	}

	addPanListener { OSCFunc ({|msg|
				{
				    var orbitIndex = msg[1];
				    var value     = msg[2];

				    orbits.at(orbitIndex).set(\pan, value.linlin(0,1,0,1.0));
				    orbitsElements[orbitIndex][\pan][\element].value_(orbits.at(orbitIndex).get(\pan));
					orbitsElements[orbitIndex][\pan][\value].value_(orbits.at(orbitIndex).get(\pan));
			}.defer;
	    }, ("/SuperDirtMixer/pan"), recvPort: 57120).fix;
	}

	addGainListener { OSCFunc ({|msg|
				{
				    var orbitIndex = msg[1];
				    var value     = msg[2];

				    orbits.at(orbitIndex).set(\masterGain, value.linlin(0,2,0,2));
					orbitsElements[orbitIndex][\masterGain][\element].value_((orbits.at(orbitIndex).get(\masterGain) + 1).explin(1,3, 0,1));
					orbitsElements[orbitIndex][\masterGain][\value].value_(orbits.at(orbitIndex).get(\masterGain));
			}.defer;
	    }, ("/SuperDirtMixer/masterGain"), recvPort: 57120).fix;
	}

	addReverbListener { |reverbVariableName|

		^OSCFunc ({|msg|
				{
					try {
					    var orbitIndex = msg[1];
				        var value      = msg[2];

				        orbits.at(orbitIndex).set(reverbVariableName, value.linlin(0,1,0,1.0));
					    orbitsElements[orbitIndex][\reverb][\element].value_(orbits.at(orbitIndex).get(reverbVariableName));

					} { |error| };

			}.defer;
	    }, ("/SuperDirtMixer/reverb"), recvPort: 57120).fix;
    }

	addTidalvstPresetListener { OSCFunc ({|msg|
				{
				    var fxName = msg[1];
				    var preset = msg[2];

					var combinedDictionary = Dictionary.new;
				    var keys;

				    combinedDictionary.putPairs(~tidalvst.instruments);
				    combinedDictionary.putPairs(~tidalvst.fxs);

				    keys = combinedDictionary.keys();

				    if (keys.includes(fxName), {
					    ~tidalvst.loadPreset(fxName, preset);
				    });
			}.defer;
	    }, ("/SuperDirtMixer/tidalvstPreset"), recvPort: 57120).fix;
	}

	addLoadPresetListener { |presetListView, reverbVariableName, presetFiles|
		OSCFunc ({|msg|
			{
				 var receivedPresetFile = msg[1];
				 var presetFilesAsSymbol = presetFiles.collect({|item| item.asSymbol});
				 var presetFile = receivedPresetFile;

				    loadPreset.value(receivedPresetFile);

				    presetListView.value = presetFilesAsSymbol.indexOf(receivedPresetFile.asSymbol);

			        orbits.do({|item|
		                fxs[\eq][\setEQuiValues].value(item, fxs[\eq][\equiView]);
	                    fxs[\eq][\equiView].target = item.globalEffects[0].synth;

				        orbitsElements[item.orbitIndex][\pan][\element].value_(item.get(\pan));
						orbitsElements[item.orbitIndex][\pan][\value].value_(item.get(\pan));
						orbitsElements[item.orbitIndex][\masterGain][\element].value_((item.get(\masterGain) + 1).explin(1,3, 0,1));
						orbitsElements[item.orbitIndex][\masterGain][\value].value_(item.get(\masterGain));
						orbitsElements[item.orbitIndex][\reverb][\element].value_(item.get(reverbVariableName));
                    });

			        fxs[\eq][\setEQuiValues].value(activeOrbit, fxs[\eq][\equiView]);
	                fxs[\eq][\equiView].target = activeOrbit.globalEffects[0].synth;
			}.defer;
	    }, ("/SuperDirtMixer/loadPreset"), recvPort: 57120).fix;
	}

	addMidiControlButtonListener { |midiControlButtons, switchControlButtonEvent |
		OSCFunc ({|msg|
				{
				    var midiControlButtonIndex = msg[1];

				    20.do({|item|
					    if (item == midiControlButtonIndex,
						     {midiControlButtons.at(item).states_([[item + 1, Color.white, Color.new255(238, 180, 34)]]) },
						     {midiControlButtons.at(item).states_([[item + 1, Color.black, Color.white]]) }
					    );
				    });

				    switchControlButtonEvent.value(midiControlButtonIndex);
			}.defer;
	    }, ("/SuperDirtMixer/midiControlButton"), recvPort: 57120).fix;
    }

}