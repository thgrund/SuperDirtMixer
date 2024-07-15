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
	var reverbVariableName = \room;
	var reverbNativeSize = 0.95;
	var oscMasterLevelSender;
	var defaultParentEvent, defaultParentEventKeys;
	var eventHandler;

	*new { |dirt|
		^super.newCopyArgs(dirt).init
	}

	/* INITIALIZATION */

	init {
		try {
			"---- initialize SuperDirtMixer ----".postln;
			this.prLoadSynthDefs;

			dirt.server.sync;

			defaultParentEvent = Dictionary.new;

			eventHandler = EventHandler.new;

			this.prInitGlobalEffect;

			dirt.orbits.do({
				|orbit, i|
				orbit.set(\label, "d" ++ (i+1))
			});

			"SuperDirtMixer was successfully initialized".postln;
		}
	}

	prInitGlobalEffect {
		dirt.orbits.do { |x|
			x.globalEffects = x.globalEffects.addFirst(GlobalDirtEffect(\dirt_global_eq, [\activeEq]));
			x.globalEffects = x.globalEffects.addFirst(GlobalDirtEffect(\dirt_master_mix, [\masterGain, \gainControlLag]));
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
		var window, v;// local machine
		var masterUI = MasterUI.new(eventHandler);
		var mixerUI = MixerUI.new(eventHandler, dirt.orbits);
		var utilityUI =  UtilityUI.new(eventHandler, dirt.orbits, presetPath);
		var equalizerUI = EqualizerUI.new(eventHandler, dirt.orbits);
		var midiControlUI = MidiControlUI.new(switchControlButtonEvent);

		mixerUI.reverbVariableName = reverbVariableName;
		mixerUI.reverbNativeSize = reverbNativeSize;

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
					masterUI.createUI(this.prMasterBus),
					utilityUI.createUI
				)
			)
		);

		//eventHandler.printEventNames;

		window.onClose_({ dirt.stopSendRMS });
		window.front;
	}

}
