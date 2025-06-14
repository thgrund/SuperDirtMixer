EqualizerUI : UIFactories{

	var handler;
	var orbits;
	var controlBusses;
	var container;
	var eqView, freqScope;
	var activeOrbit;
	var defaultParentEvent;
	var bypassButton;
	var equalizerElements;
	var globalEffects;
	var loadedOrbitsPreset;
	var controlBusHandlers;

    *new { |initHandler, initOrbits, initControlBusses, initContainer|
        ^super.new.init(initHandler, initOrbits, initControlBusses, initContainer);
    }

	init { |initHandler, initOrbits, initControlBusses, initContainer|
		globalEffects = GlobalEffects.new;

		handler = initHandler;
		orbits = initOrbits;
		controlBusses = initControlBusses;

		defaultParentEvent = [
		    \hiPassFreq , 100, \hiPassGain , 0, \hiPassRq , 1.4, \hiPassBypass , 1, \loShelfFreq , 150, \loShelfGain, 0, \loShelfRs, 1, \loShelfBypass, 0, \loPeakFreq , 250, \loPeakGain , 0, \loPeakRq , 1, \loPeakBypass , 0, \midPeakFreq , 1000, \midPeakGain , 0, \midPeakRq , 1, \midPeakBypass , 0, \hiPeakFreq , 3500, \hiPeakGain , 0, \hiPeakRq , 1, \hiPeakBypass , 0, \hiShelfFreq , 6000, \hiShelfGain , 0, \hiShelfRs , 1, \hiShelfBypass , 0, \loPassFreq , 8000, \loPassGain , 0, \loPassRq , 1.4, \loPassBypass, 1, \activeEq, 1
	    ];

		if (handler.isNil.not, {
			handler.subscribe(this, \setActiveOrbit);
			handler.subscribe(this, \updateActiveOrbit);
			handler.subscribe(this, \updateUI);
			handler.subscribe(this, \resetAll);
			handler.subscribe(this, \releaseAll);
			handler.subscribe(this, \addRemoteControl);
			handler.subscribe(this, \destroy);
			handler.subscribe(this, \presetLoaded);

			handler.emitEvent(\extendDefaultParentEvent, defaultParentEvent);
		});

		equalizerElements = Dictionary.new;
        controlBusHandlers = Dictionary.new;

		bypassButton = Button.new.string_("Bypass").maxWidth_(75);

		bypassButton.action_({
			this.setBypassButtonState(bypassButton, true, activeOrbit, \activeEq);

			if (activeOrbit.get(\activeEq) == 1, {
				this.updateGlobalEffect(activeOrbit);
				this.enableButtons(true);
				this.setEQuiValues(activeOrbit);
			}, {
				this.setOrbitEQValues(activeOrbit);
				this.disableUI();
				this.updateGlobalEffect(activeOrbit);
			});
		});


		if (orbits.isNil.not, {
			this.prInitGlobalEffect;

			this.createUI(initContainer);

			orbits.do({|orbit|
				this.setEQuiValues(orbit);
			});

			activeOrbit = orbits[0];


			this.setOrbits(defaultParentEvent);
			this.setEQuiValues(activeOrbit);

			loadedOrbitsPreset = Array.fill(orbits.size, {defaultParentEvent.asEvent})
		});
	}

	handleEvent { |eventName, eventData|
		if (eventName == \setActiveOrbit, {
			// Save EQ values before switching to the new eq orbit
			if (activeOrbit.isNil.not, {
				this.setOrbitEQValues(activeOrbit);
			});

			activeOrbit = eventData;

			this.setEQuiValues(activeOrbit);
			this.setBypassButtonState(bypassButton, false, activeOrbit, \activeEq);

			if (activeOrbit.get(\activeEq) == 1, {
				this.enableButtons(true);
			}, {
				this.disableUI();
			});

			equalizerElements.keysValuesDo({
				|key, value|
				this.setEmptyButtonState(value[\element], false, activeOrbit, key);
			});

			freqScope.inBus = activeOrbit.dryBus;
		});

		if (eventName == \updateUI, {

			orbits.do({|orbit|
				this.updateGlobalEffect(orbit);
				this.setEQuiValues(orbit);
			});

			this.setEQuiValues(activeOrbit);
			this.setBypassButtonState(bypassButton, false, activeOrbit, \activeEq);

			if (activeOrbit.get(\activeEq) == 1, {
				this.enableButtons(true);
			}, {
				this.disableUI();
			});

			equalizerElements.keysValuesDo({
				|key, value|
				this.setEmptyButtonState(value[\element], false, activeOrbit, key);
			});
		});

		if (eventName == \updateActiveOrbit, {
			this.setOrbitEQValues(activeOrbit);
		});

		if (eventName == \resetAll, {
			this.setOrbits(defaultParentEvent);

			orbits.do({|orbit|
				this.updateGlobalEffect(orbit);
				this.setEQuiValues(orbit);
			});

			equalizerElements.keysValuesDo({
				|key, value|
				this.setEmptyButtonState(value[\element], false, activeOrbit, key);
			});
		});

		if (eventName == \releaseAll, {
			orbits.do({|orbit| globalEffects.releaseGlobalEffect(orbit, \dirt_global_eq)});
			freqScope.kill;
		});

		if (eventName == \addRemoteControl, {
			this.addRemoteControlListener;
		});

		if (eventName == \destroy, {
			freqScope.kill;
		});

		if (eventName == \presetLoaded, {
			eventData.do({
				|value, index|
				var eventTemplate = ();

				defaultParentEvent.keysValuesDo({|defaultKey, defaultValue|
					if (value[defaultKey].isNil, {
						eventTemplate[defaultKey] = defaultValue;
					}, {
						eventTemplate[defaultKey] = value[defaultKey];
					})
				});

				loadedOrbitsPreset.put(index, eventTemplate);
			});
		});

    }

	prInitGlobalEffect {
		orbits.do { |orbit|
			globalEffects.addGlobalEffect(orbit, GlobalDirtEffect(\dirt_global_eq, [\activeEq]))
		};
	}

	searchForEffectSynth { | orbit |
		var effect = orbit.globalEffects.detect({| effect | effect.name.asSymbol == \dirt_global_eq.asSymbol; });
		^effect;
	}

	updateGlobalEffect { |orbit|
		var effect = this.searchForEffectSynth(orbit);

		if(orbit.get(\activeEq) == 1, {
			effect.active_(true);
		}, {
			effect.active_(false);
		});
	}

	startEQEffect {
		this.setOrbits([\activeEq,1]);
	}

	stopEQEffect {
		this.setOrbits([\activeEq,nil]);
	}

	setEQuiValues {|orbit|
		var effect = this.searchForEffectSynth(orbit);
			eqView.value = EQuiParams.new(
				hiPassFreq: orbit.get(\hiPassFreq),
				hiPassRq: orbit.get(\hiPassRq),
				hiPassBypass: orbit.get(\hiPassBypass),
				loShelfFreq: orbit.get(\loShelfFreq),
				loShelfGain: orbit.get(\loShelfGain),
				loShelfRs: orbit.get(\loShelfRs),
				loShelfBypass: orbit.get(\loShelfBypass),
				loPeakFreq:  orbit.get(\loPeakFreq),
				loPeakGain: orbit.get(\loPeakGain),
				loPeakRq: orbit.get(\loPeakRq),
				loPeakBypass: orbit.get(\loPeakBypass),
				midPeakFreq: orbit.get(\midPeakFreq),
				midPeakGain: orbit.get(\midPeakGain),
				midPeakRq: orbit.get(\midPeakRq),
				midPeakBypass: orbit.get(\midPeakBypass),
				hiPeakFreq: orbit.get(\hiPeakFreq),
				hiPeakGain: orbit.get(\hiPeakGain),
				hiPeakRq: orbit.get(\hiPeakRq),
			    hiPeakBypass: orbit.get(\hiPeakBypass),
				hiShelfFreq: orbit.get(\hiShelfFreq),
				hiShelfGain: orbit.get(\hiShelfGain),
				hiShelfRs: orbit.get(\hiShelfRs),
				hiShelfBypass: orbit.get(\hiShelfBypass),
				loPassFreq: orbit.get(\loPassFreq),
				loPassRq: orbit.get(\loPassRq),
				loPassBypass: orbit.get(\loPassBypass),
			);


		if (effect.isNil.not, {eqView.target = effect.synth});
	}

	disableUI {
		var p = eqView.value;
		var effect = this.searchForEffectSynth(activeOrbit);

		p.hiPassBypass = 1;
		p.loShelfBypass = 1;
		p.loPeakBypass = 1;
		p.midPeakBypass = 1;
		p.hiPeakBypass = 1;
		p.hiShelfBypass = 1;
		p.loPassBypass = 1;

		eqView.valueAction = p; // reset to params stored in p, and update target

		if (effect.isNil.not, {eqView.target = effect.synth});

		this.enableButtons(false);
	}

	enableButtons {
		|enable|
		equalizerElements.do({|buttonMap|
			buttonMap[\element].enabled = enable
		});
	}

	setOrbitEQValues {|orb|
		var bypassValues = Array.with(\hiPassBypass, orb.get(\hiPassBypass)
			, \loShelfBypass, orb.get(\loShelfBypass)
			, \loPeakBypass, orb.get(\loPeakBypass)
			, \midPeakBypass, orb.get(\midPeakBypass)
			, \hiPeakBypass, orb.get(\hiPeakBypass)
			, \hiShelfBypass, orb.get(\hiShelfBypass)
			, \loPassBypass, orb.get(\loPassBypass)
		);

		orb.set(*eqView.value.asArgsArray);
		orb.set(*bypassValues.asPairs);
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

		/* DEFINE EQ GUI */

		var equalizerComposite = CompositeView(container, Rect(5, 5, 700,  container.bounds.height - 15));
	    var userView = UserView(equalizerComposite).minHeight_(equalizerComposite.bounds.height-80);

		equalizerComposite.background_(Color.gray(0.85));

		this.setBypassButtonState(bypassButton, false, activeOrbit, \activeEq);

		this.eqFilterButtonFactory(\hiPassBypass, "../../assets/images/highPass.svg".resolveRelative);
		this.eqFilterButtonFactory(\loShelfBypass, "../../assets/images/lowShelf.svg".resolveRelative);
		this.eqFilterButtonFactory(\loPeakBypass, "../../assets/images/peak.svg".resolveRelative);
		this.eqFilterButtonFactory(\midPeakBypass, "../../assets/images/peak.svg".resolveRelative);
		this.eqFilterButtonFactory(\hiPeakBypass, "../../assets/images/peak.svg".resolveRelative);
		this.eqFilterButtonFactory(\hiShelfBypass, "../../assets/images/highShelf.svg".resolveRelative );
		this.eqFilterButtonFactory(\loPassBypass, "../../assets/images/lowPass.svg".resolveRelative);

		equalizerComposite.layout = VLayout(
		    HLayout(
			    bypassButton,
			    StaticText.new.string_("Equalizer").fixedHeight_(15),
		   )
		   , userView
		   , HLayout(
				[equalizerElements[\hiPassBypass][\element]],
				[equalizerElements[\loShelfBypass][\element]],
				[equalizerElements[\loPeakBypass][\element]],
				[equalizerElements[\midPeakBypass][\element]],
				[equalizerElements[\hiPeakBypass][\element]],
				[equalizerElements[\hiShelfBypass][\element]],
				[equalizerElements[\loPassBypass][\element]],
			);
	    );

		freqScope = FreqScopeView(userView, Rect(0,0, userView.bounds.width, userView.bounds.height));
		freqScope.freqMode = 1;
		freqScope.dbRange = 86;
		freqScope.active_(true); // turn it on the first time;
		freqScope.style_(1);
		freqScope.waveColors = [Color.new255(235, 235, 235)];
		freqScope.background = Color.new255(255, 255, 255);
		freqScope.fill = true;
        freqScope.inBus = orbits[0].dryBus;

		eqView = EQui.new(userView, Rect(0,0, userView.bounds.width, userView.bounds.height),
			this.searchForEffectSynth(orbits[0]).synth, uiCallback: {
				|select, freq, gain, rs|
				var bands = #[ \hiPass, \loShelf, \loPeak, \midPeak, \hiPeak, \hiShelf,\loPass];
				var freqSymbol = (bands[select] ++ "Freq").asSymbol;
				var gainSymbol = (bands[select] ++ "Gain").asSymbol;
				var suffix = if(Set[0,2,3,4,6].includes(select), {"Rq"}, {"Rs"});
				var rsSymbol = (bands[select] ++ suffix).asSymbol;

				loadedOrbitsPreset[activeOrbit.orbitIndex].put(freqSymbol, freq);
				activeOrbit.set(freqSymbol, freq);

				loadedOrbitsPreset[activeOrbit.orbitIndex].put(gainSymbol, gain);
				activeOrbit.set(gainSymbol, gain);

				loadedOrbitsPreset[activeOrbit.orbitIndex].put(rsSymbol, rs);
				activeOrbit.set(rsSymbol, rs);

		});
	}

	updateEQ {
		arg orbit;

		this.setEQuiValues(orbit);

		if ((activeOrbit.orbitIndex == orbit.orbitIndex).not, {
			this.setEQuiValues(activeOrbit);
		});

	}

	eqFilterButtonFactory {
		|property, icon|
			equalizerElements.put(property, Dictionary.newFrom([\element,
			(Button()
				.icon_(Image.openSVG(icon, 50@50))
				.action_({
					this.setOrbitEQValues(activeOrbit);
					this.setEmptyButtonState(equalizerElements[property][\element], true, activeOrbit, property);
					this.setEQuiValues(activeOrbit);
					loadedOrbitsPreset[activeOrbit.orbitIndex].put(property, activeOrbit.get(property));
				});
			)
		    ]));
			this.setEmptyButtonState(equalizerElements[property][\element], false, activeOrbit, property);
	}

	stopControlBusTask {
		|orbitIndex, param, reset|

		if (controlBusHandlers[orbitIndex].notNil, {
			if (controlBusHandlers[orbitIndex][param].notNil, {
				controlBusHandlers[orbitIndex][param].linkDown.stop;
				controlBusHandlers[orbitIndex].removeAt(param);
			});
		})
	}

	createControlBusHandler {
		|orbitIndex, param, delta, busId, shallUIBeUpdated |
		var task, resetTask;

		if (controlBusHandlers[orbitIndex].isNil, {
			controlBusHandlers.put(orbitIndex, Dictionary.new);
		});


			task = Task {

				//0.05.wait;

				loop {
					controlBusses[busId].get({
						|value |

						if (orbits.at(orbitIndex).get(param) != value, {

							orbits.at(orbitIndex).set(param, value);

							if (shallUIBeUpdated == 1.0, {
								{
									this.updateEQ(orbits.at(orbitIndex));
								}.defer;
							});
						});
					});
					0.001.wait;
				};
			};

			resetTask = Task {
				(delta + 0.1).wait;
				this.stopControlBusTask(orbitIndex, param, true);
			};

			if (controlBusHandlers[orbitIndex][param].isNil, {
				controlBusHandlers[orbitIndex].put(param, Pair(task, resetTask));
				task.start;
			}, {
				controlBusHandlers[orbitIndex][param].linkAcross.stop;
			});

			controlBusHandlers[orbitIndex][param].linkAcross.play(doReset: true);
	}

	addRemoteControlListener { OSCFunc ({|msg, time| {
		var event = ();
		var superDirtOSC = NetAddr("127.0.0.1", 57120);
		var orbitIndex;
		var eqParams = defaultParentEvent.asDict.keys;

		event.putPairs(msg[1..]);

		orbitIndex = event.at(\orbit);

		if (orbits.at(orbitIndex).get(\activeEq) == 1, {

			eqParams.do({
				arg item;

				if (event.at(item.asSymbol).notNil, {

					if (event.at(item.asSymbol).asString.beginsWith("c").not, {
						this.stopControlBusTask(orbitIndex, item.asSymbol, false);
						orbits.at(orbitIndex).set(item.asSymbol, event.at(item.asSymbol));
					}, {
						var busId = event.at(item.asSymbol).asString.split($c)[1].asInteger;
						var shallUIBeUpdated = event.at(\shallUIBeUpdated);

						this.createControlBusHandler(orbitIndex, item.asSymbol, event.at(\delta), busId, shallUIBeUpdated);
					});
				}, {
					orbits.at(orbitIndex).set(item.asSymbol, loadedOrbitsPreset[orbitIndex].at(item.asSymbol));
					this.stopControlBusTask(orbitIndex, item.asSymbol, false);
				});
			});

			if (activeOrbit.isNil.not && event.at(\shallUIBeUpdated) == 1.0, {
				this.updateEQ(orbits.at(orbitIndex));
			});

			equalizerElements.keysValuesDo({
				|key, value|
				this.setEmptyButtonState(value[\element], false, activeOrbit, key);
			});
		});
	}.defer;
	}, ("/dirt/play"), recvPort: 57120).fix;
	}
}
