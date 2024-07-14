MasterUI : UIFactories {
    var handler;
	var leftMasterIndicator, rightMasterIndicator;

    *new { |initHandler|
        ^super.new.init(initHandler);
    }

	init { |initHandler|
		handler = initHandler;

		if (handler.isNil.not, {
			handler.subscribe(this, \masterEvent);
		});

		leftMasterIndicator = LevelIndicator.new.maxWidth_(12).drawsPeak_(true).warning_(0.9).critical_(1.0);
		rightMasterIndicator = LevelIndicator.new.maxWidth_(12).drawsPeak_(true).warning_(0.9).critical_(1.0);

		this.addMasterLevelOSCFunc;
	}

	handleEvent { |eventName, eventData|
        ("ServiceC received % with data: %".format(eventName, eventData)).postln;
    }

	createUI {
		|stageMaster, prMasterBus|

			^VLayout(
				/* DEFINE MASTER UI : EXTRACT -> Stage master */
				if (prMasterBus.isNil.not, { StaticText.new.string_("Master").minWidth_(100).maxHeight_(30).align_(\center)}),
				if (prMasterBus.isNil.not, { HLayout(leftMasterIndicator,rightMasterIndicator).spacing_(0)}),
				10,
				StaticText.new.string_("Stage Master").minWidth_(100).maxHeight_(30).align_(\center),
				10,
			    Button.new.string_("Live").action_({ |a|
				   "Live 1 button was pressed".postln;
				   handler.emitEvent(\eqEvent, "Data for eqEvent from MasterUIService");
			    }),
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