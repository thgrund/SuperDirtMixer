EqualizerUI {

	var handler;
	var orbits;
	var eqView, <equalizerComposite, freqScope;
	var activeOrbit;

    *new { |initHandler, initOrbits|
        ^super.new.init(initHandler, initOrbits);
    }

	init { |initHandler, initOrbits|
		handler = initHandler;
		orbits = initOrbits;

		if (handler.isNil.not, {
			handler.subscribe(this, \setActiveOrbit);
			handler.subscribe(this, \updateActiveOrbit);
			handler.subscribe(this, \updateUI);
		});

		if (orbits.isNil.not, {

			this.createUI();

			orbits.do({|orbit|
				this.setEQuiValues(orbit);
			});

			activeOrbit = orbits[0];

			this.setEQuiValues(activeOrbit);

		});

		this.addEqualizerListener;
	}

	handleEvent { |eventName, eventData|
		if (eventName == \setActiveOrbit, {
			// Save EQ values before switching to the new eq orbit
			if (activeOrbit.isNil.not, {
				this.setOrbitEQValues(activeOrbit);
			});

			activeOrbit = eventData;

			this.setEQuiValues(activeOrbit);
			freqScope.inBus = activeOrbit.dryBus;
		});

		if (eventName == \updateUI, {
			orbits.do({|orbit|
				this.setEQuiValues(orbit);
			});

			this.setEQuiValues(activeOrbit);
		});

		if (eventName == \updateActiveOrbit, {
			this.setOrbitEQValues(activeOrbit);
		});
    }

	startEQEffect {
		this.setOrbits(\activeEq,1);
	}

	stopEQEffect {
		this.setOrbits(\activeEq,nil);
	}

	setEQuiValues {|orbit|
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

		eqView.target = orbit.globalEffects[0].synth;

	}

	setOrbitEQValues {|orb|
		orb.set(*eqView.value.asArgsArray);
	}


	setOrbits { |...pairs|
		orbits.do(_.set(*pairs))
	}

	createUI {
		var height = 450;
		var width = 1200;

		/* DEFINE EQ GUI */
		equalizerComposite = CompositeView.new;

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

		eqView = EQui.new(equalizerComposite, equalizerComposite.bounds, orbits[0].globalEffects[0].synth);

		this.startEQEffect;
	}

	updateEQ {
		arg orbit;

		this.setEQuiValues(orbit);

		if ((activeOrbit.orbitIndex == orbit.orbitIndex).not, {
			this.setEQuiValues(activeOrbit);
		});

	}


	addEqualizerListener { OSCFunc ({|msg| {
		var event = ();
		var superDirtOSC = NetAddr("127.0.0.1", 57120);
		var orbitIndex;
		var eqParams = Array.with( \loShelfFreq, \loShelfGain, \loShelfRs,
			\loPeakFreq, \loPeakGain, \loPeakRq,
			\midPeakFreq, \midPeakGain, \midPeakRq,
			\hiPeakFreq, \hiPeakGain, \hiPeakRq,
			\hiShelfFreq, \hiShelfGain, \hiShelfRs);

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