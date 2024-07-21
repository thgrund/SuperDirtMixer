CompressorUI : UIFactories {
	var handler;
	var orbits;
	var threshold, ratio;
	var thresholdSlider, ratioSlider, ampThresholdSlider, ampThreshold, gainReductionSlider;
	var compressorElements;
	var bypassButton;
	var activeOrbit;
	var defaultParentEvent;
	var minThreshold;
	var uiKnobFactories;


	var width, height;

    *new { |initHandler, initOrbits|
        ^super.new.init(initHandler, initOrbits);
    }

	init { |initHandler, initOrbits|
		handler = initHandler;
		orbits = initOrbits;

		minThreshold = -60;
		threshold = -20; // Threshold in dB
		ratio = 4; // Compression ratio

		defaultParentEvent = [
			\activeCompressor, 0, \cpAttack, 0.01, \cpRelease, 0.1, \cpThresh, threshold, \cpTrim, 0, \cpGain, 0, \cpRatio, ratio, \cpLookahead, 0.0, \cpSaturate, 1, \cpHpf, 50, \cpKnee, 0, \cpBias, 0
	    ];

		// Parameters for the compressor
		ampThreshold = -30;

		height = 400;
		width = 400;

		if (orbits.isNil.not, {
			activeOrbit = orbits[0];
			this.setOrbits(defaultParentEvent);
		});

		if (handler.isNil.not, {
			handler.subscribe(this, \setActiveOrbit);
			handler.subscribe(this, \updateUI);

			handler.emitEvent(\extendDefaultParentEvent, defaultParentEvent);
		});

		bypassButton = Button.new.string_("Bypass").maxWidth_(75);

		bypassButton.action_({
			this.setBypassButtonState(bypassButton, true, activeOrbit, \activeCompressor);
		});

		compressorElements = Dictionary.new;

		uiKnobFactories = UIKnobFactories(activeOrbit);

		this.addSynthListener;
	}

	handleEvent { |eventName, eventData|
		if (eventName == \setActiveOrbit, {
			activeOrbit = eventData;

			uiKnobFactories.activeOrbit = activeOrbit;

			this.setBypassButtonState(bypassButton, false, activeOrbit, \activeCompressor);

			compressorElements.keysValuesDo {|key, components|
				if (activeOrbit.get(key).isNil.not, {
					components[\element].valueAction =
					    components[\orbitToKnobValue].value(activeOrbit.get(key));
				});
			};
		});

		if (eventName == \updateUI, {
			this.setBypassButtonState(bypassButton, false, activeOrbit, \activeCompressor);
		});
    }

	setOrbits { |pairs|
		orbits.do(_.set(*pairs))
	}

	// Function to draw the grid
	drawGrid {
		Pen.color = Color.new255(150, 150, 150);
		Pen.width = 0.5;

		// Draw vertical grid lines (Input Level)
		(0..height).do { |x|
			if (x % (height / 10) == 0, {Pen.line(Point(x, 0), Point(x, height))});
		};

		// Draw horizontal grid lines (Output Level)
		(0..width).do { |y|
			if (y % (width / 10) == 0, { Pen.line(Point(0, y), Point(width, y))});
		};

		Pen.stroke;

		// Draw axis labels
		Pen.color = Color.black;
		Pen.stringAtPoint("Input Level (dB)", Point(height * 0.5, width * 0.9));
		Pen.rotate(0);
		Pen.stringAtPoint("Output Level (dB)", Point(0, width * 0.5)); // Adjusted position for better visibility
		Pen.rotate(-0);
	}

	// Function to draw the compressor curve
	drawCompressorCurve {
		var inputLevels, outputLevels, thresholdX, thresholdY, offset, ampThresholdY, ampThresholdX;

		offset = threshold - (threshold / ratio);

		// Map threshold to coordinates
		thresholdX = threshold.linlin(minThreshold, 0, 0, width);

		// Calculate output level at threshold
		thresholdY = if (threshold < threshold, {
			threshold.linlin(minThreshold, 0, height, 0);
		}, {
			((threshold + (threshold - threshold) / ratio) + offset).linlin(minThreshold, 0, height, 0);
		});

		// Draw the threshold circle
		Pen.color = Color.new(red: 1, green: (187/255), blue: 82/255, alpha: 1.0);
		Pen.fillOval(Rect(thresholdX - 10, thresholdY - 10, 20, 20));

		// Calculate and draw the compressor curve
		inputLevels = [minThreshold, threshold, 0];
		outputLevels = inputLevels.collect { |input|
			if (input < threshold, {
				input;
			}, {
				((threshold + (input - threshold) / ratio) + offset);
			});
		};

		Pen.color = Color.new(red: (179/255), green: (179/255), blue: 1, alpha: 1.0);
		Pen.width = 2;

		inputLevels.do { |input, i|
			var x, y;
			x = input.linlin(minThreshold, 0, 0, width);
			y = outputLevels[i].linlin(minThreshold, 0, height, 0);
			if (i == 0, {
				Pen.moveTo(Point(x, y));
			}, {
				Pen.lineTo(Point(x, y));
			});
		};

		Pen.stroke;
		// Map amplitude threshold to coordinates
		ampThresholdX = ampThreshold.linlin(minThreshold, 0, 0, width);

		// Calculate output level at amplitude threshold
		ampThresholdY = if (ampThreshold < threshold, {
			ampThreshold.linlin(minThreshold, 0, height, 0);
		}, {
			((threshold + (ampThreshold - threshold) / ratio) + offset).linlin(minThreshold, 0, height, 0);
		});

		// Draw the amplitude threshold circle
		Pen.color = Color.new(red: 1, green: (117/255), blue: 82/255, alpha: 1.0);
		Pen.fillOval(Rect(ampThresholdX - 10, ampThresholdY - 10, 20,20));
	}

	createUI {

		var compressorComposite = CompositeView.new;
		var comporessorView;
		var gainReductionSlider;
		var thresholdLabel;
		var defaultParentEventDict = defaultParentEvent.asDict;

		/*
		Ndef(\compressor).addSpec(
		     \attack, [0.0000001,0.1, \exp],
		     \release, [0.0000001,0.4, \exp],
		     \threshhold, [0,-120],
		     \trim, [0,60],
		     \gain, [0,60],
		     \ratio, [1,20, \exp],
		     \lookahead, [0.0,1],
		     \saturate, \switch,
		     \hpf, [10, 1000] ,
		     \knee, [0.0, 10] ,
		     \bias, [0.0, 0.5] ,
		);
		*/

		this.setBypassButtonState(bypassButton, false, activeOrbit, \activeCompressor);

		compressorComposite.minHeight_(height);
		compressorComposite.minWidth_(width);

		comporessorView = UserView(compressorComposite, compressorComposite.bounds).background_(Color.new255(235, 235, 235));

		// Draw the comporessorView
		comporessorView.drawFunc = {
			this.drawGrid();
			this.drawCompressorCurve();
		};

		comporessorView.refresh;

		gainReductionSlider = LevelIndicator.new.fixedWidth_(30).value_(0.9);
		// inverse
		gainReductionSlider.background = Color.green;
		gainReductionSlider.meterColor = Color.black.alpha_(1);
		gainReductionSlider.warningColor = Color.black.alpha_(1);
		gainReductionSlider.criticalColor = Color.black.alpha_(1);

		thresholdLabel = StaticText.new.string_("%dB".format(defaultParentEventDict[\cpThresh])).fixedWidth_(60).align_(\center);

		// Slider to adjust threshold
		thresholdSlider = Slider(nil, Rect(610, 50, 30, height - 15))
		.maxHeight_(height)
		.fixedWidth_(30)
		.orientation_(\vert)
		//.value_(threshold.linlin(minThreshold, 0, 0, 1))
		.value_(1)
		.action_({ |slider|
			threshold = slider.value.linlin(0, 1, minThreshold, 0);
			activeOrbit.set(\cpThresh, threshold);
			thresholdLabel.string_("%dB".format(threshold.round(1e-1)));

			comporessorView.refresh;
		});

		compressorElements.put(\cpThresh,
			Dictionary.newFrom([
				\element, thresholdSlider
				, \title, StaticText.new.string_("T").align_(\center).maxHeight_(15)
				, \value, thresholdLabel
				, \orbitToKnobValue, {|value| value.linlin(minThreshold, 0, 0, 1)}
			])
		);

		compressorElements.put(\gainReduction,
			Dictionary.newFrom([
				\element, gainReductionSlider
				, \title, StaticText.new.string_("GR").align_(\center).maxHeight_(15)
			])
		);

		uiKnobFactories.knobWithValueLabelFactory2(
			compressorElements
			, \cpRatio, "Ratio",
			{|value| value.linexp(0,1,1,20).round(1e-2) },
			{|value| (value).explin(1,20,0,1)},
			"%",
		    {
				ratio = activeOrbit.get(\cpRatio);
				comporessorView.refresh;
			}
		);


		uiKnobFactories.knobWithValueLabelFactory2(
			compressorElements
			, \cpAttack, "Attack",
			{|value| value.linexp(0,1,1,11).round(1e-2) - 1},
			{|value| (value + 1).explin(1,11,0,1)},
			"%ms"
		);

		uiKnobFactories.knobWithValueLabelFactory2(
			compressorElements
			, \cpAttack, "Attack",
			{|value| value.linexp(0,1,1,11).round(1e-2) - 1},
			{|value| (value + 1).explin(1,11,0,1)},
			"%ms"
		);

		uiKnobFactories.knobWithValueLabelFactory2(
			compressorElements
			, \cpRelease, "Release",
			{|value| value.linexp(0,1,1,11).round(1e-2) - 1},
			{|value| (value + 1).explin(1,11,0,1)},
			"%ms"
		);


		uiKnobFactories.knobWithValueLabelFactory2(
			compressorElements
			, \cpTrim, "Trim",
			{|value| value.linlin(0, 1, 0, 60).round(1e-2)},
			{|value| value.linlin(0,60,0,1)},
			"%dB"
		);

		uiKnobFactories.knobWithValueLabelFactory2(
			compressorElements
			, \cpGain, "Gain",
			{|value| value.linlin(0, 1, 0, 60).round(1e-2)},
			{|value| value.linlin(0,60,0,1)},
			"%dB"
		);

		uiKnobFactories.knobWithValueLabelFactory2(
			compressorElements
			, \cpLookahead, "Lookahead",
			{|value| value.round(1e-2)},
			{|value| value},
			"%"
		);

		uiKnobFactories.knobWithValueLabelFactory2(
			compressorElements
			, \cpKnee, "Knee",
			{|value| value.linlin(0, 1, 0, 10).round(1e-2)},
			{|value| value.linlin(0,10,0,1)},
			"%"
		);

		uiKnobFactories.knobWithValueLabelFactory2(
			compressorElements
			, \cpBias, "Bias",
			{|value| value.linlin(0, 1, 0, 0.5).round(1e-2)},
			{|value| value.linlin(0,0.5,0,1)},
			"%"
		);

		uiKnobFactories.knobWithValueLabelFactory2(
			compressorElements
			, \cpHpf, "HPF",
			{|value| value.linlin(0, 1, 10, 1000).round(1e-2)},
			{|value| value.linlin(10,1000,0,1)},
			"%Freq"
		);

		comporessorView.refresh;

		^VLayout(
			HLayout(bypassButton, StaticText.new.string_("Compressor").fixedHeight_(15))
			,HLayout(
				VLayout(
					compressorElements[\cpThresh][\title]
					, compressorElements[\cpThresh][\element]
					, compressorElements[\cpThresh][\value]
				    ,VLayout (compressorElements[\cpKnee][\title], compressorElements[\cpKnee][\element], compressorElements[\cpKnee][\value])
				    ,VLayout (compressorElements[\cpBias][\title], compressorElements[\cpBias][\element], compressorElements[\cpBias][\value])
				).setAlignment(1, \center)
				, compressorComposite
				, VLayout(
					compressorElements[\gainReduction][\title]
					, compressorElements[\gainReduction][\element]
				    ,VLayout (compressorElements[\cpLookahead][\title], compressorElements[\cpLookahead][\element], compressorElements[\cpLookahead][\value])
				    ,VLayout (compressorElements[\cpTrim][\title], compressorElements[\cpTrim][\element], compressorElements[\cpTrim][\value])
				).setAlignment(1, \center)
			)
			, 20
			,HLayout(
				VLayout (compressorElements[\cpRatio][\title], compressorElements[\cpRatio][\element], compressorElements[\cpRatio][\value])
				, VLayout (compressorElements[\cpAttack][\title], compressorElements[\cpAttack][\element], compressorElements[\cpAttack][\value])
				, VLayout (compressorElements[\cpRelease][\title], compressorElements[\cpRelease][\element], compressorElements[\cpRelease][\value])
			    , VLayout (compressorElements[\cpGain][\title], compressorElements[\cpGain][\element], compressorElements[\cpGain][\value])
				, VLayout (compressorElements[\cpHpf][\title], compressorElements[\cpHpf][\element], compressorElements[\cpHpf][\value])

			)
		);
	}

	/*addSynthListener {
		OSCFunc({ |msg|
			"GR original: %, compressed: %, index: %".format(msg[3], msg[4], msg[5]).postln
		}, '/compressor')
	}*/
	addSynthListener {
		OSCFunc({ |msg|
			"CP original: %".format(msg).postln
		}, '/cpInRms');

		OSCFunc({ |msg|
			"CP compressed: %".format(msg).postln
		}, '/cpCompressedRms');
	}

}