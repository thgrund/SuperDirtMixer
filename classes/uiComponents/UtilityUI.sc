UtilityUI {
	var orbitsElements, orbits, fxs, activeOrbit, guiElements, defaultParentEvents;
	var loadPreset, savePreset;
	var equiView, setEQuiValues;
	var masterOutResp, presetListView, reverbVariableName;

	*new { | initGuiElements, initOrbits, initActiveOrbit, initLoadPreset, initSavePreset, initDefaultParentEvents, initMasterOutResp, initPresetListView, initReverbVariableName|
        ^super.new.init(initGuiElements, initOrbits, initActiveOrbit, initLoadPreset, initSavePreset, initDefaultParentEvents, initMasterOutResp, initPresetListView, initReverbVariableName)
    }

    init { |initGuiElements, initOrbits, initActiveOrbit, initLoadPreset, initSavePreset, initDefaultParentEvents, initMasterOutResp, initPresetListView, initReverbVariableName|
		guiElements = initGuiElements;
        orbitsElements = initGuiElements[\orbits];
		orbits = initOrbits;
		equiView = initGuiElements[\fxs][\eq][\equiView];
		setEQuiValues = initGuiElements[\fxs][\eq][\setEQuiValues];

		activeOrbit = initActiveOrbit;
		loadPreset = initLoadPreset;
		savePreset = initSavePreset;
		defaultParentEvents = initDefaultParentEvents;
		masterOutResp = initMasterOutResp;
		presetListView = initPresetListView;
		reverbVariableName = initReverbVariableName;
	}

	/* DEFINE PRESET UI */
    utilityElements { |window|
		var equiView = guiElements[\fxs][\eq][\equiView];

        window.onClose_({ masterOutResp.free; }); // you must have this

	    ^VLayout(
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
				    activeOrbit.set(*equiView.value.asArgsArray);
				    savePreset.value()
				}),
		Button.new.string_("Load Preset")
			    .action_({
				    |view|
				    loadPreset.value();

			        orbits.do({|item|
		                setEQuiValues.value(item, equiView);
	                    equiView.target = item.globalEffects[0].synth;

						guiElements[\orbits][item.orbitIndex][\pan][\element].value_(item.get(\pan));
						guiElements[\orbits][item.orbitIndex][\pan][\value].value_(item.get(\pan));
						guiElements[\orbits][item.orbitIndex][\masterGain][\element].value_((item.get(\masterGain) + 1).explin(1,3, 0,1));
						guiElements[\orbits][item.orbitIndex][\masterGain][\value].value_(item.get(\masterGain));
						guiElements[\orbits][item.orbitIndex][\reverb][\element].value_(item.get(reverbVariableName));
                    });

			        setEQuiValues.value(activeOrbit, equiView);
	                equiView.target = activeOrbit.globalEffects[0].synth;
			     }),
		20,
		Button.new.string_("Reset All")
			    .action_({
				    |view|
			        this.defaultParentEvents;

			        orbits.do({|item|
		                setEQuiValues.value(item, equiView);
						equiView.target = item.globalEffects[0].synth;

						guiElements[\orbits][item.orbitIndex][\pan][\element].value_(0.5);
						guiElements[\orbits][item.orbitIndex][\pan][\value].value_(0.5);
						guiElements[\orbits][item.orbitIndex][\masterGain][\element].value_(1/2);
						guiElements[\orbits][item.orbitIndex][\masterGain][\value].value_(1.0);
						guiElements[\orbits][item.orbitIndex][\reverb][\element].value_(0.0);
                    });

			        setEQuiValues.value(activeOrbit, equiView);
	                equiView.target = activeOrbit.globalEffects[0].synth;
			    })
         )
    }
}