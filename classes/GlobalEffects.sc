GlobalEffects {
	var fxOrder, notFoundIndex;
	var dirt;

    *new { | initDirt |
		^super.new.init(initDirt);
    }

	init {
		|initDirt|
		fxOrder = [\dirt_master_mix, \dirt_global_eq, \dirt_global_compressor];
		notFoundIndex = -1;
		dirt = initDirt;
	}

	sortGlobalEffects { | orbit |
		var sortedList;

		var sortFunction = { |a, b|
			var indexA;
			var indexB;

			indexA = fxOrder.indexOf(a.name.asSymbol);
			indexB = fxOrder.indexOf(b.name.asSymbol);

			if (indexA == nil, { indexA = notFoundIndex });
			if (indexB == nil, { indexB = notFoundIndex });

			if (indexA != notFoundIndex and: { indexB != notFoundIndex }, {
				indexA < indexB
			}, {
				if (indexA != notFoundIndex, {
					true
				}, {
					if (indexB != notFoundIndex, {
						false
					}, {
						orbit.globalEffects.indexOf(a) < orbit.globalEffects.indexOf(b)
					})
				})
			})
		};

		orbit.globalEffects.sort(sortFunction);
	}

	addGlobalEffect { | orbit, globalDirtEffect, shouldInitNodeTree = false|
		var isIncluded = orbit.globalEffects.detect({| effect |
			effect.name.asSymbol == globalDirtEffect.name.asSymbol;
		});

		if (isIncluded.isNil, {
			orbit.globalEffects = orbit.globalEffects.addFirst(globalDirtEffect);

			this.sortGlobalEffects(orbit);

			if (shouldInitNodeTree, {
				orbit.globalEffects.do({|effect| effect.synth.free });
				orbit.initNodeTree;
			});
		});
	}

	releaseGlobalEffect { | orbit, name |
		orbit.globalEffects.do({
			|effect, index|

			var removeIndex;

			if(effect.name == name, {
				removeIndex = index;
			});

			if (removeIndex.isNil.not, {
				orbit.globalEffects[removeIndex].synth.free;
				orbit.globalEffects.removeAt(removeIndex);
			});
		});
	}
}
