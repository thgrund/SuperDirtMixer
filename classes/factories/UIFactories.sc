UIFactories {
	classvar <>formaters; // Define a class variable with getter and setter

	*new {
        formaters = Dictionary.newFrom([
			\toDecibels, {|value| "%dB".format(value.ampdb.linlin(-40, 0, -60,0).round(1e-1)) },
            \toMilliseconds, {|value| "%ms".format(value.linlin(0,1,0,10).round(1e-2))},
            \toFreqdB, {|value| "%dB".format(value.linlin(0,1,-24,24).round(1e-2))}
        ]);

		^super.new.init;
    }

	setBypassButtonState {
		| bypassButton, shouldToggle, activeOrbit, property |

		if (shouldToggle == true, {
			if (activeOrbit.get(property) == 1, {
				bypassButton.states_([["Bypass", Color.white, Color.new255(238, 180, 34)]]);
				activeOrbit.set(property, 0);

			}, {
				bypassButton.states_([["Bypass", Color.black, Color.white]]);
				activeOrbit.set(property, 1);
			});
		}, {
			if (activeOrbit.get(property) == 1, {
				bypassButton.states_([["Bypass", Color.black, Color.white]]);
			}, {
				bypassButton.states_([["Bypass", Color.white, Color.new255(238, 180, 34)]]);
			});
		});
	}


}