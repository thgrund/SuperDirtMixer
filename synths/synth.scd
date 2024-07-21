(
{
var numChannels = ~dirt.numChannels;

SynthDef("dirt_global_eq" ++ numChannels, { |out, dryBus, gate = 1, activeEq|
		var sound;
		var in = In.ar(dryBus, numChannels);
		//ReplaceOut.ar(dryBus, in * (1 - gobalCutoffDry));

		// Dry/Wet
		// TODO: disable if no value was received
		sound = in.equi;
		DirtPause.ar(sound * activeEq, graceTime: 1 * activeEq);

		//ReplaceOut.ar(dryBus, (in * (1 - reverbWet)) + (sound * reverbWet ));
	    ReplaceOut.ar(dryBus, sound);

	}, [\ir]).add;


/*SynthDef("dirt_global_compressor" ++ numChannels, { |out, dryBus, gate = 1, orbitIndex, cpThresh = 1, cpRatio = 1, cpKnee = 6, cpMakeUp = 0, cpAttack = 0.1, cpRelease = 0.05, activeCompressor = 0|
		var in = In.ar(dryBus, numChannels);
		var compressedSound;

		//compressedSound = DCompressor.ar(in, ratio: cpRatio, threshold: cpThresh, attack: cpAttack, release: cpRelease);
		//compressedSound = SoftKneeCompressor.ar(in, 1, cpThresh, cpRatio, cpKnee, cpAttack, cpRelease, cpMakeUp);

		/*compressedSound = Compander.ar(in, in * 4, cpThresh,
			slopeBelow: 2,
			slopeAbove: cpRatio,
			clampTime: cpAttack,
			relaxTime: cpRelease
		);*/

		compressedSound = Compander.ar(in, in * 4, 0.2,
			slopeBelow: 2,
			slopeAbove: 1/3,
			clampTime: cpAttack,
			relaxTime: cpRelease
		);

		DirtPause.ar(in * activeCompressor, graceTime: 1 * activeCompressor);
		EnvGen.kr(Env.asr, gate, doneAction:2);

		//SendReply.kr(Impulse.kr(5), '/compressor', [ampIn, ampCompressor, orbitIndex]);
		SendPeakRMS.kr(in, replyRate: 8, peakLag: 3, cmdName:"/cpInRms", replyID: orbitIndex);
		SendPeakRMS.kr(compressedSound, replyRate: 8, peakLag: 3, cmdName:"/cpCompressedRms", replyID: orbitIndex);

	    ReplaceOut.ar(dryBus, compressedSound);

	}, [\ir]).add;*/

	// Adapted from
	// https://www.youtube.com/watch?v=CAJXgJpQuSM&list=PLXCUkMwOEWQtB-leHHSexTizzcACdozp9&index=16
	// https://github.com/alikthename/Musical-Design-in-Supercollider
	SynthDef("dirt_global_compressor" ++ numChannels, { |out, dryBus, gate = 1, orbitIndex,
		cpAttack=0.01, cpRelease=0.1, cpThresh= -6, cpTrim=0, cpGain=0, cpRatio=4,
	    cpLookahead=0, cpSaturate=1, cpHpf=50, cpKnee=0, cpBias=0 , activeCompressor = 0|

		var local, dry, in, t, o, r, c, e, cpLookaheadDelay;
		var cpKneelag;

		dry = In.ar(dryBus, numChannels);

		in = dry * cpTrim.dbamp;
		e = in.mean;
		e = HPF.ar(e, cpHpf);
		e = EnvDetect.ar(e, cpAttack, cpRelease);

		o = e.ampdb.excess(cpThresh);

		r = cpRatio;

		c = (max(o, 0.0)) * (r.reciprocal - 1);
		cpKneelag = cpAttack * cpKnee;

		c = c.lag(cpKneelag);
		c = c.dbamp;

		cpLookaheadDelay = DelayC.ar(in, 0.5, (cpAttack + cpRelease + cpKneelag * cpLookahead).lag(0.4));

		in = cpLookaheadDelay * c;
		in = in * cpGain.dbamp;

		in = Select.ar(cpSaturate, [in, (in + K2A.ar(cpBias)).softclip - cpBias]);
		in = LeakDC.ar(in);

		DirtPause.ar(dry * activeCompressor, graceTime: 1 * activeCompressor);
		EnvGen.kr(Env.asr, gate, doneAction:2);

		//SendReply.kr(Impulse.kr(5), '/compressor', [ampIn, ampCompressor, orbitIndex]);
		/*SendPeakRMS.kr(in, replyRate: 8, peakLag: 3, cmdName:"/cpInRms", replyID: orbitIndex);
		SendPeakRMS.kr(compressedSound, replyRate: 8, peakLag: 3, cmdName:"/cpCompressedRms", replyID: orbitIndex);*/

	    ReplaceOut.ar(dryBus, in * activeCompressor);

	}, [\ir]).add;

SynthDef("dirt_master_mix" ++ numChannels, { |dryBus, effectBus, gate = 1, masterGain = 1, gainControlLag = 0.1, globalMasterGain = 1|
	var drySignal = In.ar(dryBus, numChannels);
	var wetSignal = In.ar(effectBus, numChannels);
	var env = EnvGen.kr(Env.asr, gate);
	drySignal = (drySignal * masterGain.lag(gainControlLag)) * globalMasterGain;
	wetSignal = (wetSignal * masterGain.lag(gainControlLag)) * globalMasterGain;
	DirtPause.ar(drySignal + wetSignal, graceTime:4);
	XOut.ar(dryBus, env, drySignal);
	XOut.ar(effectBus, env, wetSignal);
}, [\ir, \ir, \kr, \kr, \kr, \kr]).add;

}.value
);