MidiControlUI {
	var midiControlButtons;
	var switchControlButtonEvent;
	var midiOut;

    *new { |initSwitchControlButtonEvent, initMidiOut|
        ^super.new.init(initSwitchControlButtonEvent, initMidiOut);
    }

	init { |initSwitchControlButtonEvent, initMidiOut|
		midiControlButtons = Array.new(20);
		switchControlButtonEvent = initSwitchControlButtonEvent;
		midiOut = initMidiOut;

		this.addMidiControlButtonListener;
	}

	handleEvent { |eventName, eventData|
        ("ServiceC received % with data: %".format(eventName, eventData)).postln;
    }

	createUI {
		| container |
		var reshapedMidiControlButtons;

		16.do({|item|
			var button = Button.new
				.action_({
					midiOut.control(0, item + 100, 0);
				})
				.string_(item + 1).minHeight_(36).minWidth_(36);

			button.states_([[item + 1, Color.black, Color.gray(0.9)]]);

			midiControlButtons.add(button);
	    });

		reshapedMidiControlButtons = midiControlButtons.reshape(8,2);

		if (midiOut.isNil.not,{
			container.layout = VLayout(
				StaticText.new.string_("MIDI Part Switch").fixedHeight_(15).align_(\center),
				HLayout(*reshapedMidiControlButtons[0]),
				HLayout(*reshapedMidiControlButtons[1]),
				HLayout(*reshapedMidiControlButtons[2]),
				HLayout(*reshapedMidiControlButtons[3]),
				HLayout(*reshapedMidiControlButtons[4]),
				HLayout(*reshapedMidiControlButtons[5]),
				HLayout(*reshapedMidiControlButtons[6]),
				HLayout(*reshapedMidiControlButtons[7]),
				CompositeView().minHeight_(120)
			);
		}, {
			^VLayout()
		});
	}

	addMidiControlButtonListener {
		OSCFunc ({|msg|
				{
				    var midiControlButtonIndex = msg[1];

				    16.do({|item|
					    if (item == midiControlButtonIndex,
						     {midiControlButtons.at(item).states_([[item + 1, Color.white, Color.new255(238, 180, 34)]]) },
						     {midiControlButtons.at(item).states_([[item + 1, Color.black, Color.gray(0.9)]]) }
					    );
				    });

				    switchControlButtonEvent.value(midiControlButtonIndex);
			}.defer;
	    }, ("/SuperDirtMixer/midiControlButton"), recvPort: 57120).fix;
    }
}