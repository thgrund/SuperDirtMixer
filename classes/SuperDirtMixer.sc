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
		var v;// local machine
		var window = Window("SuperDirtMixer", Rect(100, 100, 2000, 1000), scroll: true);
		var eventHandler = EventHandler.new;
		var mixerContainer = ScrollView(window, Rect(10, 10, 380, 500)).background_(Color.gray(0.7));
		var fxContainer = ScrollView(window, Rect(10, 310, 450, 500)).background_(Color.gray(0.7));
		var settingsContainer = CompositeView(window, Rect(0, 0, 135, 500)).background_(Color.gray(0.85));
		var midiContainer = CompositeView(window, Rect(0, 0, 135, 500)).background_(Color.gray(0.85));
		var masterContainer = CompositeView(window, Rect(0, 0, 135, 500)).background_(Color.gray(0.85));
		var masterUI = MasterUI.new(eventHandler);
		var mixerUI = MixerUI.new(eventHandler, dirt.orbits);
		var utilityUI =  UtilityUI.new(eventHandler, dirt.orbits, presetPath, defaultParentEvent);
		var equalizerUI = EqualizerUI.new(eventHandler, dirt.orbits, fxContainer);
		var midiControlUI = MidiControlUI.new(switchControlButtonEvent, midiInternalOut);
		var compressorUI = CompressorUI.new(eventHandler, dirt.orbits);


		mixerUI.reverbVariableName = reverbVariableName;
		mixerUI.reverbNativeSize = reverbNativeSize;

		/* INIT GUI COMPONENTS */

		mixerUI.createUI(mixerContainer);
		utilityUI.createUI(settingsContainer);
		masterUI.createUI(masterContainer,this.prMasterBus);
		midiControlUI.createUI(midiContainer);
		compressorUI.createUI(fxContainer);

		window.drawFunc = {
			mixerContainer.resizeTo(window.bounds.width - 20, window.bounds.height - fxContainer.bounds.height - 20);

			fxContainer.resizeTo(
				window.bounds.width - 20 - settingsContainer.bounds.width
				- midiContainer.bounds.width - masterContainer.bounds.width, fxContainer.bounds.height
			);
			fxContainer.moveTo(10, window.bounds.height - fxContainer.bounds.height);

			settingsContainer.moveTo(window.bounds.width - settingsContainer.bounds.width - 10, window.bounds.height - fxContainer.bounds.height);
			midiContainer.moveTo(
				window.bounds.width - settingsContainer.bounds.width - midiContainer.bounds.width
				- masterContainer.bounds.width - 10, window.bounds.height - fxContainer.bounds.height
			);
			masterContainer.moveTo(
				window.bounds.width - settingsContainer.bounds.width - masterContainer.bounds.width - 10,
				window.bounds.height - fxContainer.bounds.height);
        };

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
