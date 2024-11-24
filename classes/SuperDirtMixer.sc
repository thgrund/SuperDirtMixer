/*

StageMaster
- [] GR Anzeige des Compressor auf Summe
- [] peak dB Anzeige auf Master

*/
SuperDirtMixer {
	var dirt;
	var <>presetPath = "../../presets/";
	var <>prMasterBus;
	var <>switchControlButtonEvent;
	var >midiInternalOut;
	var reverbVariableName = \room;
	var reverbNativeSize = 0.95;
	var oscMasterLevelSender;
	var defaultParentEvent, defaultParentEventKeys;
	var freeAction, addRemoteControlAction;

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

			dirt.startSendRMS;

			dirt.orbits.do({
				|orbit, i|
				orbit.set(\label, "d" ++ (i+1))
			});

			// Additional app state
			currentEnvironment.put(\SuperDirtMixer
				, Dictionary[
					\wasRemoteControlAdded -> false,
					\wasStageMasterSynthCreated -> false,
				]
			);

			"SuperDirtMixer was successfully initialized".postln;
		}
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

	setMasterBus { |masterBus|
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

	free {
		freeAction.value();
	}

	addRemoteControl {
		addRemoteControlAction.value();
	}


	/* GUI */
	gui {
		/* DECLARE GUI VARIABLES */
		var window, v;// local machine
		var eventHandler = EventHandler.new;
		var masterUI = MasterUI.new(eventHandler);
		var mixerUI = MixerUI.new(eventHandler, dirt.orbits);
		var utilityUI =  UtilityUI.new(eventHandler, dirt.orbits, presetPath, defaultParentEvent);
		var equalizerUI = EqualizerUI.new(eventHandler, dirt.orbits);
		var midiControlUI = MidiControlUI.new(switchControlButtonEvent, midiInternalOut);
		var compressorUI = CompressorUI.new(eventHandler, dirt.orbits);

		mixerUI.reverbVariableName = reverbVariableName;
		mixerUI.reverbNativeSize = reverbNativeSize;

		/* INIT GUI COMPONENTS */

		// Create a window
		window = Window.new("Mixer", Rect(0,0,300,1000), scroll: true);
		window.background_(Color.grey(0.8));
		window.layout_(
			VLayout(
				mixerUI.createUI,
				30,
				HLayout (
					equalizerUI.createdUI,
					20,
					compressorUI.createUI,
					20,
					midiControlUI.createUI,
					20,
					masterUI.createUI(this.prMasterBus),
					20,
					utilityUI.createUI
				)
			)
		);


		//eventHandler.printEventNames;
		eventHandler.emitEvent(\setActiveOrbit, dirt.orbits[0]);

		freeAction = {
			eventHandler.emitEvent(\releaseAll);
			dirt.stopSendRMS;
		};

		addRemoteControlAction = {
			eventHandler.emitEvent(\addRemoteControl);
		};

		if (currentEnvironment[\SuperDirtMixer][\wasRemoteControlAdded] == false, {
			eventHandler.emitEvent(\addRemoteControl);
			currentEnvironment[\SuperDirtMixer].put(\wasRemoteControlAdded, true);
		});

		window.onClose_({
			eventHandler.emitEvent(\destroy);
			//eventHandler.emitEvent(\releaseAll);
			//dirt.stopSendRMS;
		});
		window.front;
	}

}
