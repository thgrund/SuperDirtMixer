EqualizerUI : UIFactories{

	var handler;
	var orbits;
	var eqView, freqScope;
	var activeOrbit;
	var defaultParentEvent;
	var bypassButton;
	var globalEffects;

    *new { |initHandler, initOrbits|
        ^super.new.init(initHandler, initOrbits);
    }

	init { |initHandler, initOrbits|
		globalEffects = GlobalEffects.new;

		handler = initHandler;
		orbits = initOrbits;

		defaultParentEvent = [
		    \loShelfFreq, 100, \loShelfGain, 0, \loShelfRs, 1,
			\loPeakFreq, 250, \loPeakGain, 0, \loPeakRq, 1,
			\midPeakFreq, 1000, \midPeakGain, 0, \midPeakRq, 1,
			\hiPeakFreq, 3500, \hiPeakGain, 0, \hiPeakRq, 1,
			\hiShelfFreq, 6000, \hiShelfGain, 0, \hiShelfRs, 1,
			\activeEq, 1
	    ];

		if (handler.isNil.not, {
			handler.subscribe(this, \setActiveOrbit);
			handler.subscribe(this, \updateActiveOrbit);
			handler.subscribe(this, \updateUI);

			handler.emitEvent(\extendDefaultParentEvent, defaultParentEvent);
		});

		bypassButton = Button.new.string_("Bypass").maxWidth_(75);

		bypassButton.action_({
			this.setOrbitEQValues(activeOrbit);
			this.setBypassButtonState(bypassButton, true, activeOrbit, \activeEq);
			this.updateGlobalEffect(activeOrbit);
			this.setEQuiValues(activeOrbit);
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
			freqScope.inBus = activeOrbit.dryBus;
		});

		if (eventName == \updateUI, {
			orbits.do({|orbit|
				this.updateGlobalEffect(orbit);
				this.setEQuiValues(orbit);
			});

			this.setEQuiValues(activeOrbit);
			this.setBypassButtonState(bypassButton, false, activeOrbit, \activeEq);
		});

		if (eventName == \updateActiveOrbit, {
			this.setOrbitEQValues(activeOrbit);
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
				loShelfFreq: orbit.get(\loShelfFreq),
				loShelfGain: orbit.get(\loShelfGain),
				loShelfRs: orbit.get(\loShelfRs),
				loPeakFreq:  orbit.get(\loPeakFreq),
				loPeakGain: orbit.get(\loPeakGain),
				loPeakRq: orbit.get(\loPeakRq),
				midPeakFreq: orbit.get(\midPeakFreq),
				midPeakGain: orbit.get(\midPeakGain),
				midPeakRq: orbit.get(\midPeakRq),
				hiPeakFreq: orbit.get(\hiPeakFreq),
				hiPeakGain: orbit.get(\hiPeakGain),
				hiPeakRq: orbit.get(\hiPeakRq),
				hiShelfFreq: orbit.get(\hiShelfFreq),
				hiShelfGain: orbit.get(\hiShelfGain),
				hiShelfRs: orbit.get(\hiShelfRs)
			);

		if (effect.isNil.not, {eqView.target = effect.synth});
	}

	setOrbitEQValues {|orb|
		orb.set(*eqView.value.asArgsArray);
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

		^VLayout(
		    HLayout(
			    bypassButton,
			    StaticText.new.string_("Equalizer").fixedHeight_(15)
		   )
		   , equalizerComposite

	    );
	}

	updateEQ {
		arg orbit;

		this.setEQuiValues(orbit);

		if ((activeOrbit.orbitIndex == orbit.orbitIndex).not, {
			this.setEQuiValues(activeOrbit);
		});

	}


	addRemoteControlListener { OSCFunc ({|msg| {
		var event = ();
		var superDirtOSC = NetAddr("127.0.0.1", 57120);
		var orbitIndex;
		var eqParams = defaultParentEvent.keys;

		event.putPairs(msg[1..]);

		orbitIndex = event.at(\orbit);

		eqParams.do({
			arg item;

			if (event.at(item.asSymbol).isNil.not, {
				orbits.at(orbitIndex).defaultParentEvent.put(item.asSymbol, event.at(item.asSymbol));

				if (activeOrbit.isNil.not, {
					this.updateEQ(orbits.at(orbitIndex));
				})
			});
		});

	}.defer;
	}, ("/SuperDirtMixer"), recvPort: 57121).fix;
	}


}