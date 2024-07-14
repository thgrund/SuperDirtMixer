SuperDirtMixerOSCListener {
	var orbitsElements, orbits, fxs, activeOrbit;
	var loadPreset;

	*new { | initGuiElements, initOrbits, initActiveOrbit|
        ^super.new.init(initGuiElements, initOrbits, initActiveOrbit)
    }

    init { |initGuiElements, initOrbits, initActiveOrbit|
        orbitsElements = initGuiElements[\orbits];
		orbits = initOrbits;
		fxs = initGuiElements[\fxs];
		activeOrbit = initActiveOrbit;
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