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

	addGlobalEffect { | orbit, globalDirtEffect, nodeShouldBeAdded = true|
		var isNotIncluded = orbit.globalEffects.detect({| effect |
			effect.name.asSymbol == globalDirtEffect.name.asSymbol;
		}).isNil;

		if (isNotIncluded, {
			orbit.globalEffects = orbit.globalEffects.addFirst(globalDirtEffect);

			if (nodeShouldBeAdded == true, {
				globalDirtEffect.play(
					orbit.group, orbit.outBus, orbit.dryBus, orbit.globalEffectBus, orbit.orbitIndex
				);
				this.sortGlobalEffects(orbit);

				if (~dirt.isNil.not, {
					orbit.globalEffects.reverseDo({|effect|
						~dirt.server.sendMsg("/n_after", effect.synth.nodeID, orbit.group)
					});
				});

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
