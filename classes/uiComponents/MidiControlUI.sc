MidiControlUI {
	var midiControlButtons;
	var switchControlButtonEvent;

    *new { |initSwitchControlButtonEvent|
        ^super.new.init(initSwitchControlButtonEvent);
    }

	init { |initSwitchControlButtonEvent|
		midiControlButtons = Array.new(20);
		switchControlButtonEvent = initSwitchControlButtonEvent;

		this.addMidiControlButtonListener;
	}

	handleEvent { |eventName, eventData|
        ("ServiceC received % with data: %".format(eventName, eventData)).postln;
    }

	createUI {
		var reshapedMidiControlButtons;

		20.do({|item|
			midiControlButtons.add(Button.new.action_({ ~midiInternalOut.control(0, item + 100, 0)}).string_(item + 1).minHeight_(36).minWidth_(36));
		});

		reshapedMidiControlButtons = midiControlButtons.reshape(5,5);

		^VLayout(
			StaticText.new.string_("MIDI Part Switch").minWidth_(100).maxHeight_(30).align_(\center),
			HLayout(*reshapedMidiControlButtons[0]),
			HLayout(*reshapedMidiControlButtons[1]),
			HLayout(*reshapedMidiControlButtons[2]),
			HLayout(*reshapedMidiControlButtons[3]),
			300
		);
	}

	addMidiControlButtonListener {
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