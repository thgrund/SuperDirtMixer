MixerUI {
   var <>activeOrbit;

   var <orbitLevelIndicators;

	*new { | orbitSize|
        ^super.new.init(orbitSize);
    }

    init { |orbitSize|
       orbitLevelIndicators = Array.new(orbitSize);
    }

   createMixerUIComponent { |text, orbit, guiElements, setOrbitEQValues, tidalNetAddr, reverbVariableName|

			var equiView = guiElements[\fxs][\eq][\equiView];
	     	var freqScope = guiElements[\fxs][\eq][\freqScope];
		    var setEQuiValues = guiElements[\fxs][\eq][\setEQuiValues];
			var orbitElements = guiElements[\orbits][orbit.orbitIndex];

			var panKnob = Knob().value_(orbit.get(\pan)).centered_(true).action_({|a|
				    orbit.set(\pan,a.value);
				    panNumBox.value_(a.value);
			    });

			var panNumBox = NumberBox()
			   .decimals_(2)
	            .clipLo_(0).clipHi_(1).align_(\center)
	            .scroll_step_(0.1).value_(orbit.get(\pan)).action_({|a|
		            panKnob.value_(a.value);
		            orbit.set(\pan,a.value);
	            });

	        var gainSlider = Slider.new.maxWidth_(30).value_(orbit.get(\masterGain).linexp(0, 2, 1,2) - 1).action_({|a|
			        orbit.set(\masterGain,a.value.linexp(0, 1.0, 1,3) - 1);
		            gainNumBox.value_(a.value.linexp(0, 1.0, 1,3)-1);
		        });

	        var gainNumBox = NumberBox()
	            .decimals_(2)
	            .clipLo_(0).clipHi_(2).align_(\center)
	            .scroll_step_(0.1).value_(orbit.get(\masterGain)).action_({|a|
		            gainSlider.value_(a.value.linexp(0, 2, 1,2.0) - 1);
		            orbit.set(\masterGain,a.value);
	            });

	        var eqButton = Button.new.string_("EQ").action_({ |a|
				    // Save EQ values before switching to the new eq orbit
				    setOrbitEQValues.value(this.activeOrbit, equiView);
			        this.activeOrbit = orbit;
                    setEQuiValues.value(orbit, equiView);
			        equiView.target = orbit.globalEffects[0].synth;
			        freqScope.inBus = orbit.dryBus;
				    guiElements[\orbits].do({arg item; item[\eq][\element].states_([["EQ", Color.black, Color.white]])});
		            a.states_([["EQ", Color.white, Color.new255(238, 180, 34)]]);
	            });

            var reverbKnob =  Knob().value_(orbit.get(reverbVariableName)).action_({|a|
					orbit.set(reverbVariableName,a.value);
				});

			var orbitLabelView = StaticText.new.string_(text).minWidth_(100).align_(\center);

			var newOrbitElements = Dictionary.newFrom([
				\orbitLabel,  Dictionary.newFrom([\element, orbitLabelView])
				, \pan, Dictionary.newFrom([\element, panKnob, \value, panNumBox])
				, \masterGain, Dictionary.newFrom([\element, gainSlider, \value, gainNumBox ])
				, \reverb, Dictionary.newFrom([\element, reverbKnob])
				, \eq, Dictionary.newFrom([\element, eqButton])
			]);

			guiElements[\orbits].add(newOrbitElements);

			this.orbitLevelIndicators.add(Array.fill(~dirt.numChannels, {LevelIndicator.new.maxWidth_(12).drawsPeak_(true).warning_(0.9).critical_(1.0)}));

			if (orbit == activeOrbit,
				{eqButton.states_([["EQ", Color.white, Color.new255(238, 180, 34)]])},
				{eqButton.states_([["EQ", Color.black, Color.white]])});

			^VLayout(
				orbitLabelView,
				panKnob,
				panNumBox,
				HLayout(
					this.orbitLevelIndicators[orbit.orbitIndex][0],
					this.orbitLevelIndicators[orbit.orbitIndex][1],
					gainSlider
				).spacing_(0),
				gainNumBox,
				StaticText.new.string_("FX - Reverb").minWidth_(100).align_(\center),
				reverbKnob,
				eqButton,
				HLayout(
					Button.new.maxWidth_(25)
					.states_([["M", Color.black, Color.white], ["M", Color.white, Color.blue]])
					.action_({
						|view|
						if(view.value == 0) { this.tidalNetAddr.sendMsg("/unmute",orbit.orbitIndex + 1) };
						if(view.value == 1) { this.tidalNetAddr.sendMsg("/mute",orbit.orbitIndex + 1) };
					}),
					Button.new.maxWidth_(25).states_([["S", Color.black, Color.white], ["S", Color.white, Color.red]]).action_({
						|view|
						if(view.value == 0) { this.tidalNetAddr.sendMsg("/unsolo",orbit.orbitIndex + 1) };
						if(view.value == 1) { this.tidalNetAddr.sendMsg("/solo",orbit.orbitIndex + 1) };
					}),
				),
			)
		}
}