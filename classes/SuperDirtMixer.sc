/*
Optional TODOs
- [] Dynamic amounts of level indicators per orbit based on the channel count
- [] UI shall not be shown if EQui is not installed
- [] Master EQ
- [] Consider to add the SoftKneeCompression UGen directly to the mixer: https://github.com/supercollider-quarks/wslib/blob/master/wslib-classes/Extensions/UGens/SoftKneeCompressor.sc
- [] Create new repo for EQUi (fork)
- [] Create new repo for CompUi (original)

*/
SuperDirtMixer {
	var dirt;
	var <>presetPath = "../../presets/";
	var <>prMasterBus;
	var <>switchControlButtonEvent;
	var <>setEQValueFunc;
	var reverbVariableName = \room;
	var reverbNativeSize = 0.95;
	var oscMasterLevelSender;
	var defaultParentEvent, defaultParentEventKeys, eqDefaultEventKeys;
	var <>enabledCopyPasteFeature = false;
	var eventHandler;

	*new { |dirt|
		^super.newCopyArgs(dirt).init
	}

	/* INITIALIZATION */

	init {
		try {
			var environment;

			"---- initialize SuperDirtMixer ----".postln;
			this.prLoadSynthDefs;

			dirt.server.sync;

			defaultParentEvent = Dictionary.new;

			eventHandler = EventHandler.new;

			this.prInitGlobalEffect;
			this.initDefaultParentEvents;

			dirt.orbits.do({
				|orbit, i|
				orbit.set(\label, "d" ++ (i+1))
			});

			eqDefaultEventKeys = Array.with(
				\loShelfFreq, \loShelfGain, \loShelfRs,
				\loPeakFreq, \loPeakGain, \loPeakRq,
				\midPeakFreq, \midPeakGain, \midPeakRq,
				\hiPeakFreq, \hiPeakGain, \hiPeakRq,
				\hiShelfFreq, \hiShelfGain, \hiShelfRs);

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

	setOrbitLabels {
		|labels|

		labels.do({
			|label, index|
			dirt.orbits[index].set(\label, label);
		});
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

	/* GUI */
	gui {
		/* DECLARE GUI VARIABLES */
		var window, v, meterResp;// local machine
		var activeOrbit = dirt.orbits[0];
		var setOrbitEQValues;
		var oscListener;
		var mixerUI = MixerUI.new(eventHandler, dirt.orbits);
		var masterUI = MasterUI.new(eventHandler);
		var utilityUI =  UtilityUI.new( eventHandler, dirt.orbits, this.initDefaultParentEvents, presetPath, reverbVariableName, reverbNativeSize);
		var equalizerUI = EqualizerUI.new(eventHandler, dirt.orbits);
		var midiControlUI = MidiControlUI.new(switchControlButtonEvent);

		var defaultFileIndex;

		var formaters = Dictionary.newFrom([
			\toDecibels, {|value| "%dB".format(value.ampdb.round(1e-1))},
			\toMilliseconds, {|value| "%ms".format(value.linlin(0,1,0,10).round(1e-2))},
			\toFreqdB, {|value| "%dB".format(value.linlin(0,1,-24,24).round(1e-2))}
		]);

		var uiKnobFactories = UIKnobFactories();

		var guiElements = Dictionary.new;

		mixerUI.reverbVariableName = reverbVariableName;

		guiElements.put(\stageMaster, Dictionary.new);

		// Live Button
		guiElements.at(\stageMaster).put(\live, Dictionary.newFrom([\element, Button.new.string_("Live").action_({ |a| })]));
		uiKnobFactories.knobWithValueLabelFactory( guiElements.at(\stageMaster)
			, \compThreshold, "Comp Threshold", formaters[\toDecibels], 0.7);
		uiKnobFactories.knobWithValueLabelFactory( guiElements.at(\stageMaster)
			, \limiterLevel, "Limiter Level",  formaters[\toMilliseconds], 1.0);
		uiKnobFactories.knobWithValueLabelFactory( guiElements.at(\stageMaster)
			, \highEndDb, "High End dB",  formaters[\toFreqdB], 2/24 + 0.5);

		/* INIT GUI COMPONENTS */


        dirt.startSendRMS;


// Create a window
window = Window.new("Mixer", Rect(0,0,300,1000), scroll: true);
window.layout_(
	 VLayout(
	   mixerUI.createUI,
	   HLayout (
	        equalizerUI.equalizerComposite,
			midiControlUI.createUI,
			masterUI.createUI(guiElements[\stageMaster], this.prMasterBus),
			utilityUI.createUI
		)
		)
	  );

		window.onClose_({ dirt.stopSendRMS;  meterResp.free});
		window.front;
	}

}

