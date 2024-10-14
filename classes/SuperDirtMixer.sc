/*
Optional TODOs
- [] Release everything when GUI is closed
StageMaster
- [x] Regler curve statt exp

- [] GR Anzeige des Compressor auf Summe
- [x] Limiter Level ist in dB
- [] peak dB Anzeige auf Master

Compressor

- [x] Regeler in zwei Gruppen (kreativ, und essenziell)
- [] HPF Label UI flakiness beheben

EQ

- [x] EQ Status (peak, filter etc.) Anzeige vergrößern

Preset Management

- [] Bessere und prominentere Dokumentation (für eigene Presets)
- [x] MrReason Presets löschen

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

			dirt.startSendRMS;

			dirt.orbits.do({
				|orbit, i|
				orbit.set(\label, "d" ++ (i+1))
			});

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

	/* GUI */
	gui {
		/* DECLARE GUI VARIABLES */
		var window, v;// local machine
		var masterUI = MasterUI.new(eventHandler);
		var mixerUI = MixerUI.new(eventHandler, dirt.orbits);
		var utilityUI =  UtilityUI.new(eventHandler, dirt.orbits, presetPath);
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
		eventHandler.emitEvent(\resetAll);
		eventHandler.emitEvent(\setActiveOrbit, dirt.orbits[0]);

		window.onClose_({
			eventHandler.emitEvent(\releaseAll);
			dirt.stopSendRMS;
		});
		window.front;
	}

}
