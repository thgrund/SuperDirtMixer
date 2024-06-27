/*
   guiElements: {
        stageMaster: {
            live: {button},
            compThreshold: { title, knob, valueLabel },
            limiterLevel: { title, knob, valueLabel },
            highEndDB: { title, knob, valueLabel }
        },
        midiControlButtons: [
            button
        ],
        orbits: [
            {
               pan: {knob, numberBox}
               masterGain: {fader, numberBox}
               reverb: knob,
               mute: button,
               solo: button,
            }
        ],
        fxs: {
            eq: {
                equiView: equiView,
                freqScope: freqScope
            },
            compressor: {
                thresh: {title, fader, valueLabel},
                ratio : {title, knob, valueLabel},
                attack: {title, knob, valueLabel},
                release: {title, knob, valueLabel}
            }
        }
   }

*/

/*
Optional TODOs
- [] Dynamic amounts of level indicators per orbit based on the channel count
- [] UI shall not be shown if EQui is not installed
- [] Master EQ
- [] Consider to add the SoftKneeCompression UGen directly to the mixer: https://github.com/supercollider-quarks/wslib/blob/master/wslib-classes/Extensions/UGens/SoftKneeCompressor.sc

*/
SuperDirtMixer {
	var dirt;
	var <>presetPath = "../presets/";
	var <>tidalNetAddr;
	var <>orbitLabels;
	var <>prMasterBus;
	var <>switchControlButtonEvent;
	var <>setEQValueFunc;
	var <updateEQ;
	var reverbVariableName = \room;
	var reverbNativeSize = 0.95;
	var oscMasterLevelSender;
	var <>presetFiles;
	var <>presetFile;
	var defaultParentEvent, defaultParentEventKeys, eqDefaultEventKeys;
	var <>enabledCopyPasteFeature = false;

	*new { |dirt|
		^super.newCopyArgs(dirt).init
	}

	/* INITIALIZATION */

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

			eqDefaultEventKeys = Array.with(
				\loShelfFreq, \loShelfGain, \loShelfRs,
				\loPeakFreq, \loPeakGain, \loPeakRq,
				\midPeakFreq, \midPeakGain, \midPeakRq,
				\hiPeakFreq, \hiPeakGain, \hiPeakRq,
				\hiShelfFreq, \hiShelfGain, \hiShelfRs);

			presetFile = 'Default.json';

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

	/* PUBLIC FUNCTIONS */
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

	startEQEffect {
		dirt.set(\activeEq,1);
	}

	stopEQEffect {
		dirt.set(\activeEq,nil);
	}

	/* PRESET MANAGEMENT */
	loadPreset {
		var defaultEvents = JSONlib.convertToSC(File.readAllString((presetPath ++ this.presetFile).resolveRelative, "r"));

	    defaultEvents.do({
		     arg defaultEvent, index;
			 dirt.orbits[index].set(*defaultEvent.asPairs);
        });

	}

	savePreset {
		var orbitPresets = Array.new(dirt.orbits.size);
		var file = File((presetPath ++ this.presetFile).resolveRelative, "w");

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

	/* GUI */
	gui {
		/* DECLARE GUI VARIABLES */
		var window, v, equalizerComposite, freqScope, orbitUIElements, utilityElements, meterResp, masterOutResp, loadPresetListener;// local machine
		var setEQuiValues;
		var activeOrbit = dirt.orbits[0];
		var midiControlButtons = Array.new(20);
		var reshapedMidiControlButtons;
		var orbitMixerViews = Array.new((dirt.orbits.size * 2) - 1);
		var setOrbitEQValues;
		var leftMasterIndicator = LevelIndicator.new.maxWidth_(12).drawsPeak_(true).warning_(0.9).critical_(1.0);
	    var rightMasterIndicator = LevelIndicator.new.maxWidth_(12).drawsPeak_(true).warning_(0.9).critical_(1.0);
		var panListener, gainListener, reverbListener, midiControlButtonListener, tidalvstPresetListener;
		var presetListView = ListView(window,Rect(10,10,30,30))
		.items_(presetFiles)
        .action_({ arg sbs;
				this.presetFile = presetListView.items[sbs.value]; // .value returns the integer
		});
		var oscListener;
		var mixerUI = MixerUI.new(dirt.orbits.size);
		var utilityUI;

		var defaultFileIndex;

		var formaters = Dictionary.newFrom([
			\toDecibels, {|value| "%dB".format(value.ampdb.round(1e-1))},
			\toMilliseconds, {|value| "%ms".format(value.linlin(0,1,0,10).round(1e-2))},
			\toFreqdB, {|value| "%dB".format(value.linlin(0,1,-24,24).round(1e-2))}
		]);

		var uiKnobFactories = UIKnobFactories();

		var guiElements = Dictionary.new;

		var wrapperLoadPreset = {
			|presetFile|
			this.presetFile = presetFile;
			this.loadPreset;
		};

		guiElements.put(\stageMaster, Dictionary.new);

		guiElements.put(\orbits, Array.new(dirt.orbits.size));

		// Live Button
		guiElements.at(\stageMaster).put(\live, Dictionary.newFrom([\element, Button.new.string_("Live").action_({ |a| })]));
		uiKnobFactories.knobWithValueLabelFactory( guiElements.at(\stageMaster)
			, \compThreshold, "Comp Threshold", formaters[\toDecibels], 0.7);
		uiKnobFactories.knobWithValueLabelFactory( guiElements.at(\stageMaster)
			, \limiterLevel, "Limiter Level",  formaters[\toMilliseconds], 1.0);
		uiKnobFactories.knobWithValueLabelFactory( guiElements.at(\stageMaster)
			, \highEndDb, "High End dB",  formaters[\toFreqdB], 2/24 + 0.5);


		mixerUI.activeOrbit = activeOrbit;

		/* INIT GUI COMPONENTS */
		20.do({|item|
			midiControlButtons.add(Button.new.action_({ ~midiInternalOut.control(0, item + 100, 0)}).string_(item + 1).minHeight_(36).minWidth_(36));
		});

		reshapedMidiControlButtons = midiControlButtons.reshape(5,5);

		presetFiles.do({|item,i| if (item.asSymbol == this.presetFile, {defaultFileIndex = i})});

		presetListView.value = defaultFileIndex;

        dirt.startSendRMS;

		this.loadPreset();

		/* DEFINE EQ GUI */
		equalizerComposite = CompositeView.new;
		//composite.backColor = Color.rand;

		equalizerComposite.minHeight_(450);
		equalizerComposite.minWidth_(1200);

		freqScope = FreqScopeView(equalizerComposite, equalizerComposite.bounds);
		freqScope.freqMode = 1;
		freqScope.dbRange = 86;
		freqScope.active_(true); // turn it on the first time;
		freqScope.style_(1);
		freqScope.waveColors = [Color.new255(235, 235, 235)];
		freqScope.background = Color.new255(200, 200, 200);
		freqScope.fill = true;
        freqScope.inBus = dirt.orbits[0].dryBus;

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

		guiElements.put(\fxs, Dictionary.newFrom([
			\eq, Dictionary.newFrom([
				\equiView, EQui.new(equalizerComposite, equalizerComposite.bounds, dirt.orbits[0].globalEffects[0].synth)
				, \freqScope, freqScope
				, \setEQuiValues, setEQuiValues
			])
		]));

		updateEQ = {
			arg orbit;
			setEQValueFunc.value(this, activeOrbit, orbit, setEQuiValues, guiElements[\fxs][\eq][\equiView]);
		};

		setOrbitEQValues = {|orb, equiView|
			orb.set(*equiView.value.asArgsArray);
		};

		(0..(dirt.orbits.size - 1)).do({|item|
			setEQuiValues.value(dirt.orbits[item], guiElements[\fxs][\eq][\equiView]);
			guiElements[\fxs][\eq][\equiView].target = dirt.orbits[item].globalEffects[0].synth;
		});

		setEQuiValues.value(dirt.orbits[0], guiElements[\fxs][\eq][\equiView]);

		(0..(dirt.orbits.size - 1)).do({
			arg item;
			var baseIndex = item * 2;
			orbitMixerViews.insert(baseIndex,
				mixerUI.createMixerUIComponent(
					orbitLabels[item]
					, dirt.orbits[item]
					, guiElements
					, setOrbitEQValues
					, this.tidalNetAddr
					, reverbVariableName
			));
			if ( (item == (dirt.orbits.size - 1)).not, {orbitMixerViews.insert(baseIndex + 1, 15)});
		});

		utilityUI = UtilityUI.new(
			guiElements
			, dirt.orbits
			, activeOrbit
			, { this.loadPreset }
			, { this.savePreset }
			, this.initDefaultParentEvents
			, masterOutResp, presetListView, reverbVariableName);


// Create a window
window = Window.new("Mixer", Rect(0,0,300,1000), scroll: true);
window.layout_(
	 VLayout(
	   HLayout(
			*orbitMixerViews
	   ),
	   HLayout (
	        equalizerComposite,
			VLayout(
				StaticText.new.string_("MIDI Part Switch").minWidth_(100).maxHeight_(30).align_(\center),
				HLayout(*reshapedMidiControlButtons[0]),
				HLayout(*reshapedMidiControlButtons[1]),
				HLayout(*reshapedMidiControlButtons[2]),
				HLayout(*reshapedMidiControlButtons[3]),
				300
			),
			MasterUI.createMasterUIComponent(guiElements[\stageMaster], this.prMasterBus, leftMasterIndicator, rightMasterIndicator),
			utilityUI.utilityElements(window)
		)
		)
	  );

		this.startEQEffect;

		oscListener = SuperDirtMixerOSCListener.new(guiElements, dirt.orbits, activeOrbit, wrapperLoadPreset);

		/* OSC LISTENERS */
		oscListener.addMasterLevelOSCFunc(leftMasterIndicator, rightMasterIndicator);
		oscListener.addMeterResponseOSCFunc(mixerUI.orbitLevelIndicators);
		oscListener.addReverbListener(reverbVariableName);
		oscListener.addPanListener();
		oscListener.addGainListener();
		oscListener.addTidalvstPresetListener();
		oscListener.addLoadPresetListener(presetListView, reverbVariableName, this.presetFiles);
		oscListener.addMidiControlButtonListener(midiControlButtons, switchControlButtonEvent);

		window.onClose_({ dirt.stopSendRMS;  meterResp.free; loadPresetListener.free; });
		window.front;

		//JSONlib.convertToJSON(guiElements).postln;
	}

}

