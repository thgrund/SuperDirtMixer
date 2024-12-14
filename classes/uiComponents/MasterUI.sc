MasterUI : UIFactories {
	var handler;
	var leftMasterIndicator, rightMasterIndicator;
	var stageMaster;
	var synth;

    *new { | initHandler |
		^super.new.init(initHandler);
    }

	init {
		| initHandler |
		var uiKnobFactories = UIKnobFactories();
		handler = initHandler;
		stageMaster = Dictionary.new;

		leftMasterIndicator = LevelIndicator.new.maxWidth_(12).drawsPeak_(true).warning_(0.9).critical_(1.0);
		rightMasterIndicator = LevelIndicator.new.maxWidth_(12).drawsPeak_(true).warning_(0.9).critical_(1.0);

		if (handler.isNil.not, {
			handler.subscribe(this, \releaseAll);
		});


		// Live Button
		stageMaster.put(\live, Dictionary.newFrom([\element,
			Button.new.string_("Live")
			.action_({ |button|
				if(button.value == 1, {
					synth.run(true);
					this.changeButtonsStatus(true);
				}, {
					synth.run(false);
					this.changeButtonsStatus(false);
				});
			})
		]));

		stageMaster[\live][\element].states = [["Live", Color.black, Color.gray(0.9)], ["Live", Color.white, Color.new255(238, 180, 34)]];

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
			, {|value| value.ampdb.round(1e-2)} //knobToSynthValue
			, {|value| value} // synthToKnobValue
			, "%dB"
			, 1
			, {|value|
				synth.set(\limiterLevel, value);
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

		this.changeButtonsStatus(false);

		this.addMasterLevelOSCFunc;
	}

	handleEvent { |eventName, eventData|
		if (eventName == \releaseAll, {
			synth.free;
		});
	}

	changeButtonsStatus {
		|enable|

		stageMaster[\compThreshold][\element].enabled_(enable);
		stageMaster[\limiterLevel][\element].enabled_(enable);
		stageMaster[\highEndDb][\element].enabled_(enable);
	}

	createUI {
		|container, prMasterBus|

		    if (prMasterBus.isNil, {
			    stageMaster[\live][\element].enabled_(false);
			    stageMaster[\live][\element].states_([["Live", Color.grey(0.4), Color.white]]);
		    },{
			    if (synth.isNil.not, {synth.set(\out, prMasterBus)})
		    });

			if (synth.isNil && currentEnvironment[\SuperDirtMixer][\wasStageMasterSynthCreated] == false, {
			    synth = Synth.newPaused(\stageMaster, target: RootNode(~dirt.server), addAction: \addToTail);
			    currentEnvironment[\SuperDirtMixer].put(\wasStageMasterSynthCreated, true);
		    });

		    container.layout = VLayout(
				/* DEFINE MASTER UI : EXTRACT -> Stage master */
				StaticText.new.string_("Master").align_(\center),
				HLayout(leftMasterIndicator,rightMasterIndicator).spacing_(0),
				10,
				StaticText.new.string_("Stage Master").align_(\center),
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