/*
Optional TODOs
- [] Dynamic amounts of level indicators per orbit based on the channel count
- [] UI shall not be shown if EQui is not installed
- [] Master EQ

*/
SuperDirtMixer {
	var dirt;
	var <>presetPath = "../presets/";
	var <>tidalNetAddr;
	var <>orbitLabels;
	var <>prMasterBus;
	var <>receivePresetLoad;
	var reverbVariableName = \room;
	var reverbNativeSize = 0.95;
	var oscMasterLevelSender;
	var presetFiles;
	var eqDefaultParentEvent;
	var <orbitLabelViews;
	var defaultParentEvent, defaultParentEventKeys, eqDefaultEventKeys;

	*new { |dirt|
		^super.newCopyArgs(dirt).init
	}

	init {
		try {
			"---- initialize SuperDirtMixer ----".postln;
			tidalNetAddr = NetAddr.new("127.0.0.1", 6010);

			this.prLoadSynthDefs;

			dirt.server.sync;

			defaultParentEvent = Dictionary.new;

			this.prInitGlobalEffect;
			this.initDefaultParentEvents;

			orbitLabels = Array.fill(dirt.orbits.size, {arg i; "d" ++ (i+1); });
			orbitLabelViews = Array.new(dirt.orbits.size);

			eqDefaultEventKeys = Array.with(
				\loShelfFreq, \loShelfGain, \loShelfRs,
				\loPeakFreq, \loPeakGain, \loPeakRq,
				\midPeakFreq, \midPeakGain, \midPeakRq,
				\hiPeakFreq, \hiPeakGain, \hiPeakRq,
				\hiShelfFreq, \hiShelfGain, \hiShelfRs);

			this.prLoadPresetFiles;
			"SuperDirtMixer was successfully initialized".postln;
		}
	}

	prInitGlobalEffect {
		dirt.orbits.do { |x|
			x.globalEffects = x.globalEffects.addFirst(GlobalDirtEffect(\dirt_master_mix, [\masterGain, \gainControlLag]));
			x.globalEffects = x.globalEffects.addFirst(GlobalDirtEffect(\dirt_global_eq, [\activeEq]));
	        x.initNodeTree;
        };
	}

	initDefaultParentEvents {
		var defaultParentEventTemplate =[
			    \loShelfFreq, 100, \loShelfGain, 0, \loShelfRs, 1,
			    \loPeakFreq, 250, \loPeakGain, 0, \loPeakRq, 1,
			    \midPeakFreq, 1000, \midPeakGain, 0, \midPeakRq, 1,
			    \hiPeakFreq, 3500, \hiPeakGain, 0, \hiPeakRq, 1,
			    \hiShelfFreq, 6000, \hiShelfGain, 0, \hiShelfRs, 1,
			    reverbVariableName, 0.0
		];

		if (reverbVariableName == \room,
			{defaultParentEventTemplate.addAll([\size, reverbNativeSize])},
			{defaultParentEventTemplate.addAll([\size, nil])});

		dirt.set(defaultParentEventTemplate);

		defaultParentEvent.putPairs(defaultParentEventTemplate);
	}

	setReverbNativeSize { |size|
		if (reverbVariableName == \room, {
			dirt.set(\size, size);
		});
		reverbNativeSize = size;
	}

	setReverbVariableName { |variableName|
		dirt.orbits.do({arg item;
			if ((variableName == \room).not, {
				item.defaultParentEvent.put(\size, nil);
			}, {
				item.defaultParentEvent.put(\size, reverbNativeSize)
			});

			if ((variableName == reverbVariableName).not , {
				item.defaultParentEvent.put(variableName, item.defaultParentEvent.at(reverbVariableName));
				item.defaultParentEvent.put(reverbVariableName, nil);
			});
		});

		reverbVariableName = variableName;
	}


	prLoadSynthDefs { |path|
		var filePaths;
		path = path ?? { "../synths".resolveRelative };
		filePaths = pathMatch(standardizePath(path +/+ "*"));

		filePaths.do { |filepath|
		    if(filepath.splitext.last == "scd") {
			    (dirt:dirt).use { filepath.load }; "loading synthdefs in %\n".postf(filepath)
		    }
		}
	}

	prLoadPresetFiles { |path|
		var filePaths;
		path = path ?? { presetPath.resolveRelative };
		filePaths = pathMatch(standardizePath(path +/+ "*"));

		presetFiles = filePaths.collect { |filepath|
		   PathName.new(filepath).fileName;
		};
	}

	enableMasterPeakRMS { |masterBus|
		this.prMasterBus = masterBus;

		oscMasterLevelSender = {
			var sound = InFeedback.ar(this.prMasterBus, 2);
			SendPeakRMS.kr(sound, 10, 3, "/MixerMasterOutLevels")
		}.play;
	}

	disableMasterPeakRMS {
		this.prMasterBus = nil;
		oscMasterLevelSender.free;
	}

	loadPreset { |presetFile|
		var defaultEvents = JSONlib.convertToSC(File.readAllString((presetPath ++ presetFile).resolveRelative, "r"));

	    defaultEvents.do({
		     arg defaultEvent, index;
			 dirt.orbits[index].set(*defaultEvent.asPairs);
        });

	}

	savePreset { |presetFile|
		var orbitPresets = Array.new(dirt.orbits.size);
		var file = File((presetPath ++ presetFile).resolveRelative, "w");

		dirt.orbits.do({
	         arg orbit;
  			 var presetEvent = ();

			 eqDefaultEventKeys.do({|eventKey| presetEvent.put(eventKey, orbit.get(eventKey)) });

			 presetEvent.put(reverbVariableName, orbit.get(reverbVariableName));
			 if (reverbVariableName == \room, {
				presetEvent.put(\size, reverbNativeSize);
			 });

			 presetEvent.put(\masterGain, orbit.get(\masterGain));
			 presetEvent.put(\pan, orbit.get(\pan));

			 orbitPresets.add(presetEvent);
        });

		file.write(*JSONlib.convertToJSON(orbitPresets));
        file.close;
	}

	startEQEffect {
		dirt.set(\activeEq,1);
	}

	stopEQEffect {
		dirt.set(\activeEq,nil);
	}

	gui {
		var window, v, composite, freqScope, orbitUIElements, masterFunc, meterResp, masterOutResp, loadPresetListener;// local machine
		var equiView, setEQuiValues;
		var activeOrbit = dirt.orbits[0];
		var presetFile = 'Default.json';
		var orbitLevelIndicators = Array.new(dirt.orbits.size);
		var panKnobs = Array.new(dirt.orbits.size);
		var panNumBoxs = Array.new(dirt.orbits.size);
		var gainSliders = Array.new(dirt.orbits.size);
		var gainNumBoxs = Array.new(dirt.orbits.size);
        var reverbKnobs = Array.new(dirt.orbits.size);
		var eqButtons = List.new(dirt.orbits.size);
		var orbitMixerViews = Array.new((dirt.orbits.size * 2) - 1);
		var setOrbitEQValues;
		var leftMasterIndicator = LevelIndicator.new.maxWidth_(12).drawsPeak_(true).warning_(0.9).critical_(1.0);
	    var rightMasterIndicator = LevelIndicator.new.maxWidth_(12).drawsPeak_(true).warning_(0.9).critical_(1.0);
		var panListener, gainListener, reverbListener;
		var presetListView = ListView(window,Rect(10,10,30,30))
		.items_(presetFiles)
        .action_({ arg sbs;
				presetFile = presetListView.items[sbs.value] // .value returns the integer
		});

        dirt.startSendRMS;

		this.loadPreset(presetFile);

		composite = CompositeView.new;
		//composite.backColor = Color.rand;

		composite.minHeight_(450);
		composite.minWidth_(1400);

		freqScope = FreqScopeView(composite, composite.bounds);
		freqScope.freqMode = 1;
		freqScope.dbRange = 86;
		freqScope.active_(true); // turn it on the first time;
		freqScope.style_(1);
		freqScope.waveColors = [Color.new255(235, 235, 235)];
		freqScope.background = Color.new255(200, 200, 200);
		freqScope.fill = true;
        freqScope.inBus = dirt.orbits[0].dryBus;

		equiView = EQui.new(composite, composite.bounds, dirt.orbits[0].globalEffects[0].synth);

		setEQuiValues = {|orb, view|
			view.value = EQuiParams.new(
				loShelfFreq: orb.get(\loShelfFreq),
				loShelfGain: orb.get(\loShelfGain),
				loShelfRs: orb.get(\loShelfRs),
				loPeakFreq:  orb.get(\loPeakFreq),
				loPeakGain: orb.get(\loPeakGain),
				loPeakRq: orb.get(\loPeakRq),
				midPeakFreq: orb.get(\midPeakFreq),
				midPeakGain: orb.get(\midPeakGain),
				midPeakRq: orb.get(\midPeakRq),
				hiPeakFreq: orb.get(\hiPeakFreq),
				hiPeakGain: orb.get(\hiPeakGain),
				hiPeakRq: orb.get(\hiPeakRq),
				hiShelfFreq: orb.get(\hiShelfFreq),
				hiShelfGain: orb.get(\hiShelfGain),
				hiShelfRs: orb.get(\hiShelfRs)
			);
		};

		setOrbitEQValues = {|orb, equiView|
			orb.set(*equiView.value.asArgsArray);
		};

		(0..(dirt.orbits.size - 1)).do({|item|
			setEQuiValues.value(dirt.orbits[item], equiView);
			equiView.target = dirt.orbits[item].globalEffects[0].synth;
		});

		setEQuiValues.value(dirt.orbits[0], equiView);

		orbitUIElements = { |window, text, orbit|
			var panKnob = Knob().value_(orbit.get(\pan)).centered_(true).action_({|a|
				    orbit.set(\pan,a.value);
				    panNumBox.value_(a.value);
			    });

			var panNumBox = NumberBox()
			   .decimals_(2)
	            .clipLo_(0).clipHi_(1).align_(\center)
	            .scroll_step_(0.1).value_(orbit.get(\pan)).action_({|a|
		            panKnob.value_(a.value);
		            orbit.set(\pan,a.value);
	            });

	        var gainSlider = Slider.new.maxWidth_(30).value_(orbit.get(\masterGain).linlin(0, 2, 0,1)).action_({|a|
			        orbit.set(\masterGain,a.value.linlin(0, 1.0, 0,2));
		            gainNumBox.value_(a.value.linlin(0, 1.0, 0,2));
		        });

	        var gainNumBox = NumberBox()
	            .decimals_(2)
	            .clipLo_(0).clipHi_(2).align_(\center)
	            .scroll_step_(0.1).value_(orbit.get(\masterGain)).action_({|a|
		            gainSlider.value_(a.value.linlin(0, 2, 0,1.0));
		            orbit.set(\masterGain,a.value);
	            });

	        var eqButton = Button.new.string_("EQ").action_({ |a|
				    // Save EQ values before switching to the new eq orbit
				    setOrbitEQValues.value(activeOrbit, equiView);
			        activeOrbit = orbit;
                    setEQuiValues.value(orbit, equiView);
			        equiView.target = orbit.globalEffects[0].synth;
			        freqScope.inBus = orbit.dryBus;
		            eqButtons.do({arg item; item.states_([["EQ", Color.black, Color.white]])});
		            a.states_([["EQ", Color.white, Color.new255(238, 180, 34)]]);
	            });

            var reverbKnob =  Knob().value_(orbit.get(reverbVariableName)).action_({|a|
					orbit.set(reverbVariableName,a.value);
				});

			var orbitLabelView = StaticText.new.string_(text).minWidth_(100).align_(\center);

			panKnobs.add(panKnob);
		    panNumBoxs.add(panNumBox);
		    gainSliders.add(gainSlider);
		    gainNumBoxs.add(gainNumBox);
            reverbKnobs.add(reverbKnob);
			eqButtons.add(eqButton);
			orbitLabelViews.add(orbitLabelView);

			orbitLevelIndicators.add(Array.fill(~dirt.numChannels, {LevelIndicator.new.maxWidth_(12).drawsPeak_(true).warning_(0.9).critical_(1.0)}));

			if (orbit == activeOrbit,
				{eqButton.states_([["EQ", Color.white, Color.new255(238, 180, 34)]])},
				{eqButton.states_([["EQ", Color.black, Color.white]])});

			VLayout(
				orbitLabelView,
				panKnob,
				panNumBox,
				HLayout(
					orbitLevelIndicators[orbit.orbitIndex][0],
					orbitLevelIndicators[orbit.orbitIndex][1],
					gainSlider
				).spacing_(0),
				gainNumBox,
				StaticText.new.string_("FX - Reverb").minWidth_(100).align_(\center),
				reverbKnob,
				eqButton,
				HLayout(
					Button.new.maxWidth_(25)
					.states_([["M", Color.black, Color.white], ["M", Color.white, Color.blue]])
					.action_({
						|view|
						if(view.value == 0) { this.tidalNetAddr.sendMsg("/unmute",orbit.orbitIndex + 1) };
						if(view.value == 1) { this.tidalNetAddr.sendMsg("/mute",orbit.orbitIndex + 1) };
					}),
					Button.new.maxWidth_(25).states_([["S", Color.black, Color.white], ["S", Color.white, Color.red]]).action_({
						|view|
						if(view.value == 0) { this.tidalNetAddr.sendMsg("/unsolo",orbit.orbitIndex + 1) };
						if(view.value == 1) { this.tidalNetAddr.sendMsg("/solo",orbit.orbitIndex + 1) };
					}),
				),
			)
		};


    masterFunc = { |window|

        window.onClose_({ masterOutResp.free; }); // you must have this

	    VLayout(
			Button.new.string_("Reset Current EQ").action_({
				equiView.value = EQuiParams.new();
				equiView.target = activeOrbit.globalEffects[0].synth;
			}),
				Button.new.states_([["Mute All", Color.black, Color.white], ["Unmute All", Color.white, Color.blue]])
			    .action_({
				    |view|
				    if(view.value == 0) { this.tidalNetAddr.sendMsg("/unmuteAll") };
				    if(view.value == 1) { this.tidalNetAddr.sendMsg("/muteAll") };
			     }),
			20,
		presetListView,
		Button.new.string_("Save Preset")
				.action_({
					setOrbitEQValues.value(activeOrbit, equiView);
					this.savePreset(presetFile)
				}),
		Button.new.string_("Load Preset")
			    .action_({
				    |view|
			        this.loadPreset(presetFile);

					receivePresetLoad.value(presetFile);

			        dirt.orbits.do({|item|
		                setEQuiValues.value(item, equiView);
	                    equiView.target = item.globalEffects[0].synth;

						panKnobs[item.orbitIndex].value_(item.get(\pan));
						panNumBoxs[item.orbitIndex].value_(item.get(\pan));
						gainSliders[item.orbitIndex].value_((item.get(\masterGain)) /2);
						gainNumBoxs[item.orbitIndex].value_(item.get(\masterGain));
						reverbKnobs[item.orbitIndex].value_(item.get(reverbVariableName));
                    });

			        setEQuiValues.value(activeOrbit, equiView);
	                equiView.target = activeOrbit.globalEffects[0].synth;
			     }),
		20,
		Button.new.string_("Reset All")
			    .action_({
				    |view|
			        this.initDefaultParentEvents;

			        dirt.orbits.do({|item|
		                setEQuiValues.value(item, equiView);
	                    equiView.target = item.globalEffects[0].synth;

						panKnobs[item.orbitIndex].value_(0.5);
						panNumBoxs[item.orbitIndex].value_(0.5);
						gainSliders[item.orbitIndex].value_(1/2);
						gainNumBoxs[item.orbitIndex].value_(1.0);
						reverbKnobs[item.orbitIndex].value_(0.0);
                    });

			        setEQuiValues.value(activeOrbit, equiView);
	                equiView.target = activeOrbit.globalEffects[0].synth;
			    })
         )
    };

		(0..(dirt.orbits.size - 1)).do({
			arg item;
			var baseIndex = item * 2;
			orbitMixerViews.insert(baseIndex, orbitUIElements.value(window, orbitLabels[item], dirt.orbits[item]));
			if ( (item == (dirt.orbits.size - 1)).not, {orbitMixerViews.insert(baseIndex + 1, 15)});
		});

// Create a window
window = Window.new("Mixer", Rect(0,0,300,1000), scroll: true);
window.layout_(
	 VLayout(
	   HLayout(
			*orbitMixerViews
	   ),
	   HLayout (
	        composite,
			VLayout(
				if (this.prMasterBus.isNil.not, { StaticText.new.string_("Master").minWidth_(100).maxHeight_(30).align_(\center)}),
				if (this.prMasterBus.isNil.not, { HLayout(leftMasterIndicator,rightMasterIndicator).spacing_(0)}),
				200
			),
			masterFunc.value(window)
		)
		)
	  );

		this.startEQEffect;

		masterOutResp = OSCFunc( {|msg|
				{
					try {
				        var rmsL = msg[4].ampdb.linlin(-80, 0, 0, 1);
				        var peakL = msg[3].ampdb.linlin(-80, 0, 0, 1, \min);
                        var rmsR = msg[6].ampdb.linlin(-80, 0, 0, 1);
				        var peakR = msg[5].ampdb.linlin(-80, 0, 0, 1, \min);

				        leftMasterIndicator.value = rmsL;
				        leftMasterIndicator.peakLevel = peakL;
						rightMasterIndicator.value = rmsR;
				        rightMasterIndicator.peakLevel = peakR;

					} { |error|
						if(error.isKindOf(PrimitiveFailedError).not) { error.throw }
					};
				}.defer;
	    }, ("/MixerMasterOutLevels").asSymbol, ~dirt.server.addr).fix;

		meterResp = OSCFunc( {|msg|
				{
					try {
					    var indicators = orbitLevelIndicators[msg[2]];

                        (0..(~dirt.numChannels - 1)).do({
						    arg item;
						    var baseIndex = ((item + 1) * 2) + 1;
						    var rms = msg[baseIndex + 1].ampdb.linlin(-80, 0, 0, 1);
				            var peak = msg[baseIndex].ampdb.linlin(-80, 0, 0, 1, \min);

					        indicators[item].value = rms;
				            indicators[item].peakLevel = peak;
					    });

					} { |error| };
				}.defer;
			}, ("/rms")).fix;

		loadPresetListener = OSCFunc ({|msg|
				{
				    var receivedPresetFile = msg[1];
				    var presetFilesAsSymbol = presetFiles.collect({|item| item.asSymbol});
				    presetFile = receivedPresetFile;

				    this.loadPreset(receivedPresetFile);

				    presetListView.value = presetFilesAsSymbol.indexOf(receivedPresetFile.asSymbol);

					receivePresetLoad.value(receivedPresetFile);

			        dirt.orbits.do({|item|
		                setEQuiValues.value(item, equiView);
	                    equiView.target = item.globalEffects[0].synth;

						panKnobs[item.orbitIndex].value_(item.get(\pan));
						panNumBoxs[item.orbitIndex].value_(item.get(\pan));
						gainSliders[item.orbitIndex].value_((item.get(\masterGain)) /2);
						gainNumBoxs[item.orbitIndex].value_(item.get(\masterGain));
						reverbKnobs[item.orbitIndex].value_(item.get(reverbVariableName));
                    });

			        setEQuiValues.value(activeOrbit, equiView);
	                equiView.target = activeOrbit.globalEffects[0].synth;
			}.defer;
	    }, ("/SuperDirtMixer/loadPreset"), recvPort: 57120).fix;

		panListener = OSCFunc ({|msg|
				{
				    var orbitIndex = msg[1];
				    var value     = msg[2];

				    dirt.orbits.at(orbitIndex).set(\pan, value.linlin(0,1,0,1.0));
				    panKnobs[orbitIndex].value_(dirt.orbits.at(orbitIndex).get(\pan));
					panNumBoxs[orbitIndex].value_(dirt.orbits.at(orbitIndex).get(\pan));
			}.defer;
	    }, ("/SuperDirtMixer/pan"), recvPort: 57120).fix;

		gainListener = OSCFunc ({|msg|
				{
				    var orbitIndex = msg[1];
				    var value     = msg[2];

				    dirt.orbits.at(orbitIndex).set(\masterGain, value.linlin(0,2,0,2));
					gainSliders[orbitIndex].value_((dirt.orbits.at(orbitIndex).get(\masterGain)) /2);
					gainNumBoxs[orbitIndex].value_(dirt.orbits.at(orbitIndex).get(\masterGain));
			}.defer;
	    }, ("/SuperDirtMixer/masterGain"), recvPort: 57120).fix;

		reverbListener = OSCFunc ({|msg|
				{
				    var orbitIndex = msg[1];
				    var value     = msg[2];

				    dirt.orbits.at(orbitIndex).set(reverbVariableName, value.linlin(0,1,0,1.0));
					reverbKnobs[orbitIndex].value_(dirt.orbits.at(orbitIndex).get(reverbVariableName));

			}.defer;
	    }, ("/SuperDirtMixer/reverb"), recvPort: 57120).fix;

		window.onClose_({ dirt.stopSendRMS;  meterResp.free; loadPresetListener.free; });
		window.front;
	}

}

