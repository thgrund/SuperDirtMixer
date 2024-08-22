EqualizerUI : UIFactories{

	var handler;
	var orbits;
	var eqView, freqScope;
	var activeOrbit;
	var defaultParentEvent;
	var bypassButton;
	var equalizerElements;
	var globalEffects;

    *new { |initHandler, initOrbits|
        ^super.new.init(initHandler, initOrbits);
    }

	init { |initHandler, initOrbits|
		globalEffects = GlobalEffects.new;

		handler = initHandler;
		orbits = initOrbits;

		defaultParentEvent = [
		    \hiPassFreq , 100, \hiPassGain , 0, \hiPassRq , 1.4, \hiPassBypass , 1, \loShelfFreq , 150, \loShelfGain, 0, \loShelfRs, 1, \loShelfBypass, 0, \loPeakFreq , 250, \loPeakGain , 0, \loPeakRq , 1, \loPeakBypass , 0, \midPeakFreq , 1000, \midPeakGain , 0, \midPeakRq , 1, \midPeakBypass , 0, \hiPeakFreq , 3500, \hiPeakGain , 0, \hiPeakRq , 1, \hiPeakBypass , 0, \hiShelfFreq , 6000, \hiShelfGain , 0, \hiShelfRs , 1, \hiShelfBypass , 0, \loPassFreq , 8000, \loPassGain , 0, \loPassRq , 1.4, \loPassBypass, 1, \activeEq, 1
	    ];

		if (handler.isNil.not, {
			handler.subscribe(this, \setActiveOrbit);
			handler.subscribe(this, \updateActiveOrbit);
			handler.subscribe(this, \updateUI);
			handler.subscribe(this, \resetAll);

			handler.emitEvent(\extendDefaultParentEvent, defaultParentEvent);
		});

		equalizerElements = Dictionary.new;

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

			this.createUI();

			orbits.do({|orbit|
				this.setEQuiValues(orbit);
			});

			activeOrbit = orbits[0];


			this.setOrbits(defaultParentEvent);
			this.setEQuiValues(activeOrbit);
		});


		this.addRemoteControlListener;
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
				this.setEQuiValues(orbit);
			});

			equalizerElements.keysValuesDo({
				|key, value|
				this.setEmptyButtonState(value[\element], false, activeOrbit, key);
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
		if(orbit.get(\activeEq) == 1, {
			globalEffects.addGlobalEffect(
				orbit, GlobalDirtEffect(\dirt_global_eq, [\activeEq]));
		}, {
			globalEffects.releaseGlobalEffect(orbit, \dirt_global_eq);
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
		orbits.do(_.set(*pairs))
	}

	createUI {
		var height = 450;
		var width = 760;

		/* DEFINE EQ GUI */
		var equalizerComposite = CompositeView.new;

		equalizerComposite.minHeight_(height);
		equalizerComposite.minWidth_(width);

		freqScope = FreqScopeView(equalizerComposite, equalizerComposite.bounds);
		freqScope.freqMode = 1;
		freqScope.dbRange = 86;
		freqScope.active_(true); // turn it on the first time;
		freqScope.style_(1);
		freqScope.waveColors = [Color.new255(235, 235, 235)];
		freqScope.background = Color.new255(200, 200, 200);
		freqScope.fill = true;
        freqScope.inBus = orbits[0].dryBus;

		eqView = EQui.new(equalizerComposite, equalizerComposite.bounds, this.searchForEffectSynth(orbits[0]).synth);

		this.setBypassButtonState(bypassButton, false, activeOrbit, \activeEq);

		this.eqFilterButtonFactory(\hiPassBypass, "../../assets/images/highPass.svg".resolveRelative);
		this.eqFilterButtonFactory(\loShelfBypass, "../../assets/images/lowShelf.svg".resolveRelative);
		this.eqFilterButtonFactory(\loPeakBypass, "../../assets/images/peak.svg".resolveRelative);
		this.eqFilterButtonFactory(\midPeakBypass, "../../assets/images/peak.svg".resolveRelative);
		this.eqFilterButtonFactory(\hiPeakBypass, "../../assets/images/peak.svg".resolveRelative);
		this.eqFilterButtonFactory(\hiShelfBypass, "../../assets/images/highShelf.svg".resolveRelative );
		this.eqFilterButtonFactory(\loPassBypass, "../../assets/images/lowPass.svg".resolveRelative);


		^VLayout(
		    HLayout(
			    bypassButton,
			    StaticText.new.string_("Equalizer").fixedHeight_(15),
		   )
		   , equalizerComposite
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
				})
			)
		    ]));
			this.setEmptyButtonState(equalizerElements[property][\element], false, activeOrbit, property);
	}


	addRemoteControlListener { OSCFunc ({|msg| {
		var event = ();
		var superDirtOSC = NetAddr("127.0.0.1", 57120);
		var orbitIndex;
		var eqParams = defaultParentEvent.asDict.keys;

		event.putPairs(msg[1..]);

		orbitIndex = event.at(\orbit);

		if (orbits.at(orbitIndex).get(\activeEq) == 1, {

			eqParams.do({
				arg item;

				if (event.at(item.asSymbol).isNil.not, {
					orbits.at(orbitIndex).set(item.asSymbol, event.at(item.asSymbol));

					if (activeOrbit.isNil.not, {
						this.updateEQ(orbits.at(orbitIndex));
					})
				});
			});

			equalizerElements.keysValuesDo({
				|key, value|
				this.setEmptyButtonState(value[\element], false, activeOrbit, key);
			});
		});
	}.defer;
	}, ("/SuperDirtMixer"), recvPort: 57121).fix;
	}


}