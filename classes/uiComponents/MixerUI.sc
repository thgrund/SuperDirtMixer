MixerUI : UIFactories {
    var activeOrbit;
    var handler;
    var <orbitLevelIndicators;
    var >reverbVariableName, >reverbNativeSize;
    var orbits;
    var guiElements;
    var tidalNetAddr;
	var defaultParentEvent;

	var panListener;

	*new { | initHandler, initOrbits|
        ^super.new.init(initHandler, initOrbits);
    }

    init { |initHandler, initOrbits|
		handler = initHandler;
		orbits = initOrbits;
		orbitLevelIndicators = Array.new(orbits.size);
		tidalNetAddr = NetAddr.new("127.0.0.1", 6010);
		guiElements = Array.new(orbits.size);
		reverbNativeSize = 0;

		if (orbits.isNil.not, {
			var globalEffects = GlobalEffects.new;

			activeOrbit = orbits[0];

			orbits.do { |orbit|
				globalEffects.addGlobalEffect(orbit, GlobalDirtEffect(\dirt_master_mix, [\masterGain, \gainControlLag]), true);
			};
		});

		reverbVariableName = \room;

		if (handler.isNil.not, {
			handler.subscribe(this, \resetAll);
			handler.subscribe(this, \updateUI);
			handler.subscribe(this, \releaseAll);
			handler.subscribe(this, \addRemoteControl);
		});

    }

	handleEvent { |eventName, eventData|
		if (eventName == \updateUI, {
			orbits.do({|item|
				guiElements[item.orbitIndex][\orbitLabel][\element].string_(item.get(\label));
				guiElements[item.orbitIndex][\contextMenuLabel][\element].string_(item.get(\label));
				guiElements[item.orbitIndex][\pan][\element].value_(item.get(\pan));
				guiElements[item.orbitIndex][\pan][\value].value_(item.get(\pan));
				guiElements[item.orbitIndex][\masterGain][\element].value_((item.get(\masterGain) + 1).curvelin(1,3, 0,1, curve: 3));
				guiElements[item.orbitIndex][\masterGain][\value].value_(item.get(\masterGain));
				guiElements[item.orbitIndex][\reverb][\element].value_(item.get(reverbVariableName));
				guiElements[item.orbitIndex][\orbitWrapper][\element].background = (Color.fromHexString(item.get(\color)));
				guiElements[item.orbitIndex][\colorPicker][\element].setColorFromHexString(item.get(\color));

			});
		};
		);

		if (eventName == \resetAll, {
			orbits.do({|item|

				guiElements[item.orbitIndex][\orbitLabel][\element].string_("d" ++ (item.orbitIndex + 1));
				guiElements[item.orbitIndex][\contextMenuLabel][\element].string_("d" ++ (item.orbitIndex + 1));
				guiElements[item.orbitIndex][\pan][\element].value_(0.5);
				guiElements[item.orbitIndex][\pan][\value].value_(0.5);
				guiElements[item.orbitIndex][\masterGain][\element].value_(2.curvelin(1,3,0,1, curve: 3));
				guiElements[item.orbitIndex][\masterGain][\value].value_(1.0);
				guiElements[item.orbitIndex][\reverb][\element].value_(0.0);
				guiElements[item.orbitIndex][\orbitWrapper][\element].background = Color.fromHexString("#D9D9D9");
				guiElements[item.orbitIndex][\colorPicker][\element].setColorFromHexString("#D9D9D9");

				item.set(\pan, 0.5);
				item.set(\masterGain, 1.0);
				item.set(\color, "#D9D9D9");
				item.set(\label, "d" ++ (item.orbitIndex + 1));
				item.set(reverbVariableName.asSymbol, 0.0);
            });
		});

		if (eventName == \releaseAll, {
			var globalEffects = GlobalEffects.new;
			orbits.do({|orbit| globalEffects.releaseGlobalEffect(orbit, \dirt_master_mix )});
		});

		if (eventName == \addRemoteControl, {
			this.addRemoteControlListener;
			this.addMeterResponseOSCFunc;
		});
    }

	setOrbits { |pairs|
		pairs.pairsDo { |key, val|
			orbits.do({
				| orbit |
				if (orbit.defaultParentEvent[key].isNil, {
					orbit.set(key, val);
				});
			})
		};
	}

    createUI {
		| container |
		var orbitMixerViews = Array.new((orbits.size * 2) - 1);

		defaultParentEvent = [
			\pan, 0.5, \masterGain, 1.0, reverbVariableName.asSymbol, 0.0, \color, "#D9D9D9", \label, ""
	    ];

		this.setOrbits(defaultParentEvent);

		if (reverbVariableName.asSymbol == \room, {
			defaultParentEvent.add(\size).add(reverbNativeSize)
		});

		handler.emitEvent(\extendDefaultParentEvent, defaultParentEvent);

		(0..(orbits.size - 1)).do({
			arg item;
			var baseIndex = item * 2;

			orbitMixerViews.insert(baseIndex,
				this.createMixerUIComponent(
					orbits[item]
					, container
					, reverbVariableName
			));
			if ( (item == (orbits.size - 1)).not, {orbitMixerViews.insert(baseIndex + 1, 15)});
		});

        container.onResize = {
    	    container.children.do({
		        |compositeView|
			    compositeView.resizeTo(125, (container.bounds.height - 15).min(460));
	        });
	    };
    }

    createMixerUIComponent { |orbit, container, reverbVariableName|
		    var composite = CompositeView.new(container, Rect((orbit.orbitIndex * 130) + 5, 5, 100, 440));
		    var colorPicker = ColorPickerUI.new({|color|
			    composite.background = color;
			    orbit.defaultParentEvent.put(\color, color.hexString);
		    });
		    var contextMenuLabel;
		    var text = orbit.get(\label);
			var orbitElements = guiElements[orbit.orbitIndex];

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

		    var gainSlider = Slider.new.fixedWidth_(30).value_((orbit.get(\masterGain) + 1).curvelin(1,3,0,1, curve: 3)).action_({|a|
			        orbit.set(\masterGain,a.value.lincurve(0, 1.0, 1,3, curve: 3) - 1);
		            gainNumBox.value_(a.value.lincurve(0, 1.0, 1,3, curve: 3)-1);
		        });

	        var gainNumBox = NumberBox()
	            .decimals_(2)
	            .clipLo_(0).clipHi_(2).align_(\center)
	            .scroll_step_(0.1).value_(orbit.get(\masterGain)).action_({|a|
			        gainSlider.value_((a.value + 1).curvelin(1, 3, 0, 1, curve: 3));
		            orbit.set(\masterGain,a.value);
	            });

	        var eqButton = Button.new.string_("FX").action_({ |a|
				    // Save EQ values before switching to the new eq orbit
			        handler.emitEvent(\setActiveOrbit, orbit);
			        activeOrbit = orbit;
				    guiElements.do(
				       {arg item; item[\eq][\element].states_([["FX", Color.black, Color.gray(0.9)]])});
		            a.states_([["FX", Color.white, Color.new255(238, 180, 34)]]);
	            });

            var reverbKnob =  Knob().value_(orbit.get(reverbVariableName)).action_({|a|
					orbit.set(reverbVariableName,a.value);
				});

			var orbitLabelView = StaticText.new.string_(text).minWidth_(100).align_(\center);

		    var contextMenuLabelView = TextView.new.string_(orbit.get(\label)).fixedHeight_(30);

			var newOrbitElements = Dictionary.newFrom([
				\orbitLabel,  Dictionary.newFrom([\element, orbitLabelView])
			    , \contextMenuLabel, Dictionary.newFrom([\element, contextMenuLabelView])
				, \pan, Dictionary.newFrom([\element, panKnob, \value, panNumBox])
				, \masterGain, Dictionary.newFrom([\element, gainSlider, \value, gainNumBox ])
				, \reverb, Dictionary.newFrom([\element, reverbKnob])
				, \eq, Dictionary.newFrom([\element, eqButton])
			    , \orbitWrapper, Dictionary.newFrom([\element, composite])
			    , \colorPicker, Dictionary.newFrom([\element, colorPicker])
			]);

		    guiElements.add(newOrbitElements);

			this.orbitLevelIndicators.add(Array.fill(~dirt.numChannels, {LevelIndicator.new.fixedWidth_(12).drawsPeak_(true).warning_(0.9).critical_(1.0)}));

		    if (orbit == activeOrbit,
				{eqButton.states_([["FX", Color.white, Color.new255(238, 180, 34)]])},
				{eqButton.states_([["FX", Color.black, Color.gray(0.9)]])});

			composite.layout_(VLayout(
				orbitLabelView,
				panKnob,
				panNumBox,
				HLayout(
					this.orbitLevelIndicators[orbit.orbitIndex][0],
					this.orbitLevelIndicators[orbit.orbitIndex][1],
					gainSlider
				).spacing_(0),
				gainNumBox,
				StaticText.new.string_("FX - Reverb").align_(\center),
				reverbKnob,
				eqButton,
				HLayout(
					Button.new
					.states_([["M", Color.black, Color.gray(0.9)], ["M", Color.white, Color.blue]])
					.action_({
						|view|
						if(view.value == 0) { tidalNetAddr.sendMsg("/unmute",orbit.orbitIndex + 1) };
						if(view.value == 1) { tidalNetAddr.sendMsg("/mute",orbit.orbitIndex + 1) };
					}),
					Button.new.states_([["S", Color.black, Color.gray(0.9)], ["S", Color.white, Color.red]]).action_({
						|view|
						if(view.value == 0) { tidalNetAddr.sendMsg("/unsolo",orbit.orbitIndex + 1) };
						if(view.value == 1) { tidalNetAddr.sendMsg("/solo",orbit.orbitIndex + 1) };
					}),
				    ),
			    )
		    ).background_(Color.gray(0.85));

		    contextMenuLabelView.keyUpAction =  {|tv|
			    orbitLabelView.string_(tv.string);
			    orbit.set(\label, tv.string);
		    };

		    composite.setContextMenuActions(
		        CustomViewAction(contextMenuLabelView),
			    CustomViewAction(colorPicker.createUI().minHeight_(300).minWidth_(300))
		    );

		}

	/*
	 * ADD OSC LISTENERS
	 */
	addMeterResponseOSCFunc {
		OSCFunc({ |msg|
				{
					try {
					    var indicators = orbitLevelIndicators[msg[2]];

                        (0..(~dirt.numChannels - 1)).do({ |item|
						    var baseIndex = ((item + 1) * 2) + 1;
						    var rms = msg[baseIndex + 1].ampdb.linlin(-80, 0, 0, 1);
				            var peak = msg[baseIndex].ampdb.linlin(-80, 0, 0, 1, \min);

					        indicators[item].value = rms;
				            indicators[item].peakLevel = peak;
					    });

					} { |error| };
				}.defer;
			}, ("/rms")).fix;
	}

	addRemoteControlListener { OSCFunc ({|msg| {
		var event = ();
		var orbitIndex;

		event.putPairs(msg[1..]);

		orbitIndex = event.at(\orbit);

		if ((orbits.at(orbitIndex).isNil).not, {
			if (event.at(reverbVariableName).isNil.not, {
				orbits.at(orbitIndex).set(reverbVariableName, event.at(reverbVariableName).linlin(0,1,0,1.0));
				guiElements[orbitIndex][\reverb][\element].value_(orbits.at(orbitIndex).get(reverbVariableName));
			});

			if (event.at(\pan).isNil.not, {
				orbits.at(orbitIndex).set(\pan, event.at(\pan).linlin(0,1,0,1.0));
				guiElements[orbitIndex][\pan][\element].value_(orbits.at(orbitIndex).get(\pan));
			    guiElements[orbitIndex][\pan][\value].value_(orbits.at(orbitIndex).get(\pan));
			});

			if (event.at(\masterGain).isNil.not, {
			    orbits.at(orbitIndex).set(\masterGain, event.at(\masterGain).linlin(0,2,0,2));
				guiElements[orbitIndex][\masterGain][\element].value_((orbits.at(orbitIndex).get(\masterGain) + 1).curvelin(1,3, 0,1, curve: 3));
				guiElements[orbitIndex][\masterGain][\value].value_(orbits.at(orbitIndex).get(\masterGain));
			});

			if (event.at(\label).isNil.not, {
			    orbits.at(orbitIndex).set(\label, event.at(\label));
				guiElements[orbitIndex][\orbitLabel][\element].string_(orbits.at(orbitIndex).get(\label));
				guiElements[orbitIndex][\contextMenuLabel][\element].string_(orbits.at(orbitIndex).get(\label));
			});

			if (event.at(\color).isNil.not, {
			    orbits.at(orbitIndex).set(\color, event.at(\color));
				guiElements[orbitIndex][\colorPicker][\element].setColorFromHexString(event.at(\color).asString);
				guiElements[orbitIndex][\orbitWrapper][\element].background = event.at(\color).asString;
			});


		});

	}.defer;
	}, ("/SuperDirtMixer"), recvPort: 57121).fix;
	}

}