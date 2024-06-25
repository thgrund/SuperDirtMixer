MasterUI {
	*createMasterUIComponent {
		|stageMaster, prMasterBus, leftMasterIndicator, rightMasterIndicator|
			^VLayout(
				/* DEFINE MASTER UI : EXTRACT -> Stage master */
				if (prMasterBus.isNil.not, { StaticText.new.string_("Master").minWidth_(100).maxHeight_(30).align_(\center)}),
				if (prMasterBus.isNil.not, { HLayout(leftMasterIndicator,rightMasterIndicator).spacing_(0)}),
				20,
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
}