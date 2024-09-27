MasterUI : UIFactories {
	var leftMasterIndicator, rightMasterIndicator;
	var stageMaster;
	var synth;

    *new {
        ^super.new.init;
    }

	init {
		var uiKnobFactories = UIKnobFactories();
		stageMaster = Dictionary.new;

		leftMasterIndicator = LevelIndicator.new.maxWidth_(12).drawsPeak_(true).warning_(0.9).critical_(1.0);
		rightMasterIndicator = LevelIndicator.new.maxWidth_(12).drawsPeak_(true).warning_(0.9).critical_(1.0);

		// Live Button
		stageMaster.put(\live, Dictionary.newFrom([\element,
			Button.new.string_("Live")
			.action_({ |button|
				if(button.value == 1, {
					synth.run(true);
				}, {
					synth.run(false);
				});
			})
		]));

		stageMaster[\live][\element].states = [["Live", Color.black, Color.white], ["Live", Color.white, Color.new255(238, 180, 34)]];

		uiKnobFactories.knobWithValueLabelFactory3( stageMaster
			, \compThreshold, "Comp Threshold"
			, {|value| value.ampdb.round(1e-2) } //knobToSynthValue
			, {|value| value} // synthToKnobValue
			, "%dB"
			, 0.7
			, {|value|
				synth.set(\compThreshold, value);
			}
		);

		uiKnobFactories.knobWithValueLabelFactory3( stageMaster
			, \limiterLevel, "Limiter Level"
			, {|value| value.linlin(0.0,1.0,0.001, 10.0).round(1e-2)} //knobToSynthValue
			, {|value| value} // synthToKnobValue
			, "%ms"
			, (2.0).linlin(0.001, 10.0, 0.0,1.0)
			, {|value|
				synth.set(\limiterLevel, stageMaster[\limiterLevel][\knobToSynthValue].value(value));
			}
		);

		uiKnobFactories.knobWithValueLabelFactory3( stageMaster
			, \highEndDb, "High End dB"
			, {|value| value.linexp(0.0,1.0,1.0, 31.0).round(1e-2) - 1} //knobToSynthValue
			, {|value| value} // synthToKnobValue
			, "%dBs"
			, (3.0).explin(1.0, 31.0, 0.0,1.0)
			, {|value|
				synth.set(\highEndDb, stageMaster[\highEndDb][\knobToSynthValue].value(value));
			}
		);


		if (synth.isNil, {
			synth = Synth.newPaused(\stageMaster, target: RootNode(~dirt.server), addAction: \addToTail);
		});

		this.addMasterLevelOSCFunc;
	}

	handleEvent { |eventName, eventData|
        ("ServiceC received % with data: %".format(eventName, eventData)).postln;
    }

	createUI {
		|prMasterBus|

			^VLayout(
				/* DEFINE MASTER UI : EXTRACT -> Stage master */
				if (prMasterBus.isNil.not, { StaticText.new.string_("Master").minWidth_(100).maxHeight_(30).align_(\center)}),
				if (prMasterBus.isNil.not, { HLayout(leftMasterIndicator,rightMasterIndicator).spacing_(0)}),
				10,
				StaticText.new.string_("Stage Master").minWidth_(100).maxHeight_(30).align_(\center),
				10,
			    stageMaster[\live][\element],
			    10,
				stageMaster[\compThreshold][\title],
				stageMaster[\compThreshold][\element],
				stageMaster[\compThreshold][\value],
				10,
				stageMaster[\limiterLevel][\title],
				stageMaster[\limiterLevel][\element],
				stageMaster[\limiterLevel][\value],
				10,
				stageMaster[\highEndDb][\title],
				stageMaster[\highEndDb][\element],
				stageMaster[\highEndDb][\value],
			);
	}

	addMasterLevelOSCFunc {
		OSCFunc({ |msg|
				{
					try {
				        var rmsL = msg[4].ampdb.linlin(-80, 0, 0, 1);
				        var peakL = msg[3].ampdb.linlin(-80, 0, 0, 1, \min);
                        var rmsR = msg[6].ampdb.linlin(-80, 0, 0, 1);
				        var peakR = msg[5].ampdb.linlin(-80, 0, 0, 1, \min);
				        leftMasterIndicator.value = rmsL;
				        leftMasterIndicator.peakLevel = peakL;
						rightMasterIndicator.value = rmsR;
				        rightMasterIndicator.peakLevel = peakR;

					} { |error|
						if(error.isKindOf(PrimitiveFailedError).not) { error.throw }
					};
				}.defer;
	       }, ("/MixerMasterOutLevels").asSymbol, Server.local.addr).fix;
	}

}